package com.example.karwaan.Notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
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

    public static final String CHANNEL_ID = "channel";
    public static final String ACTION_PREVIOUS = "actionprevious";
    public static final String ACTION_PLAY = "actionplay";
    public static final String ACTION_NEXT = "actionnext";
    public static final String ACTION_FORWARD = "actionforward";
    public static final String ACTION_BACKWARD = "actionbackward";

    private static Notification notification;
    private static Bitmap bitmap;
    private static String title;
    private static String artist;
    private static NotificationCompat.Builder builder1;
    private static NotificationCompat.Builder builder2;
    private static NotificationManagerCompat notificationManagerCompat;

    //public static void createNotification(Context context, SongModel song, int playbutton, Boolean isLoading, Boolean isBuffering, Boolean isPaused, String type) {

    public static void createNotification(Context context, MediaSessionCompat mediaSessionCompat, SongModel song, int playbutton, Boolean isLoading, Boolean isBuffering, Boolean isPaused, String type) {

        if (isLoading) {
            new GetMetaData(context, song, playbutton, type).execute();
            bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.placeholder);
        }

        notificationManagerCompat = NotificationManagerCompat.from(context);
        // MediaSessionCompat mediaSessionCompat = new MediaSessionCompat(context, "tag");

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
            if (isBuffering) {
                artist = "Buffering....";
            } else {
                artist = String.valueOf(artists);
            }
            title = song.getSongName();
        }

        builder1 = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_queue_music_black_24dp)
                .setContentTitle(title)
                .setContentText(artist)
                .setLargeIcon(bitmap)
                .setContentIntent(pendingIntentSaregamaActivity)
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
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        builder2 = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_queue_music_black_24dp)
                .setContentTitle(title)
                .setContentText(artist)
                .setLargeIcon(bitmap)
                .setContentIntent(pendingIntentManualActivity)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .addAction(R.drawable.ic_fast_rewind_black_24dp, "PREVIOUS", pendingIntentPrevious)
                .addAction(playbutton, "PLAY", pendingIntentPlay)
                .addAction(R.drawable.ic_fast_forward_black_24dp, "NEXT", pendingIntentNext)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mediaSessionCompat.getSessionToken()))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (isPaused || playbutton == R.drawable.play_btn_black) {
            builder1.setOngoing(false);
            builder2.setOngoing(false);
        } else {
            builder1.setOngoing(true);
            builder2.setOngoing(true);
        }

        if (type.equals("saregama")) {
            notification = builder1.build();
        }

        if (type.equals("manual")) {
            notification = builder2.build();
        }

        notificationManagerCompat.notify(999, notification);
    }

    private static class GetMetaData extends AsyncTask<Void, Void, Void> {

        Context context;
        SongModel song;
        int playbutton;
        String type;

        GetMetaData(Context context, SongModel song, int playbutton, String type) {
            this.context = context;
            this.song = song;
            this.playbutton = playbutton;
            this.type = type;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(song.getUrl(), new HashMap<String, String>());
            byte[] data = mmr.getEmbeddedPicture();

            if (data != null) {
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            } else {
                bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.placeholder);
            }
            mmr.release();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (type.equals("saregama")) {
                builder1.setLargeIcon(bitmap);
                notification = builder1.build();
            }

            if (type.equals("manual")) {
                builder2.setLargeIcon(bitmap);
                notification = builder2.build();
            }

            notificationManagerCompat.notify(999, notification);
        }
    }
}
