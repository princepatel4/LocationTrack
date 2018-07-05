package com.locationtracking.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.locationtracking.R;
import com.locationtracking.utils.Constants;

public class ForegroundLocationFetching extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        foregroundServiceStart();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Used only in case of bound services.
        return null;
    }


    private void foregroundServiceStart(){
        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_launcher);

        Notification notification = new Notification.Builder(this)
                .setContentTitle("Location tracking")
                .setTicker("Location")
                .setContentText("My Location")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(
                        Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setOngoing(true).build();
        startForeground(Constants.FROEGROUND_SERVICE,
                notification);
    }
}
