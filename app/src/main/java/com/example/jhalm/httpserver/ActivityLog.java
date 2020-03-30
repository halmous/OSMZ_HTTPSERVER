package com.example.jhalm.httpserver;

import android.os.Parcel;
import android.os.Parcelable;

public class ActivityLog implements Parcelable {
    public String URI;
    public String custom1;
    public String custom2;

    public ActivityLog(){}

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i)
    {
        parcel.writeString(URI);
        parcel.writeString(custom1);
        parcel.writeString(custom2);
    }

    public static final Parcelable.Creator<ActivityLog> CREATOR = new Parcelable.Creator<ActivityLog>()
    {
        @Override
        public ActivityLog createFromParcel(Parcel parcel) {
            return new ActivityLog(parcel);
        }

        @Override
        public ActivityLog[] newArray(int i) {
            return new ActivityLog[i];
        }
    };

    private ActivityLog(Parcel parcel)
    {
        URI = parcel.readString();
        custom1 = parcel.readString();
        custom2 = parcel.readString();
    }
}
