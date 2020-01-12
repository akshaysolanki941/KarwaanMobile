package com.example.karwaan;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.view.MotionEvent;
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

import com.bumptech.glide.Glide;
import com.example.karwaan.Adapters.RVSongsAdapter;
import com.example.karwaan.Models.SongModel;
import com.example.karwaan.Notification.CreateNotification;
import com.example.karwaan.Services.OnClearFromRecentService;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.HashSet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ManualActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView toolbar_title, tv_sliding_view_song_name, tv_current_time, tv_total_time, tv_total_songs;
    private RecyclerView rv_songs;
    private DatabaseReference songsRef;
    private ArrayList<SongModel> songs = new ArrayList<>();
    private ArrayList<SongModel> mainSongsList = new ArrayList<>();
    private HashSet<String> artistHashSet = new HashSet<>();
    private SlidingUpPanelLayout slidingUpPanelLayout;
    private ImageButton btn_play_pause, btn_next_song, btn_prev_song;
    private SeekBar seekBar;
    private Dialog loading_dialog;
    private ImageView loading_gif_imageView, bg;
    private ChipGroup chipGroup;
    private EditText et_search;

    private ExoPlayer exoPlayer;
    private BandwidthMeter bandwidthMeter;
    private ExtractorsFactory extractorsFactory;
    private TrackSelection.Factory trackSelectionfactory;
    private DataSource.Factory dataSourceFactory;

    private int mediaFileLengthInMilliseconds;
    private final Handler handler = new Handler();
    private int index;
    private MediaSessionCompat mediaSessionCompat;

    private NotificationManager notificationManager;

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
        loading_gif_imageView = (ImageView) loading_dialog.findViewById(R.id.loading_gif_imageView);
        Glide.with(getApplicationContext()).load(R.drawable.loading).placeholder(R.drawable.loading).into(loading_gif_imageView);
        loading_dialog.setCanceledOnTouchOutside(false);
        loading_dialog.setCancelable(false);

        bg = findViewById(R.id.bg);
        et_search = findViewById(R.id.et_search);
        slidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        btn_play_pause = (ImageButton) findViewById(R.id.btn_play_pause);
        btn_next_song = findViewById(R.id.btn_next_song);
        btn_prev_song = findViewById(R.id.btn_prev_song);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        tv_current_time = findViewById(R.id.tv_current_time);
        tv_total_time = findViewById(R.id.tv_total_time);
        tv_total_songs = findViewById(R.id.tv_total_songs);
        tv_total_songs.setVisibility(View.GONE);
        tv_sliding_view_song_name = findViewById(R.id.tv_sliding_view_song_name);
        tv_sliding_view_song_name.setText("Select a song to play");
        chipGroup = findViewById(R.id.chipGroup);
        rv_songs = (RecyclerView) findViewById(R.id.rv_songs);
        rv_songs.setHasFixedSize(true);
        rv_songs.setLayoutManager(new LinearLayoutManager(this));

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        registerReceiver(broadcastReceiver, new IntentFilter("SONGS"));
        startService(new Intent(getBaseContext(), OnClearFromRecentService.class));

        mediaSessionCompat = new MediaSessionCompat(this, "tag");

        initExoPlayer();
        getSongs();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Glide.with(this).load(R.drawable.bg).into(bg);

        btn_play_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playPauseSong();
            }
        });

        btn_next_song.setOnClickListener(v -> {
            nextSong();
        });

        btn_prev_song.setOnClickListener(v -> {
            prevSong();
        });

        chipGroup.setOnCheckedChangeListener((chipGroup, i) -> {
            loading_dialog.show();
            Chip selectedChip = chipGroup.findViewById(i);
            setReduceSizeAnimation(selectedChip);
            setRegainSizeAnimation(selectedChip);
            if (selectedChip != null) {
                String selectedArtist = selectedChip.getText().toString();
                if (selectedArtist.equals("All Artists")) {
                    songs.clear();
                    songs.addAll(mainSongsList);
                    rv_songs.setAdapter(new RVSongsAdapter(songs, ManualActivity.this));
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
                }
            }
            loading_dialog.dismiss();
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
    }

    private void getSongs() {
        loading_dialog.show();

        if (!mainSongsList.isEmpty()) {
            mainSongsList.clear();
        }
        songsRef = FirebaseDatabase.getInstance().getReference("Songs");
        songsRef.keepSynced(true);
        songsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    String songName = ds.child("songName").getValue(String.class);
                    String url = ds.child("url").getValue(String.class);
                    ArrayList<String> artistsList = new ArrayList<>();
                    for (DataSnapshot ds1 : ds.child("artists").getChildren()) {
                        artistsList.add(ds1.getValue(String.class));
                    }
                    mainSongsList.add(new SongModel(url, songName, artistsList));
                }
                if (!mainSongsList.isEmpty()) {
                    tv_total_songs.setText(getResources().getString(R.string.total_songs).concat(String.valueOf(mainSongsList.size())));
                    tv_total_songs.setVisibility(View.VISIBLE);
                    songs.clear();
                    songs.addAll(mainSongsList);
                    getArtists();
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
            }
        }
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

    private void primarySeekBarProgressUpdater() {
        if (!getSharedPreferences("released", MODE_PRIVATE).getBoolean("released", true))
            if (isExoPlaying()) {
                seekBar.setProgress((int) (((float) exoPlayer.getCurrentPosition() / mediaFileLengthInMilliseconds) * 100));
                tv_current_time.setText(milliSecondsToTimer(exoPlayer.getCurrentPosition()));
                Runnable notification = new Runnable() {
                    public void run() {
                        primarySeekBarProgressUpdater();
                    }
                };
                handler.postDelayed(notification, 1000);
            }
    }

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
                    loading_dialog.show();
                    //  Toast.makeText(ManualActivity.this, "Buffering....", Toast.LENGTH_SHORT).show();
                    CreateNotification.createNotification(ManualActivity.this, mediaSessionCompat, songs.get(index), R.drawable.play_btn_black, false, true, false, "manual");
                }

                if (playbackState == Player.STATE_READY) {
                    seekBar.setSecondaryProgress((int) exoPlayer.getBufferedPosition());
                    mediaFileLengthInMilliseconds = (int) exoPlayer.getDuration();
                    tv_total_time.setText(milliSecondsToTimer(mediaFileLengthInMilliseconds));
                    primarySeekBarProgressUpdater();
                }

                if (playbackState == Player.STATE_ENDED) {
                    btn_play_pause.setImageResource(R.drawable.play_btn_black);
                    CreateNotification.createNotification(ManualActivity.this, mediaSessionCompat, songs.get(index), R.drawable.play_btn_black, false, false, true, "manual");
                }

                if (playWhenReady && playbackState == Player.STATE_READY) {
                    // media actually playing
                    loading_dialog.dismiss();
                    btn_play_pause.setImageResource(R.drawable.pause_btn_black);
                    btn_next_song.setEnabled(true);
                    btn_prev_song.setEnabled(true);
                    alphaAnimation(btn_next_song, 0, 1f);
                    alphaAnimation(btn_prev_song, 0, 1f);
                    CreateNotification.createNotification(ManualActivity.this, mediaSessionCompat, songs.get(index), R.drawable.pause_btn_black, false, false, false, "manual");
                } else if (playWhenReady) {
                    // might be idle (plays after prepare()),
                    // buffering (plays when data available)
                    // or ended (plays when seek away from end)
                    // Toast.makeText(ManualActivity.this, "Buffering....", Toast.LENGTH_SHORT).show();
                } else {
                    // player paused in any state
                    btn_play_pause.setImageResource(R.drawable.play_btn_black);
                    btn_next_song.setEnabled(false);
                    btn_prev_song.setEnabled(false);
                    alphaAnimation(btn_next_song, 1f, 0);
                    alphaAnimation(btn_prev_song, 1f, 0);
                    CreateNotification.createNotification(ManualActivity.this, mediaSessionCompat, songs.get(index), R.drawable.play_btn_black, false, false, true, "manual");
                }
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                Toast.makeText(ManualActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setUpExoPlayer(SongModel song) {
        MediaSource mediaSource = new ExtractorMediaSource(Uri.parse(song.getUrl()), dataSourceFactory, extractorsFactory, null, Throwable::printStackTrace);
        exoPlayer.prepare(mediaSource);
        exoPlayer.setPlayWhenReady(true);

        seekBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (isExoPlaying()) {
                    SeekBar sb = (SeekBar) view;
                    int playPositionInMillisecconds = (mediaFileLengthInMilliseconds / 100) * sb.getProgress();
                    exoPlayer.seekTo(playPositionInMillisecconds);
                }
                return false;
            }
        });
    }

    private void playPauseSong() {
        setReduceSizeAnimation(btn_play_pause);
        setRegainSizeAnimation(btn_play_pause);

        if (!isExoPlaying()) {
            startPlayer();
        } else {
            pausePlayer();
        }
        primarySeekBarProgressUpdater();
    }

    private void nextSong() {
        loading_dialog.show();
        if (index + 1 >= songs.size()) {
            Toast.makeText(ManualActivity.this, "This is the last song", Toast.LENGTH_SHORT).show();
            loading_dialog.dismiss();
        } else {
            index++;
            SongModel nextSong = songs.get(index);
            ArrayList<String> artistList = nextSong.getArtists();
            SpannableStringBuilder artists = new SpannableStringBuilder();

            for (int i = 0; i < artistList.size(); i++) {
                String artist = artistList.get(i);
                if (i == artistList.size() - 1) {
                    artists.append(artist);
                } else {
                    artists.append(artist).append(" | ");
                }
            }

            tv_sliding_view_song_name.setText(nextSong.getSongName() + " - " + artists);
            tv_sliding_view_song_name.setSelected(true);
            CreateNotification.createNotification(ManualActivity.this, mediaSessionCompat, nextSong, R.drawable.play_btn_black, true, false, false, "manual");
            setUpExoPlayer(nextSong);
        }
    }

    private void prevSong() {
        loading_dialog.show();
        if (index - 1 < 0) {
            Toast.makeText(ManualActivity.this, "This is the first song", Toast.LENGTH_SHORT).show();
            loading_dialog.dismiss();
        } else {
            index--;
            SongModel prevSong = songs.get(index);
            ArrayList<String> artistList = prevSong.getArtists();
            SpannableStringBuilder artists = new SpannableStringBuilder();

            for (int i = 0; i < artistList.size(); i++) {
                String artist = artistList.get(i);
                if (i == artistList.size() - 1) {
                    artists.append(artist);
                } else {
                    artists.append(artist).append(" | ");
                }
            }

            tv_sliding_view_song_name.setText(prevSong.getSongName() + " - " + artists);
            tv_sliding_view_song_name.setSelected(true);
            CreateNotification.createNotification(ManualActivity.this, mediaSessionCompat, prevSong, R.drawable.play_btn_black, true, false, false, "manual");
            setUpExoPlayer(prevSong);
        }
    }

    public void holderItemOnClick(int position) {
        SongModel song = songs.get(position);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                loading_dialog.show();
                index = position;

                ArrayList<String> artistList = song.getArtists();
                SpannableStringBuilder artists = new SpannableStringBuilder();

                for (int i = 0; i < artistList.size(); i++) {
                    String artist = artistList.get(i);
                    if (i == artistList.size() - 1) {
                        artists.append(artist);
                    } else {
                        artists.append(artist).append(" | ");
                    }
                }

                if (!btn_next_song.isEnabled()) {
                    btn_next_song.setEnabled(true);
                    alphaAnimation(btn_next_song, 0, 1f);
                }
                if (!btn_prev_song.isEnabled()) {
                    btn_prev_song.setEnabled(true);
                    alphaAnimation(btn_prev_song, 0, 1f);
                }
                tv_sliding_view_song_name.setText(song.getSongName() + " - " + artists);
                tv_sliding_view_song_name.setSelected(true);
                CreateNotification.createNotification(ManualActivity.this, mediaSessionCompat, song, R.drawable.play_btn_black, true, false, false, "manual");
                setUpExoPlayer(song);
                slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                setMargins(rv_songs, 0, 0, 0, 150);
            }
        }, 100);
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CreateNotification.CHANNEL_ID, "Karvaan Music Notifications", NotificationManager.IMPORTANCE_LOW);

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
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getSharedPreferences("released", MODE_PRIVATE).edit().putBoolean("released", true).commit();
        pausePlayer();
        if (exoPlayer != null) {
            exoPlayer.release();
        }
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
        unregisterReceiver(broadcastReceiver);
    }
}
