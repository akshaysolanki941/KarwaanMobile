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
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.akexorcist.roundcornerprogressbar.RoundCornerProgressBar;
import com.bumptech.glide.Glide;
import com.example.karwaan.Adapters.RVOfflineSongsAdapter;
import com.example.karwaan.Constants.Constants;
import com.example.karwaan.Models.SongModel;
import com.example.karwaan.Services.ManualOfflinePlaybackService;
import com.example.karwaan.Utils.FilesUtil;
import com.example.karwaan.Utils.TinyDB;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.apache.commons.io.FileUtils;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ManualOfflineActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView toolbar_title, tv_sliding_view_song_name, tv_current_time, tv_total_time, tv_total_songs, tv_total_size;
    private RecyclerView rv_songs;
    private ArrayList<Object> songs = new ArrayList<>();
    private ArrayList<Object> mainSongsList = new ArrayList<>();
    private HashSet<String> artistHashSet = new HashSet<>();
    private SlidingUpPanelLayout slidingUpPanelLayout;
    private ImageButton btn_play_pause, btn_next_song, btn_prev_song, btn_forward_10, btn_backward_10;
    private Dialog loading_dialog;
    private ImageView bg;
    private EditText et_search;
    private LottieAnimationView lottie_animation_view;
    private long total_duration;

    private NotificationManager notificationManager;
    private MediaBrowserCompat mediaBrowser;
    private MediaControllerCompat mediaController;

    private RoundCornerProgressBar seekbar;
    private SeekBar seekBarInvisible;
    private List<Integer> allColors;

    private TinyDB tinyDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_offline);

        toolbar = findViewById(R.id.toolBar);
        toolbar_title = findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);
        toolbar_title.setText(getString(R.string.toolbar_manual_mode_offline));
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
        tv_total_size = findViewById(R.id.tv_total_size);
        tv_total_songs = findViewById(R.id.tv_total_songs);
        tv_total_songs.setVisibility(View.GONE);
        tv_sliding_view_song_name = findViewById(R.id.tv_sliding_view_song_name);
        tv_sliding_view_song_name.setText("Select a song to play");
        rv_songs = findViewById(R.id.rv_songs);
        rv_songs.setHasFixedSize(true);
        rv_songs.setLayoutManager(new LinearLayoutManager(this));

        seekbar = findViewById(R.id.seekBar);
        seekBarInvisible = findViewById(R.id.seekBarInvisible);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast, new IntentFilter("loadingDismiss"));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast1, new IntentFilter("updateCurrentTimeOffline"));

        mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, ManualOfflinePlaybackService.class), connectionCallbacks, null);
        mediaBrowser.connect();

        try {
            allColors = getAllMaterialColors();
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }


        tinyDB = new TinyDB(this);
        getSongs();
        updateSeekbarColor();
        getTotalSize();
    }

    @Override
    protected void onStart() {
        super.onStart();

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
                    LocalBroadcastManager.getInstance(ManualOfflineActivity.this).sendBroadcast(intent);
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

    private final MediaBrowserCompat.ConnectionCallback connectionCallbacks =
            new MediaBrowserCompat.ConnectionCallback() {
                @Override
                public void onConnected() {
                    MediaSessionCompat.Token token = mediaBrowser.getSessionToken();
                    MediaControllerCompat mediaController = null;
                    try {
                        mediaController = new MediaControllerCompat(ManualOfflineActivity.this, token);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    MediaControllerCompat.setMediaController(ManualOfflineActivity.this, mediaController);
                    buildTransportControls();
                }

                @Override
                public void onConnectionSuspended() {
                    // The Service has crashed. Disable transport controls until it automatically reconnects
                    Toast.makeText(ManualOfflineActivity.this, "connection suspended", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onConnectionFailed() {
                    Toast.makeText(ManualOfflineActivity.this, "connection failed", Toast.LENGTH_SHORT).show();
                    // The Service has refused our connection
                }
            };

    void buildTransportControls() {
        btn_play_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setReduceSizeAnimation(btn_play_pause);
                setRegainSizeAnimation(btn_play_pause);
                int pbState = MediaControllerCompat.getMediaController(ManualOfflineActivity.this).getPlaybackState().getState();
                if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                    MediaControllerCompat.getMediaController(ManualOfflineActivity.this).getTransportControls().pause();
                } else {
                    MediaControllerCompat.getMediaController(ManualOfflineActivity.this).getTransportControls().play();
                }
            }
        });

        btn_next_song.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(ManualOfflineActivity.this).getTransportControls().skipToNext();
            }
        });

        btn_prev_song.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(ManualOfflineActivity.this).getTransportControls().skipToPrevious();
            }
        });

        btn_forward_10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(ManualOfflineActivity.this).getTransportControls().fastForward();
            }
        });

        btn_backward_10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(ManualOfflineActivity.this).getTransportControls().rewind();
            }
        });
        mediaController = MediaControllerCompat.getMediaController(ManualOfflineActivity.this);
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
                    total_duration = duration;
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
                                setMargins(rv_songs, 0, 0, 0, 150);
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
                            break;

                        case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
                            break;

                        case PlaybackStateCompat.STATE_FAST_FORWARDING:
                            break;

                        case PlaybackStateCompat.STATE_REWINDING:
                            break;

                        case PlaybackStateCompat.STATE_STOPPED:
                            btn_play_pause.setImageResource(R.drawable.play_btn_black);
                            break;

                        case PlaybackStateCompat.STATE_BUFFERING:
                            break;
                    }
                }
            };

    private void getSongs() {
        loading_dialog.show();
        if (!mainSongsList.isEmpty()) {
            mainSongsList.clear();
        }
        mainSongsList = tinyDB.getListObject("downloadedSongList", SongModel.class);
        songs.clear();
        songs.addAll(mainSongsList);
        if (!songs.isEmpty()) {
            setTotalSongsCount();
            tv_total_songs.setVisibility(View.VISIBLE);
            rv_songs.setAdapter(new RVOfflineSongsAdapter(songs, ManualOfflineActivity.this));
        } else {
            Toast.makeText(this, "No Offline Songs", Toast.LENGTH_SHORT).show();
        }
    }

    private void search(CharSequence text) {
        songs.clear();
        String query = text.toString().toLowerCase();
        for (Object obj : mainSongsList) {
            SongModel song = (SongModel) obj;
            if (song.getSongName().toLowerCase().contains(query)) {
                songs.add(song);
            } else if (song.getMovie().toLowerCase().contains(query)) {
                songs.add(song);
            }
        }
        Intent i = new Intent("searchList");
        i.putExtra("search", query);
        LocalBroadcastManager.getInstance(ManualOfflineActivity.this).sendBroadcast(i);
        rv_songs.setAdapter(new RVOfflineSongsAdapter(songs, ManualOfflineActivity.this));
    }

    public void getTotalSize() {
        File file = new File(FilesUtil.getDirPath(this));
        long size = FileUtils.sizeOfDirectory(file);
        String sizetoDisplay = FileUtils.byteCountToDisplaySize(size);
        tv_total_size.setText("Total size: ".concat(sizetoDisplay));
    }

    public void setTotalSongsCount() {
        ArrayList<Object> temp = tinyDB.getListObject("downloadedSongList", SongModel.class);
        tv_total_songs.setText(getResources().getString(R.string.total_songs).concat(String.valueOf(temp.size())));
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
            if (action.equals("updateCurrentTimeOffline")) {
                tv_current_time.setText(milliSecondsToTimer(intent.getIntExtra("currentTimeOffline", 0)));
                seekBarInvisible.setProgress(intent.getIntExtra("currentTimeOffline", 0));
                seekbar.setProgress(intent.getIntExtra("currentTimeOffline", 0));
            }
        }
    };

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getSharedPreferences("karvaanSharedPref", MODE_PRIVATE).edit().putBoolean("released", true).commit();
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
        rv_songs.setAdapter(null);
        if (MediaControllerCompat.getMediaController(ManualOfflineActivity.this) != null) {
            MediaControllerCompat.getMediaController(ManualOfflineActivity.this).unregisterCallback(controllerCallback);
        }
        mediaBrowser.disconnect();
        FileUtils.deleteQuietly(getCacheDir());
        FileUtils.deleteQuietly(getExternalCacheDir());
    }
}
