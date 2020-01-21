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
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.example.karwaan.Constants.Constants;
import com.example.karwaan.Models.SongModel;
import com.example.karwaan.Services.SaregamaOfflinePlaybackService;
import com.example.karwaan.Utils.TinyDB;

import org.apache.commons.io.FileUtils;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class SaregamaOfflineActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private TextView toolbar_title, tv_saregama_song_details, tv_total_songs;
    private ArrayList<Object> mainSongList = new ArrayList<>();
    private ImageButton btn_play_pause, btn_next, btn_previous, btn_forward10, btn_backward10;
    private LottieAnimationView lottieAnimationView, lottie_animation_view;
    private ImageView bg;
    private Dialog loading_dialog;
    private NotificationManager notificationManager;

    private MediaBrowserCompat mediaBrowser;
    private MediaControllerCompat mediaController;
    private TinyDB tinyDB;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saregama_offline);

        toolbar = findViewById(R.id.toolBar);
        toolbar_title = findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);
        toolbar_title.setText(getString(R.string.saregama_mode_offline));
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        btn_play_pause = findViewById(R.id.btn_play_pause);
        btn_play_pause.bringToFront();
        btn_play_pause.setImageResource(R.drawable.play_button);

        btn_next = findViewById(R.id.btn_next);
        btn_previous = findViewById(R.id.btn_previous);
        btn_forward10 = findViewById(R.id.btn_forward10);
        btn_backward10 = findViewById(R.id.btn_backward10);
        lottieAnimationView = findViewById(R.id.lottie_animation_view);
        bg = findViewById(R.id.bg);
        Glide.with(this).load(R.drawable.bg).into(bg);

        tv_saregama_song_details = findViewById(R.id.tv_saregama_song_details);
        tv_total_songs = findViewById(R.id.tv_total_songs);

        loading_dialog = new Dialog(this);
        loading_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loading_dialog.setContentView(R.layout.loading_dialog);
        lottie_animation_view = loading_dialog.findViewById(R.id.lottie_animation_view);
        lottie_animation_view.playAnimation();
        loading_dialog.setCanceledOnTouchOutside(false);
        loading_dialog.setCancelable(false);

        tinyDB = new TinyDB(this);
        mainSongList.clear();
        mainSongList = tinyDB.getListObject("downloadedSongList", SongModel.class);
        if (!mainSongList.isEmpty()) {
            tv_total_songs.setText(getResources().getString(R.string.total_songs).concat(String.valueOf(mainSongList.size())));
            btn_play_pause.setImageResource(R.drawable.pause_button);
        } else {
            tv_total_songs.setText(getString(R.string.no_offline_songs));
            lottieAnimationView.pauseAnimation();
            alphaAnimation(lottieAnimationView, 1f, 0);
            btn_play_pause.setVisibility(View.GONE);
            btn_backward10.setVisibility(View.GONE);
            btn_forward10.setVisibility(View.GONE);
            btn_next.setVisibility(View.GONE);
            btn_previous.setVisibility(View.GONE);
        }

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcast, new IntentFilter("loadingDismiss"));

        mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, SaregamaOfflinePlaybackService.class), connectionCallbacks, null);

        setReduceSizeAnimation(btn_play_pause);
        setReduceSizeAnimation(btn_next);
        setReduceSizeAnimation(btn_previous);
        setReduceSizeAnimation(btn_forward10);
        setReduceSizeAnimation(btn_backward10);

        mediaBrowser.connect();
    }

    private final MediaBrowserCompat.ConnectionCallback connectionCallbacks = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            MediaSessionCompat.Token token = mediaBrowser.getSessionToken();
            MediaControllerCompat mediaController = null;
            try {
                mediaController = new MediaControllerCompat(SaregamaOfflineActivity.this, token);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            MediaControllerCompat.setMediaController(SaregamaOfflineActivity.this, mediaController);
            buildTransportControls();
        }

        @Override
        public void onConnectionSuspended() {
            // The Service has crashed. Disable transport controls until it automatically reconnects
            Toast.makeText(SaregamaOfflineActivity.this, "connection suspended", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnectionFailed() {
            Toast.makeText(SaregamaOfflineActivity.this, "connection failed", Toast.LENGTH_SHORT).show();
            // The Service has refused our connection
        }
    };

    void buildTransportControls() {
        btn_play_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pbState = MediaControllerCompat.getMediaController(SaregamaOfflineActivity.this).getPlaybackState().getState();
                if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                    MediaControllerCompat.getMediaController(SaregamaOfflineActivity.this).getTransportControls().pause();
                } else {
                    MediaControllerCompat.getMediaController(SaregamaOfflineActivity.this).getTransportControls().play();
                }
            }
        });

        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(SaregamaOfflineActivity.this).getTransportControls().skipToNext();
            }
        });

        btn_previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(SaregamaOfflineActivity.this).getTransportControls().skipToPrevious();
            }
        });

        btn_forward10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(SaregamaOfflineActivity.this).getTransportControls().fastForward();
            }
        });

        btn_backward10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(SaregamaOfflineActivity.this).getTransportControls().rewind();
            }
        });
        mediaController = MediaControllerCompat.getMediaController(SaregamaOfflineActivity.this);
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
                            break;

                        case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
                            break;

                        case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
                            break;

                        case PlaybackStateCompat.STATE_FAST_FORWARDING:
                            break;

                        case PlaybackStateCompat.STATE_REWINDING:
                            break;

                        case PlaybackStateCompat.STATE_BUFFERING:
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
                lottieAnimationView.playAnimation();
            }
        }
    };

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
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
        if (MediaControllerCompat.getMediaController(SaregamaOfflineActivity.this) != null) {
            MediaControllerCompat.getMediaController(SaregamaOfflineActivity.this).unregisterCallback(controllerCallback);
        }
        mediaBrowser.disconnect();
        FileUtils.deleteQuietly(getCacheDir());
        FileUtils.deleteQuietly(getExternalCacheDir());
    }
}
