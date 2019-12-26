package com.example.karwaan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.example.karwaan.Models.SongModel;
import com.example.karwaan.Services.NotificationService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class SaregamaActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView toolbar_title;
    private ArrayList<SongModel> songList = new ArrayList<>();
    private DatabaseReference songRef;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private int index = 0;
    private ImageButton btn_play_pause, btn_next, btn_previous, btn_forward10, btn_backward10;
    private LottieAnimationView lottieAnimationView;
    private ImageView bg, loading_gif_imageView;
    private Dialog loading_dialog;
    //private BroadcastReceiver receiver;

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

        /*Intent serviceIntent = new Intent(SaregamaActivity.this, NotificationService.class);
        serviceIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
        startService(serviceIntent);*/

        /*receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, "received", Toast.LENGTH_SHORT).show();
                switch (intent.getAction()) {
                    case Constants.ACTION.NEXT_ACTION:
                        index++;
                        if (index >= songList.size() + 1) {
                            index = 0;
                            Collections.shuffle(songList);
                            playSong(songList.get(index));
                        } else {
                            playSong(songList.get(index));
                        }
                        break;

                    case Constants.ACTION.PREV_ACTION:
                        index--;
                        if (index <= -1) {
                            index = 0;
                            Collections.shuffle(songList);
                            playSong(songList.get(index));
                        } else {
                            playSong(songList.get(index));
                        }
                        break;

                    case Constants.ACTION.NEXT_10_ACTION:
                        index += 10;
                        if (index >= songList.size() + 1) {
                            index = 0;
                            Collections.shuffle(songList);
                            playSong(songList.get(index));
                        } else {
                            playSong(songList.get(index));
                        }
                        break;

                    case Constants.ACTION.PREV_10_ACTION:
                        index -= 10;
                        if (index <= -1) {
                            index = 0;
                            Collections.shuffle(songList);
                            playSong(songList.get(index));
                        } else {
                            playSong(songList.get(index));
                        }
                        break;
                }
            }
        };*/

        loading_dialog = new Dialog(this);
        loading_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loading_dialog.setContentView(R.layout.loading_dialog);
        loading_gif_imageView = (ImageView) loading_dialog.findViewById(R.id.loading_gif_imageView);
        Glide.with(getApplicationContext()).load(R.drawable.loading).placeholder(R.drawable.loading).into(loading_gif_imageView);
        loading_dialog.setCanceledOnTouchOutside(false);
        loading_dialog.setCancelable(false);
        loading_dialog.show();

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

                } else {
                    mediaPlayer.pause();
                    btn_play_pause.setImageResource(R.drawable.play_button);
                    lottieAnimationView.pauseAnimation();
                }
            }
        });

        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loading_dialog.show();
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
                loading_dialog.show();
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
                loading_dialog.show();
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
    protected void onResume() {
        super.onResume();
      /*  LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, new IntentFilter(Constants.ACTION.NEXT_ACTION));
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, new IntentFilter(Constants.ACTION.PREV_ACTION));
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, new IntentFilter(Constants.ACTION.NEXT_10_ACTION));
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, new IntentFilter(Constants.ACTION.PREV_10_ACTION));*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(receiver);
    }

    private void getSongsList() {
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
            mediaPlayer.prepare();
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
            }
        });
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
