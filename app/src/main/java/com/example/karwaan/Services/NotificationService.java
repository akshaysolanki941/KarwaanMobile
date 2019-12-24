package com.example.karwaan.Services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.os.Build;
import android.os.IBinder;
import android.transition.Visibility;
import android.widget.Toast;

import com.example.karwaan.Constants;
import com.example.karwaan.R;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class NotificationService extends Service {

    public static final String CHANNEL_ID = "channel1";
    public static final String CHANNEL_NAME = "CHANNEL";
    private Notification notification;

    public NotificationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            createNotificationChannel();
            showNotification();
            Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show();
        } else if (intent.getAction().equals(Constants.ACTION.PREV_10_ACTION)) {
            Toast.makeText(this, "Clicked Previous 10", Toast.LENGTH_SHORT).show();
        } else if (intent.getAction().equals(Constants.ACTION.PREV_ACTION)) {
            Toast.makeText(this, "Clicked Previous", Toast.LENGTH_SHORT).show();
        } else if (intent.getAction().equals(Constants.ACTION.PLAY_ACTION)) {
            Toast.makeText(this, "Clicked Play", Toast.LENGTH_SHORT).show();
        } else if (intent.getAction().equals(Constants.ACTION.NEXT_ACTION)) {
            Toast.makeText(this, "Clicked Next", Toast.LENGTH_SHORT).show();
        } else if (intent.getAction().equals(Constants.ACTION.NEXT_10_ACTION)) {
            Toast.makeText(this, "Clicked Next 10", Toast.LENGTH_SHORT).show();
        } else if (intent.getAction().equals(Constants.ACTION.STOPFOREGROUND_ACTION)) {
            Toast.makeText(this, "Service Stopped", Toast.LENGTH_SHORT).show();
            stopForeground(true);
            stopSelf();
        }
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            channel.enableLights(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showNotification() {
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);

        Intent playIntent = new Intent(this, NotificationService.class);
        playIntent.setAction(Constants.ACTION.PLAY_ACTION);
        PendingIntent pplayIntent = PendingIntent.getService(this, 1, playIntent, 0);

        Intent nextIntent = new Intent(this, NotificationService.class);
        nextIntent.setAction(Constants.ACTION.NEXT_ACTION);
        PendingIntent pnextIntent = PendingIntent.getService(this, 2, nextIntent, 0);

        Intent previousIntent = new Intent(this, NotificationService.class);
        previousIntent.setAction(Constants.ACTION.PREV_ACTION);
        PendingIntent ppreviousIntent = PendingIntent.getService(this, 3, previousIntent, 0);

        Intent next10Intent = new Intent(this, NotificationService.class);
        next10Intent.setAction(Constants.ACTION.NEXT_10_ACTION);
        PendingIntent pnext10Intent = PendingIntent.getService(this, 4, next10Intent, 0);

        Intent prev10Intent = new Intent(this, NotificationService.class);
        prev10Intent.setAction(Constants.ACTION.PREV_10_ACTION);
        PendingIntent pprev10Intent = PendingIntent.getService(this, 5, prev10Intent, 0);

        Intent stopForegroundIntent = new Intent(this, NotificationService.class);
        stopForegroundIntent.setAction(Constants.ACTION.PREV_10_ACTION);
        PendingIntent pstopForegroundIntent = PendingIntent.getService(this, 5, stopForegroundIntent, 0);

        notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle("Test title")
                .setContentText("test content")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .addAction(R.drawable.ic_replay_10_black_24dp, Constants.ACTION.PREV_10_ACTION, pprev10Intent)
                .addAction(R.drawable.ic_fast_rewind_black_24dp, Constants.ACTION.PREV_ACTION, ppreviousIntent)
                .addAction(R.drawable.play_button, Constants.ACTION.PLAY_ACTION, pplayIntent)
                .addAction(R.drawable.ic_fast_forward_black_24dp, Constants.ACTION.NEXT_ACTION, pnextIntent)
                .addAction(R.drawable.ic_forward_10_black_24dp, Constants.ACTION.NEXT_10_ACTION, pnext10Intent)
                .addAction(R.drawable.ic_cancel_black_24dp, Constants.ACTION.STOPFOREGROUND_ACTION, pstopForegroundIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(1, 2, 3))
                .build();

        notificationManagerCompat.notify(1, notification);
       // startForeground(1, notification);
    }
}
