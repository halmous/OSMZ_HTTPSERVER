package com.example.jhalm.httpserver;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class LogAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private List<ActivityLog> logList;

    public LogAdapter(Context appContext)
    {
        logList = new ArrayList<ActivityLog>();
        inflater = LayoutInflater.from(appContext);
    }

    public void addLog(ActivityLog log)
    {
        this.logList.add(log);
    }

    @Override
    public int getCount() {
        return logList.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    public void deleteAllLogs()
    {
        this.logList.clear();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        view = inflater.inflate(R.layout.log_list_layout, null);
        TextView name = (TextView) view.findViewById(R.id.Uri);
        TextView custom1 = (TextView) view.findViewById(R.id.Custom1);
        TextView custom2 = (TextView) view.findViewById(R.id.Custom2);
        name.setText("URI: " + this.logList.get(i).URI);
        custom1.setText(this.logList.get(i).custom1);
        custom2.setText(this.logList.get(i).custom2);

        return view;
    }
}
