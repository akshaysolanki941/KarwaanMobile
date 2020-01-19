package com.example.karwaan.Services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.SpannableStringBuilder;
import android.widget.Toast;

import com.example.karwaan.Models.SongModel;
import com.example.karwaan.R;
import com.example.karwaan.SaregamaOfflineActivity;
import com.example.karwaan.Utils.EncryptDecryptUtils;
import com.example.karwaan.Utils.FilesUtil;
import com.example.karwaan.Utils.TinyDB;

import org.apache.commons.io.FileUtils;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media.MediaBrowserServiceCompat;

import static com.example.karwaan.Constants.Constants.ACTION_BACKWARD;
import static com.example.karwaan.Constants.Constants.ACTION_FORWARD;
import static com.example.karwaan.Constants.Constants.ACTION_NEXT;
import static com.example.karwaan.Constants.Constants.ACTION_PLAY;
import static com.example.karwaan.Constants.Constants.ACTION_PREVIOUS;
import static com.example.karwaan.Constants.Constants.CHANNEL_ID;

public class SaregamaOfflinePlaybackService extends MediaBrowserServiceCompat {

    private static final String MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id";

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;

    private MediaPlayer mediaPlayer;

    private Notification notification;
    private Bitmap bitmap;
    private String title;
    private String artist;
    private NotificationCompat.Builder builder1;
    private NotificationManagerCompat notificationManagerCompat;

    private ArrayList<Object> songList = new ArrayList<>();
    private ArrayList<Object> mainSongList = new ArrayList<>();
    private int index = 0;
    private TextToSpeech textToSpeech;
    private NotificationManager notificationManager;
    private float volume = 0;
    private AudioManager audioManager;

    private Boolean skip10SongsEnabled;
    private FileDescriptor fileDescriptor;
    private TinyDB tinyDB;


    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot(MY_EMPTY_MEDIA_ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSession = new MediaSessionCompat(getApplicationContext(), "tag");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        stateBuilder = new PlaybackStateCompat.Builder().setActions(
                PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackStateCompat.ACTION_REWIND |
                        PlaybackStateCompat.ACTION_FAST_FORWARD);
        mediaSession.setPlaybackState(stateBuilder.build());
        mediaSession.setCallback(callback);
        setSessionToken(mediaSession.getSessionToken());

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        registerReceiver(broadcastReceiver, new IntentFilter("SONGS"));
        startService(new Intent(getBaseContext(), OnClearFromRecentService.class));

        registerReceiver(headphoneBroadcast, new IntentFilter(Intent.ACTION_HEADSET_PLUG));

        tinyDB = new TinyDB(this);
        mainSongList.clear();
        mainSongList = tinyDB.getListObject("downloadedSongList", SongModel.class);
        songList.clear();
        songList.addAll(mainSongList);
        Intent i = new Intent("loadingDismiss");
        i.putExtra("loadingDismiss", "loadingDismiss");
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
        if (mainSongList.isEmpty()) {
            Toast.makeText(this, "No offline songs", Toast.LENGTH_SHORT).show();
        } else {
            startRandomSongs();
        }

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.getDefault());
                }
            }
        });


        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            AudioFocusRequest audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setOnAudioFocusChangeListener(afChangeListener)
                    .setAudioAttributes(attrs)
                    .build();
            audioManager.requestAudioFocus(audioFocusRequest);
        }

        skip10SongsEnabled = getSharedPreferences("karvaanSharedPref", MODE_PRIVATE).getBoolean("skip10SongsEnabled", false);
    }

    private MediaSessionCompat.Callback callback = new MediaSessionCompat.Callback() {
        @Override
        public void onPlay() {
            super.onPlay();
            stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 0);
            mediaSession.setPlaybackState(stateBuilder.build());
            playPauseSong();
        }

        @Override
        public void onPause() {
            super.onPause();
            stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, 0, 0);
            mediaSession.setPlaybackState(stateBuilder.build());
            playPauseSong();
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            stateBuilder.setState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT, 0, 0);
            mediaSession.setPlaybackState(stateBuilder.build());
            nextSong();
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            stateBuilder.setState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS, 0, 0);
            mediaSession.setPlaybackState(stateBuilder.build());
            previousSong();
        }

        @Override
        public void onFastForward() {
            stateBuilder.setState(PlaybackStateCompat.STATE_FAST_FORWARDING, 0, 0);
            mediaSession.setPlaybackState(stateBuilder.build());
            skip10SongsForward();
            super.onFastForward();
        }

        @Override
        public void onRewind() {
            stateBuilder.setState(PlaybackStateCompat.STATE_REWINDING, 0, 0);
            mediaSession.setPlaybackState(stateBuilder.build());
            skip10SongsBackward();
            super.onRewind();
        }
    };

    private void setUpMediaPlayer(FileDescriptor fileDescriptor) {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaPlayer.start();
                SongModel song = (SongModel) songList.get(index);
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
                MediaMetadataCompat.Builder mediaMetadata = new MediaMetadataCompat.Builder()
                        .putString(MediaMetadata.METADATA_KEY_TITLE, song.getSongName())
                        .putString(MediaMetadata.METADATA_KEY_ARTIST, String.valueOf(artists))
                        .putString(MediaMetadata.METADATA_KEY_ALBUM, song.getMovie())
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer.getDuration());
                mediaSession.setMetadata(mediaMetadata.build());
                stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 0);
                mediaSession.setPlaybackState(stateBuilder.build());
                createNotification(getBaseContext(), mediaSession, song, fileDescriptor, R.drawable.pause_btn_black, false);
            }
        });

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                        mediaPlayer.reset();
                    } else {
                        mediaPlayer.stop();
                        mediaPlayer.reset();
                    }
                }
                index++;
                if (index >= songList.size()) {
                    index = 0;
                    Collections.shuffle(songList);
                    playSongInExoPlayer();
                } else {
                    playSongInExoPlayer();
                }
                createNotification(getBaseContext(), mediaSession, (SongModel) songList.get(index), fileDescriptor, R.drawable.play_btn_black, true);
            }
        });

        try {
            mediaPlayer.setDataSource(fileDescriptor);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void playSongInExoPlayer() {
        byte[] file = decrypt(index);
        if (file != null) {
            try {
                fileDescriptor = FilesUtil.getTempFileDescriptor(this, file);
                setUpMediaPlayer(fileDescriptor);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        setUpMediaPlayer(fileDescriptor);
    }

    private byte[] decrypt(int position) {
        try {
            SongModel song = (SongModel) songList.get(position);
            byte[] fileData = FilesUtil.readFile(FilesUtil.getFilePath(this, song.getSongName()));
            byte[] decryptedBytes = EncryptDecryptUtils.decode(EncryptDecryptUtils.getInstance(this).getSecretKey(), fileData);
            return decryptedBytes;
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    private void startRandomSongs() {
        index = 0;
        Collections.shuffle(songList);
        playSongInExoPlayer();

        SongModel song = (SongModel) songList.get(index);
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
        MediaMetadataCompat.Builder mediaMetadata = new MediaMetadataCompat.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, song.getSongName())
                .putString(MediaMetadata.METADATA_KEY_ARTIST, String.valueOf(artists))
                .putString(MediaMetadata.METADATA_KEY_ALBUM, song.getMovie())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer.getDuration());
        mediaSession.setMetadata(mediaMetadata.build());
        stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 0);
        mediaSession.setPlaybackState(stateBuilder.build());

        createNotification(getBaseContext(), mediaSession, (SongModel) songList.get(index), fileDescriptor, R.drawable.pause_btn_black, true);
    }

    private void playPauseSong() {
        if (!mediaPlayer.isPlaying()) {
            startplayer();
        } else {
            pausePlayer();
        }
    }

    private void nextSong() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.reset();
            } else {
                mediaPlayer.stop();
                mediaPlayer.reset();
            }
        }
        index++;
        if (index >= songList.size()) {
            index = 0;
            Collections.shuffle(songList);
            playSongInExoPlayer();
        } else {
            playSongInExoPlayer();
        }
        createNotification(getBaseContext(), mediaSession, (SongModel) songList.get(index), fileDescriptor, R.drawable.play_btn_black, true);
    }

    private void previousSong() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.reset();
            } else {
                mediaPlayer.stop();
                mediaPlayer.reset();
            }
        }
        index--;
        if (index <= -1) {
            index = 0;
            Collections.shuffle(songList);
            playSongInExoPlayer();
        } else {
            playSongInExoPlayer();
        }
        createNotification(getBaseContext(), mediaSession, (SongModel) songList.get(index), fileDescriptor, R.drawable.play_btn_black, true);
    }

    private void skip10SongsForward() {
        if (skip10SongsEnabled) {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                } else {
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                }
            }
            textToSpeech.speak("Ten songs skipped", TextToSpeech.QUEUE_FLUSH, null, null);
            Toast.makeText(getBaseContext(), "Skipping 10 songs in forward direction", Toast.LENGTH_SHORT).show();
            index += 10;
            if (index >= songList.size()) {
                index = 0;
                Collections.shuffle(songList);
                playSongInExoPlayer();
            } else {
                playSongInExoPlayer();
            }
            createNotification(getBaseContext(), mediaSession, (SongModel) songList.get(index), fileDescriptor, R.drawable.play_btn_black, true);
        } else {
            long position = mediaPlayer.getCurrentPosition() + 10000;
            if (position < mediaPlayer.getDuration()) {
                mediaPlayer.seekTo((int) position);
                Toast.makeText(this, "Forwarded 10 seconds", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Song is about to end", Toast.LENGTH_SHORT).show();
                Intent i = new Intent("loadingDismiss");
                i.putExtra("loadingDismiss", "loadingDismiss");
                LocalBroadcastManager.getInstance(this).sendBroadcast(i);
            }
        }
    }

    private void skip10SongsBackward() {
        if (skip10SongsEnabled) {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                } else {
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                }
            }
            textToSpeech.speak("Ten songs skipped", TextToSpeech.QUEUE_FLUSH, null, null);
            Toast.makeText(getBaseContext(), "Skipping 10 songs in backward direction", Toast.LENGTH_SHORT).show();
            index -= 10;
            if (index <= -1) {
                index = 0;
                Collections.shuffle(songList);
                playSongInExoPlayer();
            } else {
                playSongInExoPlayer();
            }
            createNotification(getBaseContext(), mediaSession, (SongModel) songList.get(index), fileDescriptor, R.drawable.play_btn_black, true);
        } else {
            long position = mediaPlayer.getCurrentPosition() - 10000;
            if (position > 0) {
                mediaPlayer.seekTo((int) position);
                Toast.makeText(this, "Rewinded 10 seconds", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Song starting position", Toast.LENGTH_SHORT).show();
                Intent i = new Intent("loadingDismiss");
                i.putExtra("loadingDismiss", "loadingDismiss");
                LocalBroadcastManager.getInstance(this).sendBroadcast(i);
            }
        }
    }

    private void startplayer() {
        mediaPlayer.setVolume(0, 0);
        mediaPlayer.start();
        startFadeIn();
        SongModel song = (SongModel) songList.get(index);
        stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 0);
        mediaSession.setPlaybackState(stateBuilder.build());
        createNotification(getBaseContext(), mediaSession, song, fileDescriptor, R.drawable.pause_btn_black, false);
    }

    private void pausePlayer() {
        mediaPlayer.pause();
        SongModel song = (SongModel) songList.get(index);
        stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, 0, 0);
        mediaSession.setPlaybackState(stateBuilder.build());
        createNotification(getBaseContext(), mediaSession, song, fileDescriptor, R.drawable.play_btn_black, false);
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getExtras().getString("actionname");

            switch (action) {
                case ACTION_PREVIOUS:
                    previousSong();
                    break;

                case ACTION_PLAY:
                    playPauseSong();
                    break;

                case ACTION_NEXT:
                    nextSong();
                    break;

                case ACTION_FORWARD:
                    skip10SongsForward();
                    break;

                case ACTION_BACKWARD:
                    skip10SongsBackward();
                    break;
            }
        }
    };

    private AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                // Pause playback because your Audio Focus was
                // temporarily stolen, but will be back soon.
                // i.e. for a phone call
                /*if (isExoPlaying()) {
                    playPauseSong();
                }*/
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        playPauseSong();
                    }
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                // Stop playback, because you lost the Audio Focus.
                // i.e. the user started some other playback app
                // Remember to unregister your controls/buttons here.
                // And release the kra — Audio Focus!
                // You’re done.
                /*if (isExoPlaying()) {
                    playPauseSong();
                }*/
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        playPauseSong();
                    }
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                // Lower the volume, because something else is also
                // playing audio over you.
                // i.e. for notifications or navigation directions
                // Depending on your audio playback, you may prefer to
                // pause playback here instead. You do you.
                /*if (isExoPlaying()) {
                    exoPlayer.setVolume(0.2f);
                }*/
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.setVolume(0.2f, 0.2f);
                    }
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                // Resume playback, because you hold the Audio Focus
                // again!
                // i.e. the phone call ended or the nav directions
                // are finished
                // If you implement ducking and lower the volume, be
                // sure to return it to normal here, as well.
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(1f, 1f);
                    startplayer();
                }
            }
        }
    };

    private BroadcastReceiver headphoneBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        //Headset is unplugged
                        if (mediaPlayer != null) {
                            if (mediaPlayer.isPlaying()) {
                                pausePlayer();
                            }
                        }
                        break;
                    case 1:
                        //Headset is plugged
                        break;
                    default:
                        //I have no idea what the headset state is
                }
            }
        }
    };

    private void startFadeIn() {

        volume = 0;

        final int FADE_DURATION = 3000; //The duration of the fade
        //The amount of time between volume changes. The smaller this is, the smoother the fade
        final int FADE_INTERVAL = 250;
        final int MAX_VOLUME = 1; //The volume will increase from 0 to 1
        int numberOfSteps = FADE_DURATION / FADE_INTERVAL; //Calculate the number of fade steps
        //Calculate by how much the volume changes each step
        final float deltaVolume = MAX_VOLUME / (float) numberOfSteps;

        //Create a new Timer and Timer task to run the fading outside the main UI thread
        final Timer timer = new Timer(true);
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                fadeInStep(deltaVolume); //Do a fade step
                //Cancel and Purge the Timer if the desired volume has been reached
                if (volume >= 1f) {
                    timer.cancel();
                    timer.purge();
                }
            }
        };

        timer.schedule(timerTask, FADE_INTERVAL, FADE_INTERVAL);
    }

    private void fadeInStep(float deltaVolume) {
        mediaPlayer.setVolume(volume, volume);
        volume += deltaVolume;

    }

    private void createNotification(Context context, MediaSessionCompat mediaSessionCompat, SongModel song, FileDescriptor fileDescriptor, int playbutton, Boolean isLoading) {

        bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.placeholder);

        notificationManagerCompat = NotificationManagerCompat.from(context);

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

        PendingIntent pendingIntentSaregamaOfflineActivity;
        Intent intentSaregamaOfflineActivity = new Intent(context, SaregamaOfflineActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntentSaregamaOfflineActivity = PendingIntent.getActivity(context, 6, intentSaregamaOfflineActivity, PendingIntent.FLAG_UPDATE_CURRENT);

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
            artist = "(Offline) ".concat(String.valueOf(artists));
            title = song.getSongName();
        }

        builder1 = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_queue_music_black_24dp)
                .setContentTitle(title)
                .setContentText(artist)
                .setLargeIcon(bitmap)
                .setContentIntent(pendingIntentSaregamaOfflineActivity)
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
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        notification = builder1.build();
        startForeground(999, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        unregisterReceiver(broadcastReceiver);
        unregisterReceiver(headphoneBroadcast);
        stopForeground(true);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
        FileUtils.deleteQuietly(getCacheDir());
        FileUtils.deleteQuietly(getExternalCacheDir());
        stopSelf();
    }
}
