package com.example.jhalm.httpserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.concurrent.Semaphore;

import android.content.res.Resources;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

public class SocketServer extends Thread {

    public static class BRunning
    {
        public boolean bRunning;
    }

	
	ServerSocket serverSocket;
	public final int port = 12345;
	BRunning bRunning;
	private Handler handler;
	private Semaphore semaphore;
	private int threadCount;
	private CameraRead camera;

	public void setHandler(Handler handler)
    {
        this.handler = handler;
    }

    public void setThreadCount(int threadCount)
    {
        this.threadCount = threadCount;
    }

    public void setCamera(CameraRead camera) { this.camera = camera; }

	public void close() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			Log.d("SERVER", "Error, probably interrupted in accept(), see log");
			e.printStackTrace();
		}
        bRunning.bRunning = false;
	}

	public void run() {
	    this.bRunning = new BRunning();
	    this.semaphore = new Semaphore(this.threadCount);

        try {
        	Log.d("SERVER", "Creating Socket");
            serverSocket = new ServerSocket(port);
            bRunning.bRunning = true;
            while (bRunning.bRunning) {
            	Log.d("SERVER", "Socket Waiting for connection");
                Socket s = serverSocket.accept(); 
                Log.d("SERVER", "Socket Accepted");
                
                ClientThread clientThread = new ClientThread();
                clientThread.setSocket(s);
                clientThread.setHandler(handler);
                clientThread.setSemaphore(this.semaphore);
                clientThread.setCamera(this.camera);
                clientThread.setRun(bRunning);
                clientThread.start();


                Log.d("SERVER", "Socket Closed");
            }
        } 
        catch (IOException e) {
            if (serverSocket != null && serverSocket.isClosed())
            	Log.d("SERVER", "Normal exit");
            else {
            	Log.d("SERVER", "Error");
            	e.printStackTrace();
            }
        }
        finally {
        	serverSocket = null;
            bRunning.bRunning = false;
        }
    }

}
