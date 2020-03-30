package com.example.jhalm.httpserver;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.List;

public class HttpServerActivity extends Activity implements OnClickListener
{
	private Messenger messenger;
	private Boolean boundToService = false;

	private ServiceConnection serviceConnection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			messenger = new Messenger(iBinder);
			boundToService = true;
			requestLogData();

			TextView status = findViewById(R.id.status);
			status.setText(R.string.server_running);
			status.setBackgroundResource(android.R.color.holo_green_light);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			messenger = null;
			boundToService = false;
			TextView status = findViewById(R.id.status);
			status.setText(R.string.server_not_running);
			status.setBackgroundResource(android.R.color.holo_red_dark);
		}
	};

	private class LogReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
		    switch (intent.getAction())
            {
                case HttpServerService.ALLDATA_BROADCAST:
                    initLogData(intent.<ActivityLog>getParcelableArrayListExtra("log"));
                    break;
                case HttpServerService.NEWDATA_BROADCAST:
                    logAdapter.addLog((ActivityLog) intent.getParcelableExtra("log"));
                    logAdapter.notifyDataSetChanged();
                    break;
            }
		}
	}

	private void initLogData(List<ActivityLog> list)
    {
        if(logAdapter.getCount() <= 0)
        {
            for(int i = 0; i < list.size(); i++)
            {
                logAdapter.addLog(list.get(i));
            }

            logAdapter.notifyDataSetChanged();
        }
    }


	private ListView listView;
	private LogReceiver logReceiver;
	private LogAdapter  logAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_http_server);

		Button btn1 = (Button) findViewById(R.id.button1);
		Button btn2 = (Button) findViewById(R.id.button2);

		btn1.setOnClickListener(this);
		btn2.setOnClickListener(this);

		listView = (ListView) findViewById(R.id.logList);

		if(logAdapter == null)
			logAdapter = new LogAdapter(getApplicationContext());

		listView.setAdapter(logAdapter);

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA},
                    50); }


		IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(HttpServerService.ALLDATA_BROADCAST);
        intentFilter.addAction(HttpServerService.NEWDATA_BROADCAST);

        logReceiver = new LogReceiver();

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(logReceiver, intentFilter);

		bindServerService();
    }

    @Override
    public void onDestroy()
    {
        if(boundToService)
        {
            unbindService(serviceConnection);
            boundToService = false;
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(logReceiver);
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.http_server, menu);
        return true;
    }


	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if (v.getId() == R.id.button1)
		{
			if(!boundToService)
			{
				boolean saveToCard = ((CheckBox) findViewById(R.id.checkBox)).isChecked();
				String threadCountStr = ((EditText)findViewById(R.id.threadsNumber)).getText().toString();
				int threadCount = 5;

				try
				{
					threadCount = Integer.parseInt(threadCountStr);
				}
				catch (Exception e)
				{
					((EditText)findViewById(R.id.threadsNumber)).setText(Integer.toString(threadCount));
				}

				if(threadCount <= 0)
				{
					threadCount = 5;
					((EditText)findViewById(R.id.threadsNumber)).setText(Integer.toString(threadCount));
				}

				Intent serverIntent = new Intent(this, HttpServerService.class);
				serverIntent.putExtra("save", saveToCard);
				serverIntent.putExtra("threads", threadCount);
				startService(serverIntent);
				bindServerService();
				logAdapter.deleteAllLogs();
				logAdapter.notifyDataSetChanged();
			}
		}
		if (v.getId() == R.id.button2) {
		    if(boundToService)
		    {
                Intent serverIntent = new Intent(this, HttpServerService.class);
                unbindService(serviceConnection);
                boundToService = false;
                stopService(serverIntent);
				TextView status = findViewById(R.id.status);
				status.setText(R.string.server_not_running);
				status.setBackgroundResource(android.R.color.holo_red_dark);
            }
		}
	}


	private void bindServerService()
	{
		try
		{
			Intent serverIntent = new Intent(this, HttpServerService.class);
			bindService(serverIntent, serviceConnection, 0);
		}
		catch (SecurityException e)
		{
			e.printStackTrace();
		}
	}

	private void requestLogData()
	{
		if(!boundToService)
			return;

		try
		{
			Message msg = new Message();
			msg.what = HttpServerService.SEND_ALLDATA;
			messenger.send(msg);
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}
	}
}
