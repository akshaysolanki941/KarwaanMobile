package com.example.karwaan.Services;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.SpannableStringBuilder;
import android.widget.Toast;

import com.example.karwaan.Models.SongModel;
import com.example.karwaan.Notification.CreateNotification;
import com.example.karwaan.R;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media.MediaBrowserServiceCompat;

public class ManualPlaybackService extends MediaBrowserServiceCompat {

    private static final String MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id";

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;

    private ExoPlayer exoPlayer;
    private BandwidthMeter bandwidthMeter;
    private ExtractorsFactory extractorsFactory;
    private TrackSelection.Factory trackSelectionfactory;
    private DataSource.Factory dataSourceFactory;

    private int index = 0;
    private TextToSpeech textToSpeech;
    private NotificationManager notificationManager;
    private Boolean skip10SongsEnabled;
    private ArrayList<SongModel> mainSongsList = new ArrayList<>();
    private ArrayList<SongModel> songs = new ArrayList<>();
    private final Handler handler = new Handler();
    private AudioManager audioManager;
    private int volumeLevel;

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

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.getDefault());
                }
            }
        });

        registerReceiver(broadcastReceiver, new IntentFilter("SONGS"));
        startService(new Intent(getBaseContext(), OnClearFromRecentService.class));

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast1, new IntentFilter("mainSongList"));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast2, new IntentFilter("searchList"));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast3, new IntentFilter("chip"));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast4, new IntentFilter("holderItemOnClick"));

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

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

        skip10SongsEnabled = getSharedPreferences("skip10SongsEnabled", MODE_PRIVATE).getBoolean("skip10SongsEnabled", false);

        initExoPlayer();
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
            prevSong();
        }

        @Override
        public void onFastForward() {
            super.onFastForward();
            stateBuilder.setState(PlaybackStateCompat.STATE_FAST_FORWARDING, 0, 0);
            mediaSession.setPlaybackState(stateBuilder.build());
            skip10SongsForward();
        }

        @Override
        public void onRewind() {
            super.onRewind();
            stateBuilder.setState(PlaybackStateCompat.STATE_REWINDING, 0, 0);
            mediaSession.setPlaybackState(stateBuilder.build());
            skip10SongsBackward();
        }
    };

    private void initExoPlayer() {
        bandwidthMeter = new DefaultBandwidthMeter();
        extractorsFactory = new DefaultExtractorsFactory();
        trackSelectionfactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, getPackageName()), (TransferListener) bandwidthMeter);
        exoPlayer = ExoPlayerFactory.newSimpleInstance(this, new DefaultTrackSelector(trackSelectionfactory));

        exoPlayer.addListener(new Player.EventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

                if (playbackState == Player.STATE_BUFFERING) {
                    stateBuilder.setState(PlaybackStateCompat.STATE_BUFFERING, 0, 0);
                    mediaSession.setPlaybackState(stateBuilder.build());
                    CreateNotification.createNotification(getBaseContext(), mediaSession, songs.get(index), R.drawable.play_btn_black, false, true, false, "manual");
                }

                if (playbackState == Player.STATE_READY) {
                    SpannableStringBuilder artists = new SpannableStringBuilder();
                    ArrayList<String> artistList = songs.get(index).getArtists();
                    for (int i = 0; i < artistList.size(); i++) {
                        String a = artistList.get(i);
                        if (i == artistList.size() - 1) {
                            artists.append(a);
                        } else {
                            artists.append(a).append(" | ");
                        }
                    }
                    mediaSession.setPlaybackState(stateBuilder.build());
                    MediaMetadataCompat.Builder mediaMetadata = new MediaMetadataCompat.Builder()
                            .putString(MediaMetadata.METADATA_KEY_TITLE, songs.get(index).getSongName())
                            .putString(MediaMetadata.METADATA_KEY_ARTIST, String.valueOf(artists))
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, exoPlayer.getDuration());
                    mediaSession.setMetadata(mediaMetadata.build());
                    showCurrentTime();
                }

                if (playbackState == Player.STATE_ENDED) {
                    stateBuilder.setState(PlaybackStateCompat.STATE_STOPPED, 0, 0);
                    mediaSession.setPlaybackState(stateBuilder.build());
                    CreateNotification.createNotification(getBaseContext(), mediaSession, songs.get(index), R.drawable.play_btn_black, false, false, true, "manual");
                }

                if (playWhenReady && playbackState == Player.STATE_READY) {
                    stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 0);
                    mediaSession.setPlaybackState(stateBuilder.build());
                    // media actually playing
                    CreateNotification.createNotification(getBaseContext(), mediaSession, songs.get(index), R.drawable.pause_btn_black, false, false, false, "manual");
                } else if (playWhenReady) {
                    // might be idle (plays after prepare()),
                    // buffering (plays when data available)
                    // or ended (plays when seek away from end)
                    // Toast.makeText(ManualActivity.this, "Buffering....", Toast.LENGTH_SHORT).show();
                } else {
                    //media paused
                    stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, 0, 0);
                    mediaSession.setPlaybackState(stateBuilder.build());
                    CreateNotification.createNotification(getBaseContext(), mediaSession, songs.get(index), R.drawable.play_btn_black, false, false, true, "manual");
                }
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                Toast.makeText(getBaseContext(), "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setUpExoPlayer(SongModel song) {
        MediaSource mediaSource = new ExtractorMediaSource(Uri.parse(song.getUrl()), dataSourceFactory, extractorsFactory, null, Throwable::printStackTrace);
        exoPlayer.prepare(mediaSource);
        exoPlayer.setPlayWhenReady(true);
    }

    private void playPauseSong() {
        if (!isExoPlaying()) {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            AudioFocusRequest audioFocusRequest = null;
            int result = 0;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setOnAudioFocusChangeListener(afChangeListener)
                        .setAudioAttributes(attrs)
                        .build();
                result = audioManager.requestAudioFocus(audioFocusRequest);
            }

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                startPlayer();
                if (audioManager != null) {
                    volumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                }
            }
        } else {
            pausePlayer();
        }
    }

    private void nextSong() {
        if (index + 1 >= songs.size()) {
            Toast.makeText(getBaseContext(), "This is the last song", Toast.LENGTH_SHORT).show();
            Intent i = new Intent("loadingDismiss");
            i.putExtra("loadingDismiss", "loadingDismiss");
            LocalBroadcastManager.getInstance(this).sendBroadcast(i);
        } else {
            index++;
            SongModel nextSong = songs.get(index);
            CreateNotification.createNotification(getBaseContext(), mediaSession, nextSong, R.drawable.play_btn_black, true, false, false, "manual");
            setUpExoPlayer(nextSong);
        }
    }

    private void prevSong() {
        if (index - 1 < 0) {
            Toast.makeText(getBaseContext(), "This is the first song", Toast.LENGTH_SHORT).show();
            Intent i = new Intent("loadingDismiss");
            i.putExtra("loadingDismiss", "loadingDismiss");
            LocalBroadcastManager.getInstance(this).sendBroadcast(i);
        } else {
            index--;
            SongModel prevSong = songs.get(index);
            CreateNotification.createNotification(getBaseContext(), mediaSession, prevSong, R.drawable.play_btn_black, true, false, false, "manual");
            setUpExoPlayer(prevSong);
        }
    }

    private void skip10SongsForward() {
        if (skip10SongsEnabled) {
            textToSpeech.speak("Ten songs skipped", TextToSpeech.QUEUE_FLUSH, null, null);
            Toast.makeText(getBaseContext(), "Skipping 10 songs in forward direction", Toast.LENGTH_SHORT).show();
            index += 10;
            if (index >= songs.size()) {
                index = 0;
                setUpExoPlayer(songs.get(index));
            } else {
                setUpExoPlayer(songs.get(index));
            }
            CreateNotification.createNotification(getBaseContext(), mediaSession, songs.get(index), R.drawable.play_btn_black, true, false, false, "manual");
        } else {
            long position = exoPlayer.getCurrentPosition() + 10000;
            if (position < exoPlayer.getDuration()) {
                exoPlayer.seekTo(position);
                Toast.makeText(this, "Forwarded 10 seconds", Toast.LENGTH_SHORT).show();
            } else {
                Intent i = new Intent("loadingDismiss");
                i.putExtra("loadingDismiss", "loadingDismiss");
                LocalBroadcastManager.getInstance(this).sendBroadcast(i);
                Toast.makeText(this, "Song is about to end", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void skip10SongsBackward() {
        if (skip10SongsEnabled) {
            textToSpeech.speak("Ten songs skipped", TextToSpeech.QUEUE_FLUSH, null, null);
            Toast.makeText(getBaseContext(), "Skipping 10 songs in backward direction", Toast.LENGTH_SHORT).show();
            index -= 10;
            if (index <= -1) {
                index = 0;
                setUpExoPlayer(songs.get(index));
            } else {
                setUpExoPlayer(songs.get(index));
            }
            CreateNotification.createNotification(getBaseContext(), mediaSession, songs.get(index), R.drawable.play_btn_black, true, false, false, "manual");
        } else {
            long position = exoPlayer.getCurrentPosition() - 10000;
            if (position > 0) {
                exoPlayer.seekTo(position);
                Toast.makeText(this, "Rewinded 10 seconds", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Song starting position", Toast.LENGTH_SHORT).show();
                Intent i = new Intent("loadingDismiss");
                i.putExtra("loadingDismiss", "loadingDismiss");
                LocalBroadcastManager.getInstance(this).sendBroadcast(i);
            }
        }
    }

    private void holderItemOnClick(int position) {
        SongModel song = songs.get(position);
        index = position;
        CreateNotification.createNotification(getBaseContext(), mediaSession, song, R.drawable.play_btn_black, true, false, false, "manual");
        setUpExoPlayer(song);
    }

    private void showCurrentTime() {
        if (isExoPlaying()) {
            Intent i = new Intent("updateCurrentTime");
            i.putExtra("updateCurrentTime", exoPlayer.getCurrentPosition());
            LocalBroadcastManager.getInstance(this).sendBroadcast(i);
            Runnable notification = new Runnable() {
                public void run() {
                    showCurrentTime();
                }
            };
            handler.postDelayed(notification, 1000);
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getExtras().getString("actionname");

            switch (action) {
                case CreateNotification.ACTION_PREVIOUS:
                    prevSong();
                    break;

                case CreateNotification.ACTION_PLAY:
                    playPauseSong();
                    break;

                case CreateNotification.ACTION_NEXT:
                    nextSong();
                    break;

                case CreateNotification.ACTION_FORWARD:
                    skip10SongsForward();
                    break;

                case CreateNotification.ACTION_BACKWARD:
                    skip10SongsBackward();
                    break;
            }
        }
    };

    private BroadcastReceiver broadcast1 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("mainSongList")) {
                mainSongsList.clear();
                mainSongsList = (ArrayList<SongModel>) intent.getSerializableExtra("mainSongList");
                songs.clear();
                songs.addAll(mainSongsList);
            }
        }
    };

    private BroadcastReceiver broadcast2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("searchList")) {
                songs.clear();
                String query = intent.getStringExtra("search");
                for (SongModel song : mainSongsList) {
                    if (song.getSongName().toLowerCase().contains(query)) {
                        songs.add(song);
                    }
                }
            }
        }
    };

    private BroadcastReceiver broadcast3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("chip")) {
                songs.clear();
                String selectedArtist = intent.getStringExtra("chip");
                if (selectedArtist.equals("All Artists")) {
                    songs.clear();
                    songs.addAll(mainSongsList);
                } else {
                    songs.clear();
                    for (SongModel song : mainSongsList) {
                        for (String artistName : song.getArtists()) {
                            if (artistName.equals(selectedArtist)) {
                                songs.add(song);
                                break;
                            }
                        }
                    }
                }
            }
        }
    };

    private BroadcastReceiver broadcast4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("holderItemOnClick")) {
                int position = intent.getIntExtra("holderItemOnClick", 0);
                holderItemOnClick(position);
            }
        }
    };

    private AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                // Pause playback because your Audio Focus was
                // temporarily stolen, but will be back soon.
                // i.e. for a phone call
                if (isExoPlaying()) {
                    playPauseSong();
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                // Stop playback, because you lost the Audio Focus.
                // i.e. the user started some other playback app
                // Remember to unregister your controls/buttons here.
                // And release the kra — Audio Focus!
                // You’re done.
                if (isExoPlaying()) {
                    playPauseSong();
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                // Lower the volume, because something else is also
                // playing audio over you.
                // i.e. for notifications or navigation directions
                // Depending on your audio playback, you may prefer to
                // pause playback here instead. You do you.
                if (isExoPlaying()) {
                    playPauseSong();
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                // Resume playback, because you hold the Audio Focus
                // again!
                // i.e. the phone call ended or the nav directions
                // are finished
                // If you implement ducking and lower the volume, be
                // sure to return it to normal here, as well.
                startPlayer();
            }
        }
    };

    private void pausePlayer() {
        exoPlayer.setPlayWhenReady(false);
    }

    private void startPlayer() {
        exoPlayer.setPlayWhenReady(true);
    }

    private boolean isExoPlaying() {
        return exoPlayer.getPlayWhenReady();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        onDestroy();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        pausePlayer();
        if (exoPlayer != null) {
            exoPlayer.release();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        unregisterReceiver(broadcastReceiver);
        stopForeground(true);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
        stopSelf();
    }
}
