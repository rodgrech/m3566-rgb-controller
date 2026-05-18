package com.codex.m3566lighttester;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import java.io.IOException;

public class LightApiService extends Service {
    static final String ACTION_START = "com.codex.m3566lighttester.START_API";
    static final String ACTION_STOP = "com.codex.m3566lighttester.STOP_API";

    private static final int NOTIFICATION_ID = 3566;
    private static final String CHANNEL_ID = "m3566_light_api";

    private LightApiServer server;

    @Override
    public void onCreate() {
        super.onCreate();
        server = new LightApiServer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, buildNotification("API available at http://" + NetworkAddress.getLanIpAddress() + ":" + LightApiServer.PORT));
        try {
            server.start();
        } catch (IOException e) {
            startForeground(NOTIFICATION_ID, buildNotification("API failed: " + e.getMessage()));
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (server != null) {
            server.stop();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification buildNotification(String text) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "M3566 Light API", NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(channel);
        }

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        return builder
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("M3566 Light API")
                .setContentText(text)
                .setOngoing(true)
                .build();
    }

}
