package com.example.karwaan;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar;
import com.bumptech.glide.Glide;
import com.example.karwaan.Adapters.RVPlaylistAdapter;
import com.example.karwaan.Adapters.RVSongsAdapter;
import com.example.karwaan.Constants.Constants;
import com.example.karwaan.Equalizer.DialogEqualizerFragment;
import com.example.karwaan.Equalizer.EqualizerModel;
import com.example.karwaan.Equalizer.EqualizerSettings;
import com.example.karwaan.Equalizer.Settings;
import com.example.karwaan.Models.SongModel;
import com.example.karwaan.RVSwipeHelpers.RVPlaylistSwipeHelper;
import com.example.karwaan.RVSwipeHelpers.RVSongSwipeHelper;
import com.example.karwaan.RoomDB.Word;
import com.example.karwaan.RoomDB.WordViewModel;
import com.example.karwaan.Services.ManualPlaybackService;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ManualActivity extends AppCompatActivity implements RVSongSwipeHelper.RecyclerItemTouchHelperListener, RVPlaylistSwipeHelper.RecyclerItemTouchHelperListener {

    private Toolbar toolbar;
    private TextView toolbar_title, tv_sliding_view_song_name, tv_current_time, tv_total_time, tv_total_songs, tv_add_by_right_swipe;
    private RecyclerView rv_songs, rv_songs_playlist;
    private DatabaseReference songsRef;
    private ArrayList<SongModel> songs = new ArrayList<>();
    private ArrayList<SongModel> mainSongsList = new ArrayList<>();
    private HashSet<String> artistHashSet = new HashSet<>();
    private SlidingUpPanelLayout slidingUpPanelLayout;
    private ImageButton btn_play_pause, btn_next_song, btn_prev_song, btn_forward_10, btn_backward_10;
    private Dialog loading_dialog;
    private ImageView bg;
    private ChipGroup chipGroup;
    private EditText et_search;
    private LottieAnimationView lottie_animation_view;
    private Chip chip_my_playlist;

    private NotificationManager notificationManager;
    private MediaBrowserCompat mediaBrowser;
    private MediaControllerCompat mediaController;

    private RoundCornerProgressBar seekbar;
    private SeekBar seekBarInvisible;
    private List<Integer> allColors;
    private ItemTouchHelper itemTouchHelper, itemTouchHelper1;
    private WordViewModel mWordViewModel;
    private RVPlaylistAdapter playlistAdapter;

    private int audioSessionID = 0;
    public static final String PREF_KEY = "equalizer";
    private DialogEqualizerFragment fragment;

    private Equalizer mEqualizer;
    private BassBoost bassBoost;
    private PresetReverb presetReverb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);

        getSharedPreferences("released", MODE_PRIVATE).edit().putBoolean("released", false).commit();

        toolbar = (Toolbar) findViewById(R.id.toolBar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);
        toolbar_title.setText(getString(R.string.manual_toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loading_dialog = new Dialog(this);
        loading_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loading_dialog.setContentView(R.layout.loading_dialog);
        lottie_animation_view = loading_dialog.findViewById(R.id.lottie_animation_view);
        lottie_animation_view.playAnimation();
        loading_dialog.setCanceledOnTouchOutside(false);
        loading_dialog.setCancelable(false);

        bg = findViewById(R.id.bg);
        Glide.with(this).load(R.drawable.bg).into(bg);

        et_search = findViewById(R.id.et_search);
        slidingUpPanelLayout = findViewById(R.id.sliding_layout);
        slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        btn_play_pause = findViewById(R.id.btn_play_pause);
        btn_next_song = findViewById(R.id.btn_next_song);
        btn_prev_song = findViewById(R.id.btn_prev_song);
        btn_forward_10 = findViewById(R.id.btn_forward_10);
        btn_backward_10 = findViewById(R.id.btn_backward_10);
        tv_current_time = findViewById(R.id.tv_current_time);
        tv_total_time = findViewById(R.id.tv_total_time);
        tv_add_by_right_swipe = findViewById(R.id.tv_add_by_right_swipe);
        tv_total_songs = findViewById(R.id.tv_total_songs);
        tv_total_songs.setVisibility(View.GONE);
        tv_sliding_view_song_name = findViewById(R.id.tv_sliding_view_song_name);
        tv_sliding_view_song_name.setText("Select a song to play");
        chipGroup = findViewById(R.id.chipGroup);
        chip_my_playlist = findViewById(R.id.chip_my_playlist);
        rv_songs = findViewById(R.id.rv_songs);
        rv_songs.setHasFixedSize(true);
        rv_songs.setLayoutManager(new LinearLayoutManager(this));

        rv_songs_playlist = findViewById(R.id.rv_songs_playlist);
        rv_songs_playlist.setHasFixedSize(true);
        rv_songs_playlist.setLayoutManager(new LinearLayoutManager(this));
        rv_songs_playlist.setVisibility(View.GONE);

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RVSongSwipeHelper(0, ItemTouchHelper.RIGHT, this);
        itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
        itemTouchHelper.attachToRecyclerView(rv_songs);

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback1 = new RVPlaylistSwipeHelper(0, ItemTouchHelper.RIGHT, this);
        itemTouchHelper1 = new ItemTouchHelper(itemTouchHelperCallback1);
        itemTouchHelper1.attachToRecyclerView(rv_songs_playlist);

        mWordViewModel = ViewModelProviders.of(ManualActivity.this).get(WordViewModel.class);

        // wave = findViewById(R.id.wave);
        seekbar = findViewById(R.id.seekBar);
        seekBarInvisible = findViewById(R.id.seekBarInvisible);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast, new IntentFilter("loadingDismiss"));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast1, new IntentFilter("updateCurrentTime"));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast2, new IntentFilter("updateSeekbar"));
        LocalBroadcastManager.getInstance(this).registerReceiver(audioSessionIDBroadcast, new IntentFilter("audioSessionIDManual"));

        mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, ManualPlaybackService.class), connectionCallbacks, null);
        mediaBrowser.connect();

        getSongs();

        try {
            allColors = getAllMaterialColors();
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }

        getSharedPreferences("firstTimeCreated", MODE_PRIVATE).edit().putBoolean("firstTimeCreated", true).commit();

        updateSeekbarColor();

        loadEqualizerSettings();
    }

    @Override
    protected void onStart() {
        super.onStart();

        chipGroup.setOnCheckedChangeListener((chipGroup, i) -> {
            loading_dialog.show();
            Chip selectedChip = chipGroup.findViewById(i);
            setReduceSizeAnimation(selectedChip);
            setRegainSizeAnimation(selectedChip);
            if (selectedChip != null) {

                et_search.setEnabled(true);
                chip_my_playlist.setChipStrokeWidth(0);
                chip_my_playlist.setChipStrokeColorResource(R.color.black);
                chip_my_playlist.setSelected(false);
                tv_add_by_right_swipe.setVisibility(View.GONE);

                String selectedArtist = selectedChip.getText().toString();
                Intent intent = new Intent("chip");
                intent.putExtra("chip", selectedArtist);
                LocalBroadcastManager.getInstance(ManualActivity.this).sendBroadcast(intent);
                if (selectedArtist.equals("All Artists")) {
                    songs.clear();
                    songs.addAll(mainSongsList);
                    rv_songs.setAdapter(new RVSongsAdapter(songs, ManualActivity.this));
                    rv_songs_playlist.setVisibility(View.GONE);
                    rv_songs_playlist.setAdapter(null);
                    rv_songs.setVisibility(View.VISIBLE);
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
                    rv_songs.setAdapter(new RVSongsAdapter(songs, ManualActivity.this));
                    rv_songs_playlist.setVisibility(View.GONE);
                    rv_songs_playlist.setAdapter(null);
                    rv_songs.setVisibility(View.VISIBLE);
                }
            }
            loading_dialog.dismiss();
        });

        chip_my_playlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                setReduceSizeAnimation(chip_my_playlist);
                setRegainSizeAnimation(chip_my_playlist);

                et_search.setEnabled(false);
                chip_my_playlist.setChipStrokeWidth(1f);
                chip_my_playlist.setChipStrokeColorResource(R.color.white);
                chip_my_playlist.setSelected(true);

                ArrayList<SongModel> tempSongList = new ArrayList<>();
                mWordViewModel.getAllWords().observe(ManualActivity.this, new Observer<List<Word>>() {
                    @Override
                    public void onChanged(List<Word> words) {
                        if (!tempSongList.isEmpty()) {
                            tempSongList.clear();
                        }
                        ArrayList<String> songNameInPlaylist = new ArrayList<>();
                        if (!words.isEmpty()) {
                            for (int i = 0; i < words.size(); i++) {
                                songNameInPlaylist.add(words.get(i).getWord());
                            }
                            for (SongModel songModel : mainSongsList) {
                                if (songNameInPlaylist.contains(songModel.getSongName())) {
                                    tempSongList.add(songModel);
                                }
                            }
                            tv_add_by_right_swipe.setVisibility(View.GONE);
                        } else {
                            tv_add_by_right_swipe.setVisibility(View.VISIBLE);
                            Toast.makeText(ManualActivity.this, "No songs in your playlist", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                songs.clear();
                songs.addAll(tempSongList);
                Intent intent = new Intent("playlist");
                intent.putExtra("playlist", songs);
                LocalBroadcastManager.getInstance(ManualActivity.this).sendBroadcast(intent);
                playlistAdapter = new RVPlaylistAdapter(songs, ManualActivity.this);
                rv_songs_playlist.setAdapter(playlistAdapter);
                rv_songs_playlist.setVisibility(View.VISIBLE);
                rv_songs.setVisibility(View.GONE);
                rv_songs.setAdapter(null);
            }
        });

        et_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!et_search.isEnabled()) {
                    Toast.makeText(ManualActivity.this, "Search can't be done in the playlist", Toast.LENGTH_SHORT).show();
                }
            }
        });

        et_search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                search(charSequence);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        seekBarInvisible.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b) {
                    Intent intent = new Intent("seekChanged");
                    intent.putExtra("seekTo", i);
                    LocalBroadcastManager.getInstance(ManualActivity.this).sendBroadcast(intent);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void getSongs() {
        loading_dialog.show();

        if (!mainSongsList.isEmpty()) {
            mainSongsList.clear();
        }
        songsRef = FirebaseDatabase.getInstance().getReference("Songs");
        songsRef.addListenerForSingleValueEvent(new ValueEventListener() {
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
                    mainSongsList.add(new SongModel(url, songName, movie, artistsList));
                }
                if (!mainSongsList.isEmpty()) {
                    tv_total_songs.setText(getResources().getString(R.string.total_songs).concat(String.valueOf(mainSongsList.size())));
                    tv_total_songs.setVisibility(View.VISIBLE);
                    songs.clear();
                    songs.addAll(mainSongsList);
                    getArtists();
                    Intent i = new Intent("mainSongList");
                    i.putExtra("mainSongList", songs);
                    LocalBroadcastManager.getInstance(ManualActivity.this).sendBroadcast(i);
                    rv_songs.setAdapter(new RVSongsAdapter(songs, ManualActivity.this));
                } else {
                    Toast.makeText(ManualActivity.this, "No songs found", Toast.LENGTH_SHORT).show();
                }
                loading_dialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ManualActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                loading_dialog.dismiss();
            }
        });
    }

    private void search(CharSequence text) {
        songs.clear();
        String query = text.toString().toLowerCase();
        for (SongModel song : mainSongsList) {
            if (song.getSongName().toLowerCase().contains(query)) {
                songs.add(song);
            } else if (song.getMovie().toLowerCase().contains(query)) {
                songs.add(song);
            }
        }
        Intent i = new Intent("searchList");
        i.putExtra("search", query);
        LocalBroadcastManager.getInstance(ManualActivity.this).sendBroadcast(i);
        rv_songs.setAdapter(new RVSongsAdapter(songs, ManualActivity.this));
    }

    private void getArtists() {
        generateChip("All Artists");

        if (!artistHashSet.isEmpty()) {
            artistHashSet.clear();
        }

        for (SongModel song : mainSongsList) {
            artistHashSet.addAll(song.getArtists());
        }

        for (String artistName : artistHashSet) {
            generateChip(artistName);
        }

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

    private final MediaBrowserCompat.ConnectionCallback connectionCallbacks =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    MediaSessionCompat.Token token = mediaBrowser.getSessionToken();
                    MediaControllerCompat mediaController = null;
                    try {
                        mediaController = new MediaControllerCompat(ManualActivity.this, token);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    MediaControllerCompat.setMediaController(ManualActivity.this, mediaController);
                    buildTransportControls();
                }

                @Override
                public void onConnectionSuspended() {
                    // The Service has crashed. Disable transport controls until it automatically reconnects
                    Toast.makeText(ManualActivity.this, "connection suspended", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onConnectionFailed() {
                    Toast.makeText(ManualActivity.this, "connection failed", Toast.LENGTH_SHORT).show();
                    // The Service has refused our connection
                }
            };

    void buildTransportControls() {
        btn_play_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setReduceSizeAnimation(btn_play_pause);
                setRegainSizeAnimation(btn_play_pause);
                int pbState = MediaControllerCompat.getMediaController(ManualActivity.this).getPlaybackState().getState();
                if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                    MediaControllerCompat.getMediaController(ManualActivity.this).getTransportControls().pause();
                } else {
                    MediaControllerCompat.getMediaController(ManualActivity.this).getTransportControls().play();
                }
            }
        });

        btn_next_song.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(ManualActivity.this).getTransportControls().skipToNext();
            }
        });

        btn_prev_song.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(ManualActivity.this).getTransportControls().skipToPrevious();
            }
        });

        btn_forward_10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(ManualActivity.this).getTransportControls().fastForward();
            }
        });

        btn_backward_10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(ManualActivity.this).getTransportControls().rewind();
            }
        });
        mediaController = MediaControllerCompat.getMediaController(ManualActivity.this);
        mediaController.registerCallback(controllerCallback);
    }

    MediaControllerCompat.Callback controllerCallback =
            new MediaControllerCompat.Callback() {
                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    String title = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
                    String artist = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
                    String movie = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
                    long duration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
                    tv_sliding_view_song_name.setText(title.concat(" (").concat(movie).concat(")").concat(" - ").concat(artist));
                    tv_sliding_view_song_name.setSelected(true);
                    tv_total_time.setText(milliSecondsToTimer(duration));
                    seekbar.setMax((float) duration);
                    seekBarInvisible.setMax((int) duration);
                }

                @Override
                public void onPlaybackStateChanged(PlaybackStateCompat state) {
                    switch (state.getState()) {
                        case PlaybackStateCompat.STATE_PLAYING:
                            loading_dialog.dismiss();
                            btn_play_pause.setImageResource(R.drawable.pause_btn_black);
                            btn_next_song.setEnabled(true);
                            btn_prev_song.setEnabled(true);
                            btn_forward_10.setEnabled(true);
                            btn_backward_10.setEnabled(true);
                            alphaAnimation(btn_next_song, 0, 1f);
                            alphaAnimation(btn_prev_song, 0, 1f);
                            alphaAnimation(btn_forward_10, 0, 1f);
                            alphaAnimation(btn_backward_10, 0, 1f);

                            if (!btn_next_song.isEnabled()) {
                                btn_next_song.setEnabled(true);
                                alphaAnimation(btn_next_song, 0, 1f);
                            }
                            if (!btn_prev_song.isEnabled()) {
                                btn_prev_song.setEnabled(true);
                                alphaAnimation(btn_prev_song, 0, 1f);
                            }
                            if (slidingUpPanelLayout.getPanelState() != SlidingUpPanelLayout.PanelState.EXPANDED || slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED) {
                                slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                                if (getSharedPreferences("firstTimeCreated", MODE_PRIVATE).getBoolean("firstTimeCreated", false)) {
                                    setMargins(rv_songs, 0, 0, 0, 150);
                                    getSharedPreferences("firstTimeCreated", MODE_PRIVATE).edit().putBoolean("firstTimeCreated", false).commit();
                                }
                                setMargins(rv_songs_playlist, 0, 0, 0, 150);
                            }
                            break;

                        case PlaybackStateCompat.STATE_PAUSED:
                            btn_play_pause.setImageResource(R.drawable.play_btn_black);
                            btn_next_song.setEnabled(false);
                            btn_prev_song.setEnabled(false);
                            btn_forward_10.setEnabled(false);
                            btn_backward_10.setEnabled(false);
                            alphaAnimation(btn_next_song, 1f, 0);
                            alphaAnimation(btn_prev_song, 1f, 0);
                            alphaAnimation(btn_forward_10, 1f, 0);
                            alphaAnimation(btn_backward_10, 1f, 0);
                            break;

                        case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
                            loading_dialog.show();
                            break;

                        case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
                            loading_dialog.show();
                            break;

                        case PlaybackStateCompat.STATE_FAST_FORWARDING:
                            loading_dialog.show();
                            break;

                        case PlaybackStateCompat.STATE_REWINDING:
                            loading_dialog.show();
                            break;

                        case PlaybackStateCompat.STATE_STOPPED:
                            btn_play_pause.setImageResource(R.drawable.play_btn_black);
                            break;

                        case PlaybackStateCompat.STATE_BUFFERING:
                            loading_dialog.show();
                            break;
                    }
                }
            };

    private BroadcastReceiver broadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("loadingDismiss")) {
                loading_dialog.dismiss();
            }
        }
    };

    private BroadcastReceiver broadcast1 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("updateCurrentTime")) {
                tv_current_time.setText(milliSecondsToTimer(intent.getLongExtra("updateCurrentTime", 0)));
                seekBarInvisible.setProgress((int) intent.getLongExtra("updateCurrentTime", 0));
            }
        }
    };

    private BroadcastReceiver broadcast2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("updateSeekbar")) {
                seekbar.setSecondaryProgress((float) intent.getLongExtra("bufferedPosition", 0));
                seekbar.setProgress((float) intent.getLongExtra("currentPosition", 0));
            }
        }
    };

    private BroadcastReceiver audioSessionIDBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("audioSessionIDManual")) {
                audioSessionID = intent.getIntExtra("audioSessionIDManual", 0);

                mEqualizer = new Equalizer(0, audioSessionID);
                bassBoost = new BassBoost(0, audioSessionID);
                presetReverb = new PresetReverb(0, audioSessionID);

                fragment = DialogEqualizerFragment.newBuilder()
                        .setAudioSessionId(audioSessionID)
                        .title(R.string.app_name)
                        .themeColor(ContextCompat.getColor(ManualActivity.this, R.color.primaryColor))
                        .textColor(ContextCompat.getColor(ManualActivity.this, R.color.textColor))
                        .accentAlpha(ContextCompat.getColor(ManualActivity.this, R.color.playingCardColor))
                        .darkColor(ContextCompat.getColor(ManualActivity.this, R.color.primaryDarkColor))
                        .setAccentColor(ContextCompat.getColor(ManualActivity.this, R.color.secondaryColor))
                        .build();
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.manual_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_equilizer_manual:
                showEquilizerDialog();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private void showEquilizerDialog() {
        if (fragment != null) {
            fragment.show(getSupportFragmentManager(), "eq");
        } else {
            Toast.makeText(this, "Start a song first", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        if (viewHolder instanceof RVSongsAdapter.ViewHolder) {
            loading_dialog.show();
            SongModel song = songs.get(position);
            mWordViewModel.insert(new Word(song.getSongName()));
            Toast.makeText(this, "Added " + song.getSongName(), Toast.LENGTH_SHORT).show();
            loading_dialog.dismiss();

            itemTouchHelper.attachToRecyclerView(null);
            itemTouchHelper.attachToRecyclerView(rv_songs);
        }

        if (viewHolder instanceof RVPlaylistAdapter.ViewHolder) {
            loading_dialog.show();

            SongModel songModel = songs.get(position);
            mWordViewModel.delete(new Word(songModel.getSongName()));
            playlistAdapter.removeItem(position);
            Toast.makeText(this, "Removed " + songModel.getSongName(), Toast.LENGTH_SHORT).show();

            loading_dialog.dismiss();

            itemTouchHelper1.attachToRecyclerView(null);
            itemTouchHelper1.attachToRecyclerView(rv_songs_playlist);
        }
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

    private void updateSeekbarColor() {
        int randomIndex = new Random().nextInt(allColors.size());
        int randomColor = allColors.get(randomIndex);
        seekbar.setProgressColor(randomColor);
        seekbar.setSecondaryProgressColor(manipulateColor(randomColor, 0.5f));
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                updateSeekbarColor();
            }
        }, 1800);
    }

    private List<Integer> getAllMaterialColors() throws IOException, XmlPullParserException {
        XmlResourceParser xrp = getResources().getXml(R.xml.materialcolor);
        List<Integer> allColors = new ArrayList<>();
        int nextEvent;
        while ((nextEvent = xrp.next()) != XmlResourceParser.END_DOCUMENT) {
            String s = xrp.getName();
            if ("color".equals(s)) {
                String color = xrp.nextText();
                allColors.add(Color.parseColor(color));
            }
        }
        return allColors;
    }

    public int manipulateColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(a, Math.min(r, 255), Math.min(g, 255), Math.min(b, 255));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(Constants.CHANNEL_ID, "Karvaan Music Notifications", NotificationManager.IMPORTANCE_LOW);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
    }

    private String milliSecondsToTimer(long milliseconds) {
        String finalTimerString = "";
        String secondsString = "";

        // Convert total duration into time
        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);
        // Add hours if there
        if (hours > 0) {
            finalTimerString = hours + ":";
        }

        // Prepending 0 to seconds if it is one digit
        if (seconds < 10) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        // return timer string
        return finalTimerString;
    }

    private void setMargins(View view, int left, int top, int right, int bottom) {
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            p.setMargins(left, top, right, bottom);
            view.requestLayout();
        }
    }

    private void alphaAnimation(View view, float from, float to) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, "alpha", from, to);
        objectAnimator.setDuration(800);
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
    public void onBackPressed() {
        if (slidingUpPanelLayout != null && (slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED || slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED)) {
            slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            super.onBackPressed();
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public static void trimCache(Context context) {
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getSharedPreferences("released", MODE_PRIVATE).edit().putBoolean("released", true).commit();
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
        rv_songs.setAdapter(null);
        rv_songs_playlist.setAdapter(null);
        try {
            trimCache(this);
            // Toast.makeText(this,"onDestroy " ,Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (MediaControllerCompat.getMediaController(ManualActivity.this) != null) {
            MediaControllerCompat.getMediaController(ManualActivity.this).unregisterCallback(controllerCallback);
        }
        mediaBrowser.disconnect();
    }
}
