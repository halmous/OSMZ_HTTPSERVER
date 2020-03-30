package com.example.jhalm.httpserver;

import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.textclassifier.TextLinks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Semaphore;

public class ClientThread extends Thread
{
    private Socket s;
    private Handler h;
    private Semaphore semaphore;
    private CameraRead camera;
    private SocketServer.BRunning run;

    public void setRun(SocketServer.BRunning run) { this.run = run; }

    public void setSemaphore(Semaphore sem) { this.semaphore = sem; }

    public void setSocket(Socket sock)
    {
        this.s = sock;
    }

    public void setHandler(Handler handler)
    {
        this.h = handler;
    }

    public void setCamera(CameraRead camera) { this.camera = camera; }

    private String parseMime(String filePath)
    {
        String array[] = filePath.split("\\.");
        if(array.length == 2)
        {
            if (array[1].equals("png"))
                return "image/png";
            else if (array[1].equals("html"))
                return "text/html";
            else if (array[1].equals("jpg"))
                return "image/jpg";
        }

        return "";
    }


    private void handleLogMessage(String URI, String custom1, String custom2)
    {
        ActivityLog log = new ActivityLog();
        log.custom1 = custom1;
        log.custom2 = custom2;
        log.URI = URI;

        Message message = new Message();
        message.obj = log;

        message.setTarget(this.h);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
        {
            message.setAsynchronous(true);
        }

        message.sendToTarget();
    }

    private String getSize(long size)
    {
        if(size < 1024)
            return size + " B";

        double tmpSize = size / 1024.0;
        DecimalFormat decimalFormat = new DecimalFormat("#.00");

        if(tmpSize < 1024)
            return decimalFormat.format(tmpSize) + " kB";

        tmpSize /= 1024.0;

        if(tmpSize < 1024)
            return  decimalFormat.format(tmpSize) + " MB";

        tmpSize /= 1024.0;
        return  decimalFormat.format(tmpSize) + " GB";
    }

    private String createResponse(int code, String mimeType)
    {
        return createResponse(code, mimeType, 0);
    }

    private String createResponse(int code, String mimeType, long payloadSize)
    {
        String ret = "HTTP/1.1 ";

        if(code == 200)
            ret += code + " OK\n";
        else if(code == 404)
            ret += code + " Not Found\n";
        else if(code == 503)
            ret += code + " Service Unavailable";
        else
            ret += "500 Internal Server Error\n";

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date today = Calendar.getInstance().getTime();

        ret += "Date: " + dateFormat.format(today) + "\n";
        ret += "Server: AndroidHttpServer (API" + android.os.Build.VERSION.SDK_INT + ")\n";
        ret += "Content-Type: " + mimeType + "\n";
        if(payloadSize > 0)
            ret += "Content-Length: " + payloadSize + "\n";

        ret += "\n";

        return ret;
    }

    private void sendFile(OutputStream o, BufferedWriter out, File file, RequestData request) throws IOException {
        try {
            out.write(createResponse(200, parseMime(file.getName()), file.length()));
            out.flush();

            handleLogMessage(request.URI, "File size " + getSize(file.length()), request.getOption("User-Agent"));

            byte buf[] = new byte[4096];
            FileInputStream fileStream = new FileInputStream(file);

            while (fileStream.available() > 0) {
                int size = fileStream.read(buf, 0, 4096);
                o.write(buf, 0, size);
            }

            o.flush();
        }
        catch (Exception e)
        {
            throw e;
        }
    }

    private void listFolder(BufferedWriter out, File file, RequestData request) throws IOException {
        try
        {
            File[] files = file.listFiles();

            String html = "<html><body>";
            html += "<h1>Index of " + request.URI + "</h1>";
            html += "<table><tr><th>Name</th><th>Size</th></tr>";

            for (int i = 0; i < files.length; i++) {
                String path = files[i].getAbsolutePath().substring((Environment.getExternalStorageDirectory().getAbsolutePath() + "/public_html").length());
                html += "<tr><td><a href=\"" + path + "\">" + files[i].getName() + "</a></td><td>" + getSize(files[i].length()) + "</td></tr>";
            }

            html += "</table></body></html>\n";
            out.write(createResponse(200, "text/html", html.length()));
            out.write(html);

            handleLogMessage(request.URI, "Path: " + file.getAbsolutePath(), request.getOption("User-Agent"));

            out.flush();
        }
        catch (Exception e)
        {
            throw e;
        }
    }

    private void send404(BufferedWriter out, RequestData request) throws IOException {
        try
        {
            String html = "<html><head><title>404 Not Found</title></head><body><h1>404 Not Found</h1></body></html>\n";
            out.write(createResponse(404, "text/html",  html.length()));
            out.write(html + "\n");

            handleLogMessage(request.URI, "404 Not found", request.getOption("User-Agent"));

            out.flush();
        }
        catch (Exception e)
        {
            throw e;
        }
    }

    private void send503Busy(BufferedWriter out) throws IOException {
        try {
            String html = "<html><head><title>503 Service Unavailable </title></head><body><h1>Server too busy</h1></body></html>\n";
            out.write(createResponse(503, "text/html", html.length()));
            out.write(html + "\n");

            out.flush();

            handleLogMessage("", "505 Service Unavailable", "Server too busy");
        }
        catch (Exception e)
        {
            throw e;
        }

    }

    private void sendCameraSnapshot(OutputStream o, BufferedWriter out, RequestData request) throws IOException
    {
        byte img[] = this.camera.getJpegData();
        out.write(createResponse(200, "image/jpg", img.length));
        out.flush();
        o.write(img);
        o.flush();
        handleLogMessage("/camera/snapshot", "Size: " + img.length, request.getOption("User-Agent"));
    }

    private void sendCameraStream(OutputStream o, BufferedWriter out, RequestData request) throws IOException, InterruptedException {
        handleLogMessage("/camera/stream", "Camera stream", request.getOption("User-Agent"));
        out.write(createResponse(200, "multipart/x-mixed-replace; boundary=\"OSMZ_boundary\""));
        out.flush();

        while(run.bRunning)
        {
            out.write("--OSMZ_boundary\n");
            out.write("Content-Type: image/jpeg\n\n");
            out.flush();

            byte img[] = this.camera.getJpegData();
            o.write(img);
            o.flush();

            Thread.sleep(50);
        }

    }

    private void executeCGIBin(OutputStream o, BufferedWriter out, RequestData request) throws IOException {
        handleLogMessage(request.URI, "CGI bin execution", request.getOption("User-Agent"));
        out.write(createResponse(200, "text/plain"));
        out.flush();

        String command = request.URI.substring(9);
        String commandAndArgsArray[] = command.split("%20");

        if(commandAndArgsArray.length > 0)
        {
            List<String> commandAndArgsList = new ArrayList<String>();
            commandAndArgsList.add(commandAndArgsArray[0]);

            for(int i = 1; i < commandAndArgsArray.length; i++)
            {
                commandAndArgsList.add(commandAndArgsArray[1]);
            }

            ProcessBuilder pb = new ProcessBuilder(commandAndArgsArray);
            Process process = pb.start();

            InputStream inputStream = process.getInputStream();

            int retLength = 0;
            boolean processEnded = false;

            do {
                try
                {
                    byte ret[] = new byte[4096];
                    retLength = inputStream.read(ret);
                    o.write(ret, 0, retLength);
                    o.flush();
                    process.exitValue();
                    processEnded = true;
                }
                catch (IllegalThreadStateException e)
                {
                    processEnded = false;
                }

            }while(!processEnded | retLength >= 4096);

            process.destroy();

        }
    }

    public void run() {
        try {
            OutputStream o = s.getOutputStream();
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(o));
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

            if(semaphore.tryAcquire() == false)
            {
                send503Busy(out);
                s.close();
                return;
            }

            String tmp = in.readLine();

            if (tmp != null) {
                Log.d("SERVER", "Accepted---" + tmp);
                RequestData request = new RequestData();

                if (request.parseRequest(tmp)) {
                    do {
                        tmp = in.readLine();
                        Log.d("SERVER", "Accepted---" + tmp);

                        request.parseOptions(tmp);
                    }
                    while (!tmp.isEmpty());

                    if (request.method.equals("GET"))
                    {
                        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/public_html" + request.URI);
                        String uriParts[] = request.URI.split("/");


                        if(uriParts.length > 1 && uriParts[1].equals("cgi-bin"))
                        {
                            executeCGIBin(o, out, request);
                        }
                        else if(request.URI.equals("/camera/snapshot"))
                        {
                            sendCameraSnapshot(o, out, request);
                        }
                        else if(request.URI.equals("/camera/stream"))
                        {
                            sendCameraStream(o, out, request);
                        }
                        else if (file.exists()) {
                            if (file.isFile()) {
                                sendFile(o, out, file, request);
                            } else {
                                listFolder(out, file, request);
                            }

                        } else {
                            send404(out, request);
                        }
                    }
                }
            }

            s.close();
        }
        catch (SocketException e)
        {
            Log.d("SERVER", "Connection lost");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        this.semaphore.release();
    }
}
