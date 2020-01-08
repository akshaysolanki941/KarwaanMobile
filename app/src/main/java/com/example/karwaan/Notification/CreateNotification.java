package com.example.karwaan.Notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;

import com.example.karwaan.Models.SongModel;
import com.example.karwaan.R;
import com.example.karwaan.Services.NotificationActionService;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class CreateNotification {

    public static final String CHANNEL_ID = "channel1";
    public static final String ACTION_PREVIOUS = "actionprevious";
    public static final String ACTION_PLAY = "actionplay";
    public static final String ACTION_NEXT = "actionnext";
    public static final String ACTION_FORWARD = "actionforward";
    public static final String ACTION_BACKWARD = "actionbackward";

    public static Notification notification;

    public static void createNotification(Context context, SongModel song, int playbutton, int pos, int size) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
            MediaSessionCompat mediaSessionCompat = new MediaSessionCompat(context, "tag");

            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.placeholder);

            PendingIntent pendingIntentPrevious;
            Intent intentPrevious = new Intent(context, NotificationActionService.class).setAction(ACTION_PREVIOUS);
            pendingIntentPrevious = PendingIntent.getBroadcast(context, 0, intentPrevious, PendingIntent.FLAG_UPDATE_CURRENT);

            PendingIntent pendingIntentPlay;
            Intent intentPlay = new Intent(context, NotificationActionService.class).setAction(ACTION_PLAY);
            pendingIntentPlay = PendingIntent.getBroadcast(context, 0, intentPlay, PendingIntent.FLAG_UPDATE_CURRENT);

            PendingIntent pendingIntentNext;
            Intent intentNext = new Intent(context, NotificationActionService.class).setAction(ACTION_NEXT);
            pendingIntentNext = PendingIntent.getBroadcast(context, 0, intentNext, PendingIntent.FLAG_UPDATE_CURRENT);

            PendingIntent pendingIntentForward;
            Intent intentForward = new Intent(context, NotificationActionService.class).setAction(ACTION_FORWARD);
            pendingIntentForward = PendingIntent.getBroadcast(context, 0, intentForward, PendingIntent.FLAG_UPDATE_CURRENT);

            PendingIntent pendingIntentBackward;
            Intent intentBackward = new Intent(context, NotificationActionService.class).setAction(ACTION_BACKWARD);
            pendingIntentBackward = PendingIntent.getBroadcast(context, 0, intentBackward, PendingIntent.FLAG_UPDATE_CURRENT);

            notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(song.getSongName())
                    .setContentText(song.getArtists().get(0))
                    .setLargeIcon(bitmap)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(false)
                    .addAction(R.drawable.ic_replay_10_black_24dp, "BACKWARD", pendingIntentBackward)
                    .addAction(R.drawable.ic_fast_rewind_black_24dp, "PREVIOUS", pendingIntentPrevious)
                    .addAction(playbutton, "PLAY", pendingIntentPlay)
                    .addAction(R.drawable.ic_fast_forward_black_24dp, "NEXT", pendingIntentNext)
                    .addAction(R.drawable.ic_forward_10_black_24dp, "FORWARD", pendingIntentForward)
                    .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setShowActionsInCompactView(1, 2, 3)
                            .setMediaSession(mediaSessionCompat.getSessionToken()))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();

            notificationManagerCompat.notify(1, notification);
        }
    }
}
