package com.akshay.karwaan;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.akshay.karwaan.Constants.Constants;
import com.akshay.karwaan.Equalizer.DialogEqualizerFragment;
import com.akshay.karwaan.Equalizer.EqualizerModel;
import com.akshay.karwaan.Equalizer.EqualizerSettings;
import com.akshay.karwaan.Equalizer.Settings;
import com.akshay.karwaan.Models.SongModel;
import com.akshay.karwaan.RoomDB.Word;
import com.akshay.karwaan.RoomDB.WordViewModel;
import com.akshay.karwaan.Services.SaregamaPlaybackService;
import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class SaregamaActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView toolbar_title, tv_saregama_song_details, tv_total_songs, tv_playing_from_playlist;
    private ArrayList<SongModel> songList = new ArrayList<>();
    private ArrayList<SongModel> mainSongList = new ArrayList<>();
    private HashSet<String> artistHashSet = new HashSet<>();
    private DatabaseReference songRef;
    private ImageButton btn_play_pause, btn_next, btn_previous, btn_forward10, btn_backward10;
    private LottieAnimationView lottieAnimationView, lottie_animation_view;
    private ImageView bg;
    private Dialog loading_dialog;
    private ChipGroup chipGroup;
    private Chip chip_my_playlist;
    private RelativeLayout rlParentLayout;
    private Boolean voiceModeEnabled;
    private NotificationManager notificationManager;

    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private String keeper = "";

    private MediaBrowserCompat mediaBrowser;
    private MediaControllerCompat mediaController;
    private int audioSessionID = 0;
    public static final String PREF_KEY = "equalizer";
    private DialogEqualizerFragment fragment;

    private Equalizer mEqualizer;
    private BassBoost bassBoost;
    private PresetReverb presetReverb;

    private WordViewModel mWordViewModel;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saregama);

        toolbar = findViewById(R.id.toolBar);
        toolbar_title = findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);
        toolbar_title.setText(getString(R.string.saregama_toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        rlParentLayout = findViewById(R.id.rlParentLayout);
        btn_play_pause = findViewById(R.id.btn_play_pause);
        btn_play_pause.bringToFront();
        btn_next = findViewById(R.id.btn_next);
        btn_previous = findViewById(R.id.btn_previous);
        btn_forward10 = findViewById(R.id.btn_forward10);
        btn_backward10 = findViewById(R.id.btn_backward10);
        lottieAnimationView = findViewById(R.id.lottie_animation_view);
        bg = findViewById(R.id.bg);
        Glide.with(this).load(R.drawable.bg).into(bg);

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        tv_saregama_song_details = findViewById(R.id.tv_saregama_song_details);
        tv_playing_from_playlist = findViewById(R.id.tv_playing_from_playlist);
        tv_total_songs = findViewById(R.id.tv_total_songs);
        tv_total_songs.setVisibility(View.GONE);
        chipGroup = findViewById(R.id.chipGroup);
        chip_my_playlist = findViewById(R.id.chip_my_playlist);

        loading_dialog = new Dialog(this);
        loading_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loading_dialog.setContentView(R.layout.loading_dialog);
        lottie_animation_view = loading_dialog.findViewById(R.id.lottie_animation_view);
        lottie_animation_view.playAnimation();
        loading_dialog.setCanceledOnTouchOutside(false);
        loading_dialog.setCancelable(false);

        mWordViewModel = ViewModelProviders.of(SaregamaActivity.this).get(WordViewModel.class);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(audioSessionIDBroadcast, new IntentFilter("audioSessionID"));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast, new IntentFilter("loadingDismiss"));

        mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, SaregamaPlaybackService.class), connectionCallbacks, null);

        setReduceSizeAnimation(btn_play_pause);
        setReduceSizeAnimation(btn_next);
        setReduceSizeAnimation(btn_previous);
        setReduceSizeAnimation(btn_forward10);
        setReduceSizeAnimation(btn_backward10);

        voiceModeEnabled = getSharedPreferences("karvaanSharedPref", MODE_PRIVATE).getBoolean("voiceModeEnabled", false);
        if (voiceModeEnabled) {
            btn_play_pause.setVisibility(View.GONE);
            btn_next.setVisibility(View.GONE);
            btn_previous.setVisibility(View.GONE);
            btn_forward10.setVisibility(View.GONE);
            btn_backward10.setVisibility(View.GONE);

            initSpeechRecognition();
        }

        setVisibilityOfPlaylistChip();

        mediaBrowser.connect();
        getSongsList();

        loadEqualizerSettings();
    }

    @Override
    protected void onStart() {
        super.onStart();

        chipGroup.setOnCheckedChangeListener((chipGroup, i) -> {
            loading_dialog.show();
            lottieAnimationView.pauseAnimation();
            ArrayList<SongModel> artistSongsList = new ArrayList<>();
            Chip selectedChip = chipGroup.findViewById(i);
            setReduceSizeAnimation(selectedChip);
            setRegainSizeAnimation(selectedChip);
            if (selectedChip != null) {

                chip_my_playlist.setChipStrokeWidth(0);
                chip_my_playlist.setChipStrokeColorResource(R.color.black);
                chip_my_playlist.setSelected(false);
                tv_playing_from_playlist.setVisibility(View.GONE);

                String selectedArtist = selectedChip.getText().toString();
                Intent chipIntent = new Intent("chipSelect");
                chipIntent.putExtra("chipSelect", selectedArtist);
                LocalBroadcastManager.getInstance(this).sendBroadcast(chipIntent);
                if (selectedArtist.equals("All Artists")) {
                    tv_total_songs.setText(getResources().getString(R.string.total_songs).concat(String.valueOf(mainSongList.size())));
                    songList.clear();
                    songList.addAll(mainSongList);
                } else {
                    songList.clear();
                    for (SongModel song : mainSongList) {
                        for (String artistName : song.getArtists()) {
                            if (artistName.equals(selectedArtist)) {
                                artistSongsList.add(song);
                                break;
                            }
                        }
                    }
                    songList.addAll(artistSongsList);
                    tv_total_songs.setText(getResources().getString(R.string.total_songs).concat(String.valueOf(artistSongsList.size())));
                }
            }
        });

        chip_my_playlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                setReduceSizeAnimation(chip_my_playlist);
                setRegainSizeAnimation(chip_my_playlist);

                chip_my_playlist.setChipStrokeWidth(1f);
                chip_my_playlist.setChipStrokeColorResource(R.color.white);
                chip_my_playlist.setSelected(true);
                tv_playing_from_playlist.setVisibility(View.VISIBLE);

                ArrayList<SongModel> tempSongList = new ArrayList<>();
                mWordViewModel.getAllWords().observe(SaregamaActivity.this, new Observer<List<Word>>() {
                    @Override
                    public void onChanged(List<Word> words) {
                        if (!tempSongList.isEmpty()) {
                            tempSongList.clear();
                        }
                        ArrayList<String> songNameInPlaylist = new ArrayList<>();
                        for (int i = 0; i < words.size(); i++) {
                            songNameInPlaylist.add(words.get(i).getWord());
                        }
                        for (SongModel songModel : mainSongList) {
                            if (songNameInPlaylist.contains(songModel.getSongName())) {
                                tempSongList.add(songModel);
                            }
                        }

                    }
                });
                songList.clear();
                songList.addAll(tempSongList);
                Intent intent = new Intent("playlistSaregama");
                intent.putExtra("playlistSaregama", songList);
                LocalBroadcastManager.getInstance(SaregamaActivity.this).sendBroadcast(intent);
                tv_total_songs.setText(getResources().getString(R.string.total_songs).concat(String.valueOf(songList.size())));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.saregama_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_equilizer:
                showEquilizerDialog();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private final MediaBrowserCompat.ConnectionCallback connectionCallbacks = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            MediaSessionCompat.Token token = mediaBrowser.getSessionToken();
            MediaControllerCompat mediaController = null;
            try {
                mediaController = new MediaControllerCompat(SaregamaActivity.this, token);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            MediaControllerCompat.setMediaController(SaregamaActivity.this, mediaController);
            buildTransportControls();
        }

        @Override
        public void onConnectionSuspended() {
            // The Service has crashed. Disable transport controls until it automatically reconnects
            Toast.makeText(SaregamaActivity.this, "connection suspended", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnectionFailed() {
            Toast.makeText(SaregamaActivity.this, "connection failed", Toast.LENGTH_SHORT).show();
            // The Service has refused our connection
        }
    };

    void buildTransportControls() {
        btn_play_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pbState = MediaControllerCompat.getMediaController(SaregamaActivity.this).getPlaybackState().getState();
                if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                    MediaControllerCompat.getMediaController(SaregamaActivity.this).getTransportControls().pause();
                } else {
                    MediaControllerCompat.getMediaController(SaregamaActivity.this).getTransportControls().play();
                }
            }
        });

        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(SaregamaActivity.this).getTransportControls().skipToNext();
            }
        });

        btn_previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(SaregamaActivity.this).getTransportControls().skipToPrevious();
            }
        });

        btn_forward10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(SaregamaActivity.this).getTransportControls().fastForward();
            }
        });

        btn_backward10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(SaregamaActivity.this).getTransportControls().rewind();
            }
        });
        mediaController = MediaControllerCompat.getMediaController(SaregamaActivity.this);
        mediaController.registerCallback(controllerCallback);
    }

    MediaControllerCompat.Callback controllerCallback =
            new MediaControllerCompat.Callback() {
                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    String title = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
                    String artist = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
                    String movie = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
                    tv_saregama_song_details.setText(title.concat(" (").concat(movie).concat(")").concat(" - ").concat(artist));
                    tv_saregama_song_details.setSelected(true);
                }

                @Override
                public void onPlaybackStateChanged(PlaybackStateCompat state) {
                    switch (state.getState()) {
                        case PlaybackStateCompat.STATE_PLAYING:
                            loading_dialog.dismiss();
                            lottieAnimationView.playAnimation();
                            btn_play_pause.setImageResource(R.drawable.pause_button);
                            alphaAnimation(lottieAnimationView, 0, 1f);
                            lottieAnimationView.playAnimation();
                            setRegainSizeAnimation(btn_play_pause);
                            setRegainSizeAnimation(btn_next);
                            setRegainSizeAnimation(btn_previous);
                            setRegainSizeAnimation(btn_forward10);
                            setRegainSizeAnimation(btn_backward10);
                            translateAnimation(btn_next, btn_play_pause.getX() - btn_next.getX(), 0, 0, 0);
                            translateAnimation(btn_previous, btn_play_pause.getX() - btn_previous.getX(), 0, 0, 0);
                            translateAnimation(btn_forward10, btn_play_pause.getX() - btn_forward10.getX(), 0, 0, 0);
                            translateAnimation(btn_backward10, btn_play_pause.getX() - btn_backward10.getX(), 0, 0, 0);
                            alphaAnimation(btn_next, 0, 1f);
                            alphaAnimation(btn_previous, 0, 1f);
                            alphaAnimation(btn_forward10, 0, 1f);
                            alphaAnimation(btn_backward10, 0, 1f);
                            alphaAnimation(chipGroup, 0, 1f);
                            chipGroup.setVisibility(View.VISIBLE);
                            break;

                        case PlaybackStateCompat.STATE_PAUSED:
                            lottieAnimationView.pauseAnimation();
                            btn_play_pause.setImageResource(R.drawable.play_button);
                            alphaAnimation(lottieAnimationView, 1f, 0);
                            lottieAnimationView.pauseAnimation();
                            setReduceSizeAnimation(btn_next);
                            setReduceSizeAnimation(btn_previous);
                            setReduceSizeAnimation(btn_forward10);
                            setReduceSizeAnimation(btn_backward10);
                            translateAnimation(btn_next, 0, btn_play_pause.getX() - btn_next.getX(), 0, 0);
                            translateAnimation(btn_previous, 0, btn_play_pause.getX() - btn_previous.getX(), 0, 0);
                            translateAnimation(btn_forward10, 0, btn_play_pause.getX() - btn_forward10.getX(), 0, 0);
                            translateAnimation(btn_backward10, 0, btn_play_pause.getX() - btn_backward10.getX(), 0, 0);
                            alphaAnimation(chipGroup, 1f, 0);
                            chipGroup.setVisibility(View.GONE);
                            break;

                        case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
                            loading_dialog.show();
                            lottieAnimationView.pauseAnimation();
                            break;

                        case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
                            loading_dialog.show();
                            lottieAnimationView.pauseAnimation();
                            break;

                        case PlaybackStateCompat.STATE_FAST_FORWARDING:
                            loading_dialog.show();
                            lottieAnimationView.pauseAnimation();
                            break;

                        case PlaybackStateCompat.STATE_REWINDING:
                            loading_dialog.show();
                            lottieAnimationView.pauseAnimation();
                            break;

                        case PlaybackStateCompat.STATE_BUFFERING:
                            loading_dialog.show();
                            lottieAnimationView.pauseAnimation();
                            break;
                    }
                }
            };

    private void initSpeechRecognition() {
        rlParentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speechRecognizer.startListening(speechRecognizerIntent);
                keeper = "";
            }
        });

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(SaregamaActivity.this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        speechRecognizerIntent.putExtra("android.speech.extra.DICTATION_MODE", true);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                Toast.makeText(SaregamaActivity.this, "Give a command", Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        speechRecognizer.stopListening();
                    }
                }, 1500);
            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {
                Toast.makeText(SaregamaActivity.this, "Buffering", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int i) {
                Toast.makeText(SaregamaActivity.this, "Some error occured!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                if (!results.isEmpty()) {
                    keeper = results.get(0);
                }

                switch (keeper) {
                    case "next":
                        MediaControllerCompat.getMediaController(SaregamaActivity.this).getTransportControls().skipToNext();
                        Toast.makeText(SaregamaActivity.this, "Command: " + keeper, Toast.LENGTH_SHORT).show();
                        break;

                    case "previous":
                        MediaControllerCompat.getMediaController(SaregamaActivity.this).getTransportControls().skipToPrevious();
                        Toast.makeText(SaregamaActivity.this, "Command: " + keeper, Toast.LENGTH_SHORT).show();
                        break;

                    case "play":
                        MediaControllerCompat.getMediaController(SaregamaActivity.this).getTransportControls().play();
                        Toast.makeText(SaregamaActivity.this, "Command: " + keeper, Toast.LENGTH_SHORT).show();
                        break;

                    case "pause":
                        MediaControllerCompat.getMediaController(SaregamaActivity.this).getTransportControls().pause();
                        Toast.makeText(SaregamaActivity.this, "Command: " + keeper, Toast.LENGTH_SHORT).show();
                        break;

                    case "forward":
                        MediaControllerCompat.getMediaController(SaregamaActivity.this).getTransportControls().fastForward();
                        Toast.makeText(SaregamaActivity.this, "Command: " + keeper, Toast.LENGTH_SHORT).show();
                        break;

                    case "backward":
                        MediaControllerCompat.getMediaController(SaregamaActivity.this).getTransportControls().rewind();
                        Toast.makeText(SaregamaActivity.this, "Command: " + keeper, Toast.LENGTH_SHORT).show();
                        break;

                    default:
                        Toast.makeText(SaregamaActivity.this, "Doesn't know what to do with this command: " + keeper, Toast.LENGTH_LONG).show();
                        break;
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });
    }

    private void getSongsList() {
        loading_dialog.show();
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
                    String movie = ds.child("movie").getValue(String.class);
                    ArrayList<String> artistsList = new ArrayList<>();
                    for (DataSnapshot ds1 : ds.child("artists").getChildren()) {
                        artistsList.add(ds1.getValue(String.class));
                    }
                    mainSongList.add(new SongModel(url, songName, movie, artistsList));
                }
                if (!mainSongList.isEmpty()) {
                    tv_total_songs.setText(getResources().getString(R.string.total_songs).concat(String.valueOf(mainSongList.size())));
                    tv_total_songs.setVisibility(View.VISIBLE);
                    getArtists();
                    songList.clear();
                    songList.addAll(mainSongList);
                    Intent i = new Intent("mainSongList");
                    i.putExtra("mainSongList", mainSongList);
                    LocalBroadcastManager.getInstance(SaregamaActivity.this).sendBroadcast(i);
                } else {
                    Toast.makeText(SaregamaActivity.this, "No Songs Found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(SaregamaActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                loading_dialog.dismiss();
            }
        });
    }

    private void getArtists() {
        generateChip("All Artists");

        if (!artistHashSet.isEmpty()) {
            artistHashSet.clear();
        }

        for (SongModel song : mainSongList) {
            artistHashSet.addAll(song.getArtists());
        }

        for (String artistName : artistHashSet) {
            generateChip(artistName);
        }

    }

    private void setVisibilityOfPlaylistChip() {
        mWordViewModel.getAllWords().observe(SaregamaActivity.this, new Observer<List<Word>>() {
            @Override
            public void onChanged(List<Word> words) {
                if (words.isEmpty()) {
                    chip_my_playlist.setVisibility(View.GONE);
                } else {
                    chip_my_playlist.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void generateChip(String title) {
        Chip chip = new Chip(chipGroup.getContext());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(5, 5, 5, 5);
        chip.setLayoutParams(layoutParams);
        chip.setId(View.generateViewId());
        chip.setText(title);
        //chip.setTextAppearance(R.style.chipTextAppearance);
        chip.setCloseIconEnabled(false);
        chip.setClickable(true);
        chip.setCheckable(true);
        if (title.equals("All Artists")) {
            chip.setChecked(true);
            chip.setTextAppearance(R.style.chipTextAppearanceBold);
        }
        chipGroup.addView(chip);
    }

    private BroadcastReceiver audioSessionIDBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("audioSessionID")) {
                audioSessionID = intent.getIntExtra("audioSessionID", 0);

                mEqualizer = new Equalizer(0, audioSessionID);
                bassBoost = new BassBoost(0, audioSessionID);
                presetReverb = new PresetReverb(0, audioSessionID);

                fragment = DialogEqualizerFragment.newBuilder()
                        .setAudioSessionId(audioSessionID)
                        .title(R.string.app_name)
                        .themeColor(ContextCompat.getColor(SaregamaActivity.this, R.color.primaryColor))
                        .textColor(ContextCompat.getColor(SaregamaActivity.this, R.color.textColor))
                        .accentAlpha(ContextCompat.getColor(SaregamaActivity.this, R.color.playingCardColor))
                        .darkColor(ContextCompat.getColor(SaregamaActivity.this, R.color.primaryDarkColor))
                        .setAccentColor(ContextCompat.getColor(SaregamaActivity.this, R.color.secondaryColor))
                        .build();
            }
        }
    };

    private BroadcastReceiver broadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("loadingDismiss")) {
                loading_dialog.dismiss();
                lottieAnimationView.playAnimation();
            }
        }
    };

    private void showEquilizerDialog() {
        //if (fragment != null) {
        fragment.show(getSupportFragmentManager(), "eq");
        //} else {
        // Toast.makeText(this, "Start a song first", Toast.LENGTH_SHORT).show();
        //}
    }

    private void loadEqualizerSettings() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        Gson gson = new Gson();
        EqualizerSettings settings = gson.fromJson(preferences.getString(PREF_KEY, "{}"), EqualizerSettings.class);
        EqualizerModel model = new EqualizerModel();
        model.setBassStrength(settings.bassStrength);
        model.setPresetPos(settings.presetPos);
        model.setReverbPreset(settings.reverbPreset);
        model.setSeekbarpos(settings.seekbarpos);
        model.setEqualizerEnabled(settings.isEqualizerEnabled);

        Settings.isEqualizerEnabled = settings.isEqualizerEnabled;
        Settings.isEqualizerReloaded = true;
        Settings.bassStrength = settings.bassStrength;
        Settings.presetPos = settings.presetPos;
        Settings.reverbPreset = settings.reverbPreset;
        Settings.seekbarpos = settings.seekbarpos;
        Settings.equalizerModel = model;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(Constants.CHANNEL_ID, "Karvaan Music Notifications", NotificationManager.IMPORTANCE_LOW);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
    }

    private void translateAnimation(View view, float fromX, float toX, float fromY, float toY) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, "translationX", fromX, toX);
        objectAnimator.setDuration(800);
        objectAnimator.setInterpolator(new DecelerateInterpolator());
        objectAnimator.start();
    }

    private void alphaAnimation(View view, float from, float to) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, "alpha", from, to);
        objectAnimator.setDuration(1200);
        objectAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        objectAnimator.start();
    }

    private void setReduceSizeAnimation(View viewToAnimate) {
        AnimatorSet reducer = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.reduce_size);
        reducer.setTarget(viewToAnimate);
        reducer.start();
    }

    private void setRegainSizeAnimation(View viewToAnimate) {
        AnimatorSet regainer = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.regain_size);
        regainer.setTarget(viewToAnimate);
        regainer.start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        lottieAnimationView.pauseAnimation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        lottieAnimationView.pauseAnimation();
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
        }
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
        if (mEqualizer != null) {
            mEqualizer.release();
        }

        if (bassBoost != null) {
            bassBoost.release();
        }

        if (presetReverb != null) {
            presetReverb.release();
        }
        if (MediaControllerCompat.getMediaController(SaregamaActivity.this) != null) {
            MediaControllerCompat.getMediaController(SaregamaActivity.this).unregisterCallback(controllerCallback);
        }
        mediaBrowser.disconnect();
    }
}
