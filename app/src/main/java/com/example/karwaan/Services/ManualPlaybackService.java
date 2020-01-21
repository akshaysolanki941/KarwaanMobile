package com.example.karwaan.Services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.SpannableStringBuilder;
import android.widget.Toast;

import com.example.karwaan.Constants.Constants;
import com.example.karwaan.Equalizer.EqualizerSettings;
import com.example.karwaan.Equalizer.Settings;
import com.example.karwaan.ManualActivity;
import com.example.karwaan.Models.SongModel;
import com.example.karwaan.R;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
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
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
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
import static com.example.karwaan.SaregamaActivity.PREF_KEY;

public class ManualPlaybackService extends MediaBrowserServiceCompat {

    private static final String MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id";
    private static final String MEDIA_ID_ROOT = "media_root_id";
    public static final String ANDROID_AUTO_PACKAGE_NAME = "com.android.bluetooth";
    public static final String ANDROID_AUTO_EMULATOR_PACKAGE_NAME = "com.android.bluetooth";

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;

    private SimpleExoPlayer exoPlayer;
    private BandwidthMeter bandwidthMeter;
    private ExtractorsFactory extractorsFactory;
    private TrackSelection.Factory trackSelectionfactory;
    private DataSource.Factory dataSourceFactory;

    private Notification notification;
    private Bitmap bitmap;
    private String title;
    private String artist;
    private NotificationCompat.Builder builder1;
    private NotificationManagerCompat notificationManagerCompat;

    private int index = 0;
    private TextToSpeech textToSpeech;
    private NotificationManager notificationManager;
    private boolean skip10SongsEnabled;
    private ArrayList<SongModel> mainSongsList = new ArrayList<>();
    private ArrayList<SongModel> songs = new ArrayList<>();
    private final Handler handler = new Handler();
    private AudioManager audioManager;
    private float volume;
    private boolean resumeOnFocusGain;

    private int audioSessionID = 0;

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        //return new BrowserRoot(MY_EMPTY_MEDIA_ROOT_ID, null);
        // To ensure you are not allowing any arbitrary app to browse your app's contents, you
        // need to check the origin:

        return new BrowserRoot(MY_EMPTY_MEDIA_ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
       /* List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        if (MEDIA_ID_ROOT.equals(parentId)) {
            Log.d("tag", "OnLoadChildren.ROOT");
            mediaItems.add(new MediaBrowserCompat.MediaItem(
                    new MediaDescriptionCompat.Builder()
                            .setTitle(mainSongsList.get(index).getSongName())
                            .setSubtitle(mainSongsList.get(index).getArtists().get(0))
                            .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            ));
        }
        result.sendResult(mediaItems);*/
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

        registerReceiver(headphoneBroadcast, new IntentFilter(Intent.ACTION_HEADSET_PLUG));

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast1, new IntentFilter("mainSongList"));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast2, new IntentFilter("searchList"));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast3, new IntentFilter("chip"));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast4, new IntentFilter("holderItemOnClick"));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast5, new IntentFilter("seekChanged"));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast6, new IntentFilter("playlist"));

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

        skip10SongsEnabled = getSharedPreferences("karvaanSharedPref", MODE_PRIVATE).getBoolean("skip10SongsEnabled", false);

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
                    createNotification(getBaseContext(), mediaSession, songs.get(index), R.drawable.play_btn_black, false, true);
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
                            .putString(MediaMetadata.METADATA_KEY_ALBUM, songs.get(index).getMovie())
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, exoPlayer.getDuration());
                    mediaSession.setMetadata(mediaMetadata.build());
                    showCurrentTime();
                    updateSeekBar();
                }

                if (playbackState == Player.STATE_ENDED) {
                    stateBuilder.setState(PlaybackStateCompat.STATE_STOPPED, 0, 0);
                    mediaSession.setPlaybackState(stateBuilder.build());
                    createNotification(getBaseContext(), mediaSession, songs.get(index), R.drawable.play_btn_black, false, false);
                }

                if (playWhenReady && playbackState == Player.STATE_READY) {
                    stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, 0, 0);
                    mediaSession.setPlaybackState(stateBuilder.build());
                    // media actually playing
                    createNotification(getBaseContext(), mediaSession, songs.get(index), R.drawable.pause_btn_black, false, false);

                    audioSessionID = exoPlayer.getAudioSessionId();
                    if (audioSessionID > 0) {
                        Intent i = new Intent("audioSessionIDManual");
                        i.putExtra("audioSessionIDManual", audioSessionID);
                        LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i);
                    }


                } else if (playWhenReady) {
                    // might be idle (plays after prepare()),
                    // buffering (plays when data available)
                    // or ended (plays when seek away from end)
                    // Toast.makeText(ManualActivity.this, "Buffering....", Toast.LENGTH_SHORT).show();
                } else {
                    //media paused
                    stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED, 0, 0);
                    mediaSession.setPlaybackState(stateBuilder.build());
                    createNotification(getBaseContext(), mediaSession, songs.get(index), R.drawable.play_btn_black, false, false);
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
           /* AudioAttributes attrs = new AudioAttributes.Builder()
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
                exoPlayer.setVolume(0);
                startPlayer();
                startFadeIn();
                if (audioManager != null) {
                    volumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                }*/
            exoPlayer.setVolume(0);
            startPlayer();
            startFadeIn();
        } else {
            pausePlayer();
            resumeOnFocusGain = false;
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
            createNotification(getBaseContext(), mediaSession, nextSong, R.drawable.play_btn_black, true, false);
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
            createNotification(getBaseContext(), mediaSession, prevSong, R.drawable.play_btn_black, true, false);
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
            createNotification(getBaseContext(), mediaSession, songs.get(index), R.drawable.play_btn_black, true, false);
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
            createNotification(getBaseContext(), mediaSession, songs.get(index), R.drawable.play_btn_black, true, false);
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
        createNotification(getBaseContext(), mediaSession, song, R.drawable.play_btn_black, true, false);
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

    private void updateSeekBar() {
        if (isExoPlaying()) {
            Intent i = new Intent("updateSeekbar");
            i.putExtra("currentPosition", exoPlayer.getCurrentPosition());
            i.putExtra("bufferedPosition", exoPlayer.getContentBufferedPosition());
            LocalBroadcastManager.getInstance(this).sendBroadcast(i);
            Runnable notification = new Runnable() {
                public void run() {
                    updateSeekBar();
                }
            };
            handler.postDelayed(notification, 1);
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getExtras().getString("actionname");

            switch (action) {
                case Constants.ACTION_PREVIOUS:
                    prevSong();
                    break;

                case Constants.ACTION_PLAY:
                    playPauseSong();
                    break;

                case Constants.ACTION_NEXT:
                    nextSong();
                    break;

                case Constants.ACTION_FORWARD:
                    skip10SongsForward();
                    break;

                case Constants.ACTION_BACKWARD:
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

    private BroadcastReceiver headphoneBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        //Headset is unplugged
                        if (isExoPlaying()) {
                            pausePlayer();
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

    private BroadcastReceiver broadcast2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("searchList")) {
                songs.clear();
                String query = intent.getStringExtra("search");
                for (SongModel song : mainSongsList) {
                    if (song.getSongName().toLowerCase().contains(query)) {
                        songs.add(song);
                    } else if (song.getMovie().toLowerCase().contains(query)) {
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

    private BroadcastReceiver broadcast6 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("playlist")) {
                songs.clear();
                ArrayList<SongModel> playlist = (ArrayList<SongModel>) intent.getSerializableExtra("playlist");
                if (playlist != null) {
                    songs.addAll(playlist);
                }
            }
        }
    };

    private void saveEqualizerSettings() {
        if (Settings.equalizerModel != null) {
            EqualizerSettings settings = new EqualizerSettings();
            settings.bassStrength = Settings.equalizerModel.getBassStrength();
            settings.presetPos = Settings.equalizerModel.getPresetPos();
            settings.reverbPreset = Settings.equalizerModel.getReverbPreset();
            settings.seekbarpos = Settings.equalizerModel.getSeekbarpos();
            settings.isEqualizerEnabled = Settings.equalizerModel.isEqualizerEnabled();

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

            Gson gson = new Gson();
            preferences.edit().putString(PREF_KEY, gson.toJson(settings)).apply();
        }
    }

    private BroadcastReceiver broadcast5 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("seekChanged")) {
                exoPlayer.seekTo(intent.getIntExtra("seekTo", 0));
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
                    pausePlayer();
                    resumeOnFocusGain = true;
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                // Stop playback, because you lost the Audio Focus.
                // i.e. the user started some other playback app
                // Remember to unregister your controls/buttons here.
                // And release the kra — Audio Focus!
                // You’re done.
                if (isExoPlaying()) {
                    pausePlayer();
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                // Lower the volume, because something else is also
                // playing audio over you.
                // i.e. for notifications or navigation directions
                // Depending on your audio playback, you may prefer to
                // pause playback here instead. You do you.
                if (isExoPlaying()) {
                    exoPlayer.setVolume(0.2f);
                    resumeOnFocusGain = true;
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                // Resume playback, because you hold the Audio Focus
                // again!
                // i.e. the phone call ended or the nav directions
                // are finished
                // If you implement ducking and lower the volume, be
                // sure to return it to normal here, as well.
                if (resumeOnFocusGain) {
                    exoPlayer.setVolume(1f);
                    startPlayer();
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
        exoPlayer.setVolume(volume);
        volume += deltaVolume;

    }

  /*  private class GetByteArrayData extends AsyncTask<Void, Void, Void> {

        ByteArrayOutputStream byteArrayOutputStream;

        @Override
        protected Void doInBackground(Void... voids) {
            URL url = null;
            try {
                url = new URL(songs.get(index).getUrl());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            //Open the stream for the file.
            InputStream inputStream = null;
            try {
                if (url != null) {
                    inputStream = url.openStream();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            //For appending incoming bytes
            byteArrayOutputStream = new ByteArrayOutputStream();
            int read = 0;
            while (read != -1) { //While there is more data
                //Read in bytes to data buffer
                try {
                    if (inputStream != null) {
                        read = inputStream.read();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //Write to output stream
                byteArrayOutputStream.write(read);
            }
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            data = byteArrayOutputStream.toByteArray();
            Toast.makeText(getBaseContext(), "done", Toast.LENGTH_SHORT).show();
            Intent i = new Intent("waveData");
            i.putExtra("waveData", data);
            LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i);
        }
    }  */

    private void createNotification(Context context, MediaSessionCompat mediaSessionCompat, SongModel song, int playbutton, Boolean isLoading, Boolean isBuffering) {

        if (isLoading) {
            new GetMetaData(context, song, playbutton).execute();
            bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.placeholder);
        }

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
                .setContentIntent(pendingIntentManualActivity)
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

    private class GetMetaData extends AsyncTask<Void, Void, Void> {

        Context context;
        SongModel song;
        int playbutton;

        GetMetaData(Context context, SongModel song, int playbutton) {
            this.context = context;
            this.song = song;
            this.playbutton = playbutton;
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

            builder1.setLargeIcon(bitmap);
            notification = builder1.build();
            startForeground(999, notification);
        }

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        saveEqualizerSettings();
        pausePlayer();
        if (exoPlayer != null) {
            exoPlayer.release();
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
        stopSelf();
    }
}
