package com.example.karwaan.Services;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media.MediaBrowserServiceCompat;

public class SaregamaPlaybackService extends MediaBrowserServiceCompat {

    private static final String MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id";

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;

    private ExoPlayer exoPlayer;
    private BandwidthMeter bandwidthMeter;
    private ExtractorsFactory extractorsFactory;
    private TrackSelection.Factory trackSelectionfactory;
    private DataSource.Factory dataSourceFactory;

    private ArrayList<SongModel> songList = new ArrayList<>();
    private ArrayList<SongModel> mainSongList = new ArrayList<>();
    private int index = 0;
    private TextToSpeech textToSpeech;
    private DatabaseReference songRef;
    private NotificationManager notificationManager;
    private int volumeLevel;
    private AudioManager audioManager;

    public SaregamaPlaybackService() {
    }

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
        LocalBroadcastManager.getInstance(this).registerReceiver(chipBroadcast, new IntentFilter("chip"));

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.getDefault());
                }
            }
        });

        initExoPlayer();
        getSongsList();

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            AudioFocusRequest audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setOnAudioFocusChangeListener(afChangeListener)
                    .setAudioAttributes(attrs)
                    .build();
        }
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

    private void initExoPlayer() {
        bandwidthMeter = new DefaultBandwidthMeter();
        extractorsFactory = new DefaultExtractorsFactory();
        trackSelectionfactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, getPackageName()), (TransferListener) bandwidthMeter);
        exoPlayer = ExoPlayerFactory.newSimpleInstance(this, new DefaultTrackSelector(trackSelectionfactory));
        exoPlayer.addListener(new Player.EventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                switch (playbackState) {
                    case Player.STATE_BUFFERING:
                        stateBuilder.setState(PlaybackStateCompat.STATE_BUFFERING, 0, 0);
                        mediaSession.setPlaybackState(stateBuilder.build());
                        CreateNotification.createNotification(getBaseContext(), mediaSession, songList.get(index), R.drawable.play_btn_black, false, true, false, "saregama");
                        break;

                    case Player.STATE_ENDED:
                        index++;
                        if (index >= songList.size() + 1) {
                            index = 0;
                            Collections.shuffle(songList);
                            playSongInExoPlayer(songList.get(index));
                        } else {
                            playSongInExoPlayer(songList.get(index));
                        }
                        CreateNotification.createNotification(getBaseContext(), mediaSession, songList.get(index), R.drawable.play_btn_black, true, false, false, "saregama");
                        break;

                    case Player.STATE_READY:
                        SpannableStringBuilder artists = new SpannableStringBuilder();
                        ArrayList<String> artistList = songList.get(index).getArtists();
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
                                .putString(MediaMetadata.METADATA_KEY_TITLE, songList.get(index).getSongName())
                                .putString(MediaMetadata.METADATA_KEY_ARTIST, String.valueOf(artists));
                        mediaSession.setMetadata(mediaMetadata.build());
                        break;
                }
                if (playWhenReady && playbackState == Player.STATE_READY) {
                    // media actually playing
                    stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 0);
                    mediaSession.setPlaybackState(stateBuilder.build());
                    CreateNotification.createNotification(getBaseContext(), mediaSession, songList.get(index), R.drawable.pause_btn_black, false, false, false, "saregama");
                } else if (playWhenReady) {
                    // might be idle (plays after prepare()),
                    // buffering (plays when data available)
                    // or ended (plays when seek away from end)
                } else {
                    // player paused in any state
                    stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, 0, 0);
                    mediaSession.setPlaybackState(stateBuilder.build());
                    CreateNotification.createNotification(getBaseContext(), mediaSession, songList.get(index), R.drawable.play_btn_black, false, false, true, "saregama");
                }
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                Toast.makeText(getBaseContext(), "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void playSongInExoPlayer(SongModel song) {
        MediaSource mediaSource = new ExtractorMediaSource(Uri.parse(song.getUrl()), dataSourceFactory, extractorsFactory, null, Throwable::printStackTrace);
        exoPlayer.prepare(mediaSource);
        exoPlayer.setPlayWhenReady(true);
    }


    private void getSongsList() {
        if (!mainSongList.isEmpty()) {
            mainSongList.clear();
        }
        songRef = FirebaseDatabase.getInstance().getReference("Songs");
        songRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    String songName = ds.child("songName").getValue(String.class);
                    String url = ds.child("url").getValue(String.class);
                    ArrayList<String> artistsList = new ArrayList<>();
                    for (DataSnapshot ds1 : ds.child("artists").getChildren()) {
                        artistsList.add(ds1.getValue(String.class));
                    }
                    mainSongList.add(new SongModel(url, songName, artistsList));
                }
                if (!mainSongList.isEmpty()) {
                    songList.clear();
                    songList.addAll(mainSongList);
                    startRandomSongs();
                } else {
                    Toast.makeText(getBaseContext(), "No Songs Found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getBaseContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startRandomSongs() {
        index = 0;
        Collections.shuffle(songList);
        playSongInExoPlayer(songList.get(index));
        CreateNotification.createNotification(getBaseContext(), mediaSession, songList.get(index), R.drawable.pause_btn_black, true, false, false, "saregama");
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
        index++;
        if (index >= songList.size()) {
            index = 0;
            Collections.shuffle(songList);
            playSongInExoPlayer(songList.get(index));
        } else {
            playSongInExoPlayer(songList.get(index));
        }
        CreateNotification.createNotification(getBaseContext(), mediaSession, songList.get(index), R.drawable.play_btn_black, true, false, false, "saregama");
    }

    private void previousSong() {
        index--;
        if (index <= -1) {
            index = 0;
            Collections.shuffle(songList);
            playSongInExoPlayer(songList.get(index));
        } else {
            playSongInExoPlayer(songList.get(index));
        }
        CreateNotification.createNotification(getBaseContext(), mediaSession, songList.get(index), R.drawable.play_btn_black, true, false, false, "saregama");
    }

    private void skip10SongsForward() {
        textToSpeech.speak("Ten songs skipped", TextToSpeech.QUEUE_FLUSH, null, null);
        Toast.makeText(getBaseContext(), "Skipping 10 songs in forward direction", Toast.LENGTH_SHORT).show();
        index += 10;
        if (index >= songList.size()) {
            index = 0;
            Collections.shuffle(songList);
            playSongInExoPlayer(songList.get(index));
        } else {
            playSongInExoPlayer(songList.get(index));
        }
        CreateNotification.createNotification(getBaseContext(), mediaSession, songList.get(index), R.drawable.play_btn_black, true, false, false, "saregama");
    }

    private void skip10SongsBackward() {
        textToSpeech.speak("Ten songs skipped", TextToSpeech.QUEUE_FLUSH, null, null);
        Toast.makeText(getBaseContext(), "Skipping 10 songs in backward direction", Toast.LENGTH_SHORT).show();
        index -= 10;
        if (index <= -1) {
            index = 0;
            Collections.shuffle(songList);
            playSongInExoPlayer(songList.get(index));
        } else {
            playSongInExoPlayer(songList.get(index));
        }
        CreateNotification.createNotification(getBaseContext(), mediaSession, songList.get(index), R.drawable.play_btn_black, true, false, false, "saregama");
    }

    private void pausePlayer() {
        exoPlayer.setPlayWhenReady(false);
    }

    private void startPlayer() {
        exoPlayer.setPlayWhenReady(true);
    }

    private boolean isExoPlaying() {
        return exoPlayer.getPlayWhenReady();
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getExtras().getString("actionname");

            switch (action) {
                case CreateNotification.ACTION_PREVIOUS:
                    previousSong();
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

    private BroadcastReceiver chipBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("chip")) {
                songList.clear();
                songList = (ArrayList<SongModel>) intent.getSerializableExtra("list");
                startRandomSongs();
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
