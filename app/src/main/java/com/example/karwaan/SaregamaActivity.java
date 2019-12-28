package com.example.karwaan;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Dialog;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.media.MediaBrowserCompat;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.example.karwaan.Models.SongModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SaregamaActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView toolbar_title, tv_saregama_song_details;
    private ArrayList<SongModel> songList = new ArrayList<>();
    private DatabaseReference songRef;
    private MediaPlayer mediaPlayer;
    private int index = 0;
    private ImageButton btn_play_pause, btn_next, btn_previous, btn_forward10, btn_backward10;
    private LottieAnimationView lottieAnimationView;
    private ImageView bg, loading_gif_imageView;
    private Dialog loading_dialog;

    private MediaBrowserCompat mediaBrowser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saregama);

        toolbar = (Toolbar) findViewById(R.id.toolBar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);
        toolbar_title.setText("Saregama Mode");
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        btn_play_pause = (ImageButton) findViewById(R.id.btn_play_pause);
        btn_next = (ImageButton) findViewById(R.id.btn_next);
        btn_previous = (ImageButton) findViewById(R.id.btn_previous);
        btn_forward10 = (ImageButton) findViewById(R.id.btn_forward10);
        btn_backward10 = (ImageButton) findViewById(R.id.btn_backward10);
        lottieAnimationView = (LottieAnimationView) findViewById(R.id.lottie_animation_view);
        bg = findViewById(R.id.bg);
        tv_saregama_song_details = findViewById(R.id.tv_saregama_song_details);

        loading_dialog = new Dialog(this);
        loading_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loading_dialog.setContentView(R.layout.loading_dialog);
        loading_gif_imageView = (ImageView) loading_dialog.findViewById(R.id.loading_gif_imageView);
        Glide.with(getApplicationContext()).load(R.drawable.loading).placeholder(R.drawable.loading).into(loading_gif_imageView);
        loading_dialog.setCanceledOnTouchOutside(false);
        loading_dialog.setCancelable(false);

        setReduceSizeAnimation(btn_play_pause);
        setReduceSizeAnimation(btn_next);
        setReduceSizeAnimation(btn_previous);
        setReduceSizeAnimation(btn_forward10);
        setReduceSizeAnimation(btn_backward10);

        initMediaPlayer();
        getSongsList();

    }

    @Override
    protected void onStart() {
        super.onStart();

        Glide.with(this).load(R.drawable.bg).into(bg);

        if (!mediaPlayer.isPlaying()) {
            btn_play_pause.setImageResource(R.drawable.play_button);
            lottieAnimationView.pauseAnimation();

        } else {
            btn_play_pause.setImageResource(R.drawable.pause_button);
            lottieAnimationView.playAnimation();
        }

        btn_play_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                    btn_play_pause.setImageResource(R.drawable.pause_button);
                    lottieAnimationView.playAnimation();
                    setRegainSizeAnimation(btn_play_pause);
                    setRegainSizeAnimation(btn_next);
                    setRegainSizeAnimation(btn_previous);
                    setRegainSizeAnimation(btn_forward10);
                    setRegainSizeAnimation(btn_backward10);

                } else {
                    mediaPlayer.pause();
                    btn_play_pause.setImageResource(R.drawable.play_button);
                    lottieAnimationView.pauseAnimation();
                    setReduceSizeAnimation(btn_play_pause);
                    setReduceSizeAnimation(btn_next);
                    setReduceSizeAnimation(btn_previous);
                    setReduceSizeAnimation(btn_forward10);
                    setReduceSizeAnimation(btn_backward10);
                }
            }
        });

        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loading_dialog.show();
                lottieAnimationView.pauseAnimation();
                index++;
                if (index >= songList.size() + 1) {
                    index = 0;
                    Collections.shuffle(songList);
                    playSong(songList.get(index));
                } else {
                    playSong(songList.get(index));
                }
            }
        });

        btn_previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loading_dialog.show();
                lottieAnimationView.pauseAnimation();
                index--;
                if (index <= -1) {
                    index = 0;
                    Collections.shuffle(songList);
                    playSong(songList.get(index));
                } else {
                    playSong(songList.get(index));
                }
            }
        });

        btn_forward10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(SaregamaActivity.this, "Skipping 10 songs in forward direction", Toast.LENGTH_SHORT).show();
                loading_dialog.show();
                lottieAnimationView.pauseAnimation();
                index += 10;
                if (index >= songList.size() + 1) {
                    index = 0;
                    Collections.shuffle(songList);
                    playSong(songList.get(index));
                } else {
                    playSong(songList.get(index));
                }
            }
        });

        btn_backward10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(SaregamaActivity.this, "Skipping 10 songs in backward direction", Toast.LENGTH_SHORT).show();
                loading_dialog.show();
                lottieAnimationView.pauseAnimation();
                index -= 10;
                if (index <= -1) {
                    index = 0;
                    Collections.shuffle(songList);
                    playSong(songList.get(index));
                } else {
                    playSong(songList.get(index));
                }
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
        lottieAnimationView.pauseAnimation();
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setScreenOnWhilePlaying(true);
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        //mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setVolume(1.0f, 1.0f);
    }

    private void getSongsList() {
        loading_dialog.show();
        if (!songList.isEmpty()) {
            songList.clear();
        }
        songRef = FirebaseDatabase.getInstance().getReference("Songs");
        songRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    SongModel song = ds.getValue(SongModel.class);
                    if (song != null) {
                        songList.add(song);
                    }
                }
                startRandomSongs();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void startRandomSongs() {
        Collections.shuffle(songList);
        playSong(songList.get(index));

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                loading_dialog.show();
                lottieAnimationView.pauseAnimation();
                index++;
                if (index >= songList.size() + 1) {
                    index = 0;
                    Collections.shuffle(songList);
                    playSong(songList.get(index));
                } else {
                    playSong(songList.get(index));
                }
            }
        });
    }

    private void playSong(SongModel song) {

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        } else if (!mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }

        try {
            mediaPlayer.setDataSource(song.getUrl());
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
                btn_play_pause.setImageResource(R.drawable.pause_button);
                lottieAnimationView.playAnimation();
                loading_dialog.dismiss();
                setRegainSizeAnimation(btn_play_pause);
                setRegainSizeAnimation(btn_next);
                setRegainSizeAnimation(btn_previous);
                setRegainSizeAnimation(btn_forward10);
                setRegainSizeAnimation(btn_backward10);
            }
        });

        tv_saregama_song_details.setText(song.getSongName());
        tv_saregama_song_details.setSelected(true);
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
        mediaPlayer.release();
        lottieAnimationView.pauseAnimation();
    }
}
