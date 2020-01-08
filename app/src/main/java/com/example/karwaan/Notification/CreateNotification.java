package com.example.karwaan.Notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.SpannableStringBuilder;

import com.example.karwaan.ManualActivity;
import com.example.karwaan.Models.SongModel;
import com.example.karwaan.R;
import com.example.karwaan.SaregamaActivity;
import com.example.karwaan.Services.NotificationActionService;

import java.util.ArrayList;
import java.util.HashMap;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class CreateNotification {

    public static final String CHANNEL_ID = "channel_saregama";
    public static final String CHANNEL_ID1 = "channel_manual";
    public static final String ACTION_PREVIOUS = "actionprevious";
    public static final String ACTION_PLAY = "actionplay";
    public static final String ACTION_NEXT = "actionnext";
    public static final String ACTION_FORWARD = "actionforward";
    public static final String ACTION_BACKWARD = "actionbackward";

    private static Notification notification;
    private static Notification notificationManual;

    public static void createNotification(Context context, SongModel song, int playbutton, Boolean isLoading, String type) {
        Bitmap bitmap;
        String title;
        String artist;

        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        MediaSessionCompat mediaSessionCompat = new MediaSessionCompat(context, "tag");

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(song.getUrl(), new HashMap<String, String>());
        byte[] data = mmr.getEmbeddedPicture();

        if (data != null) {
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        } else {
            bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.placeholder);
        }

        PendingIntent pendingIntentPrevious;
        Intent intentPrevious = new Intent(context, NotificationActionService.class).setAction(ACTION_PREVIOUS);
        pendingIntentPrevious = PendingIntent.getBroadcast(context, 0, intentPrevious, PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent pendingIntentPlay;
        Intent intentPlay = new Intent(context, NotificationActionService.class).setAction(ACTION_PLAY);
        pendingIntentPlay = PendingIntent.getBroadcast(context, 1, intentPlay, PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent pendingIntentNext;
        Intent intentNext = new Intent(context, NotificationActionService.class).setAction(ACTION_NEXT);
        pendingIntentNext = PendingIntent.getBroadcast(context, 2, intentNext, PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent pendingIntentForward;
        Intent intentForward = new Intent(context, NotificationActionService.class).setAction(ACTION_FORWARD);
        pendingIntentForward = PendingIntent.getBroadcast(context, 3, intentForward, PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent pendingIntentBackward;
        Intent intentBackward = new Intent(context, NotificationActionService.class).setAction(ACTION_BACKWARD);
        pendingIntentBackward = PendingIntent.getBroadcast(context, 4, intentBackward, PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent pendingIntentSaregamaActivity;
        Intent intentSaregamaActivity = new Intent(context, SaregamaActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntentSaregamaActivity = PendingIntent.getActivity(context, 5, intentSaregamaActivity, PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent pendingIntentManualActivity;
        Intent intentManualActivity = new Intent(context, ManualActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntentManualActivity = PendingIntent.getActivity(context, 6, intentManualActivity, PendingIntent.FLAG_UPDATE_CURRENT);


        SpannableStringBuilder artists = new SpannableStringBuilder();
        ArrayList<String> artistList = song.getArtists();
        for (int i = 0; i < artistList.size(); i++) {
            String a = artistList.get(i);
            if (i == artistList.size() - 1) {
                artists.append(a);
            } else {
                artists.append(a).append(" | ");
            }
        }

        if (isLoading) {
            title = "Loading....";
            artist = "";
        } else {
            title = song.getSongName();
            artist = String.valueOf(artists);
        }

        if (type.equals("saregama")) {
            notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_queue_music_black_24dp)
                    .setContentTitle(title)
                    .setContentText(artist)
                    .setContentIntent(pendingIntentSaregamaActivity)
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
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setOngoing(true)
                    .build();

            notificationManagerCompat.notify(1, notification);
        }

        if (type.equals("manual")) {
            notificationManual = new NotificationCompat.Builder(context, CHANNEL_ID1)
                    .setSmallIcon(R.drawable.ic_queue_music_black_24dp)
                    .setContentTitle(title)
                    .setContentText(artist)
                    .setContentIntent(pendingIntentManualActivity)
                    .setLargeIcon(bitmap)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(false)
                    .addAction(R.drawable.ic_fast_rewind_black_24dp, "PREVIOUS", pendingIntentPrevious)
                    .addAction(playbutton, "PLAY", pendingIntentPlay)
                    .addAction(R.drawable.ic_fast_forward_black_24dp, "NEXT", pendingIntentNext)
                    .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setShowActionsInCompactView(0, 1, 2)
                            .setMediaSession(mediaSessionCompat.getSessionToken()))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setOngoing(true)
                    .build();

            notificationManagerCompat.notify(2, notificationManual);
        }
    }
    //}
}
