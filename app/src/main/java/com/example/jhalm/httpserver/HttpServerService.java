package com.example.jhalm.httpserver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;

public class HttpServerService extends Service
{
    private final String CHANNEL_ID = "HttpServerServiceChannel";

    public static final String ALLDATA_BROADCAST = "com.example.jhalm.httpserver.ALL_DATA";
    public static final String NEWDATA_BROADCAST = "com.example.jhalm.httpserver.NEW_DATA";

    public static final int SEND_ALLDATA = 1;

    private SocketServer socketServer;
    private Handler msgHandler;
    private List<ActivityLog> log;
    private CameraRead camera;

    @Override
    public void onCreate()
    {
        super.onCreate();
        this.log = new ArrayList<ActivityLog>();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        Messenger messenger = new Messenger(new Handler(this.getMainLooper())
        {
            @Override
            public void handleMessage(final Message inputMessage)
            {
                if(inputMessage.what == SEND_ALLDATA)
                {
                    Intent broadcastIntent = new Intent(ALLDATA_BROADCAST);
                    broadcastIntent.putParcelableArrayListExtra("log", (ArrayList<? extends Parcelable>) log);
                    LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
                    localBroadcastManager.sendBroadcast(broadcastIntent);
                }
            }
        });
        return messenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int Flags, int startID)
    {
        //Toast.makeText(this.getApplicationContext(), "HTTP Server service start", Toast.LENGTH_SHORT).show();
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, HttpServerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.notfication))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setSound(null)
                .build();

        startForeground(1, notification);

        this.camera = new CameraRead(getApplicationContext());
        this.camera.getCamera();

        if(intent.getBooleanExtra("save", false))
            this.camera.startTakeFileSnapshots();
        else
            this.camera.startTakeArraySnapshots();

        this.msgHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(final Message inputMessage)
            {
                log.add((ActivityLog) inputMessage.obj);
                Intent broadcastIntent = new Intent(NEWDATA_BROADCAST);
                broadcastIntent.putExtra("log", ((ActivityLog) inputMessage.obj));
                LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
                localBroadcastManager.sendBroadcast(broadcastIntent);
            }
        };

        this.socketServer = new SocketServer();
        this.socketServer.setThreadCount(intent.getIntExtra("threads", 5));
        this.socketServer.setHandler(this.msgHandler);
        this.socketServer.setCamera(this.camera);
        this.socketServer.start();

        return Service.START_STICKY_COMPATIBILITY;
    }

    @Override
    public void onDestroy()
    {
        this.socketServer.close();
        this.camera.releaseCamera();
        stopForeground(true);
        destroyNotificationChannel();
    }

    private void destroyNotificationChannel()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.deleteNotificationChannel(CHANNEL_ID);
        }
    }

    private void createNotificationChannel()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
