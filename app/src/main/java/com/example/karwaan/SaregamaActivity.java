package com.example.karwaan;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.MotionEvent;
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
import com.bumptech.glide.Glide;
import com.example.karwaan.Models.SongModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SaregamaActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView toolbar_title, tv_saregama_song_details;
    private ArrayList<SongModel> songList = new ArrayList<>();
    private ArrayList<SongModel> mainSongList = new ArrayList<>();
    private ArrayList<String> artistsList = new ArrayList<>();
    private DatabaseReference songRef;
    private MediaPlayer mediaPlayer;
    private int index = 0;
    private ImageButton btn_play_pause, btn_next, btn_previous, btn_forward10, btn_backward10;
    private LottieAnimationView lottieAnimationView;
    private ImageView bg, loading_gif_imageView;
    private Dialog loading_dialog;
    private ChipGroup chipGroup;
    private RelativeLayout rlParentLayout;
    private Boolean voiceModeEnabled;

    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private String keeper = "";

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

        rlParentLayout = findViewById(R.id.rlParentLayout);
        btn_play_pause = (ImageButton) findViewById(R.id.btn_play_pause);
        btn_play_pause.bringToFront();
        btn_next = (ImageButton) findViewById(R.id.btn_next);
        btn_previous = (ImageButton) findViewById(R.id.btn_previous);
        btn_forward10 = (ImageButton) findViewById(R.id.btn_forward10);
        btn_backward10 = (ImageButton) findViewById(R.id.btn_backward10);
        lottieAnimationView = (LottieAnimationView) findViewById(R.id.lottie_animation_view);
        bg = findViewById(R.id.bg);
        tv_saregama_song_details = findViewById(R.id.tv_saregama_song_details);
        chipGroup = findViewById(R.id.chipGroup);

        loading_dialog = new Dialog(this);
        loading_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loading_dialog.setContentView(R.layout.loading_dialog);
        loading_gif_imageView = (ImageView) loading_dialog.findViewById(R.id.loading_gif_imageView);
        Glide.with(getApplicationContext()).load(R.drawable.loading).placeholder(R.drawable.loading).into(loading_gif_imageView);
        loading_dialog.setCanceledOnTouchOutside(false);
        loading_dialog.setCancelable(false);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(SaregamaActivity.this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        speechRecognizerIntent.putExtra("android.speech.extra.DICTATION_MODE", true);

        setReduceSizeAnimation(btn_play_pause);
        setReduceSizeAnimation(btn_next);
        setReduceSizeAnimation(btn_previous);
        setReduceSizeAnimation(btn_forward10);
        setReduceSizeAnimation(btn_backward10);

        voiceModeEnabled = getSharedPreferences("voiceModeEnabled", MODE_PRIVATE).getBoolean("voiceModeEnabled", false);
        if (voiceModeEnabled) {
            btn_play_pause.setVisibility(View.GONE);
            btn_next.setVisibility(View.GONE);
            btn_previous.setVisibility(View.GONE);
            btn_forward10.setVisibility(View.GONE);
            btn_backward10.setVisibility(View.GONE);

            rlParentLayout.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    switch (motionEvent.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            speechRecognizer.startListening(speechRecognizerIntent);
                            keeper = "";
                            break;

                        /*case MotionEvent.ACTION_UP:
                            speechRecognizer.stopListening();
                            break;*/
                    }
                    return false;
                }
            });
        }

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
                playPauseSong();
            }
        });

        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextSong();
            }
        });

        btn_previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                previousSong();
            }
        });

        btn_forward10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                skip10SongsForward();
            }
        });

        btn_backward10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                skip10SongsBackward();
            }
        });

        chipGroup.setOnCheckedChangeListener((chipGroup, i) -> {
            loading_dialog.show();
            lottieAnimationView.pauseAnimation();
            ArrayList<SongModel> artistSongsList = new ArrayList<>();
            Chip selectedChip = chipGroup.findViewById(i);
            setReduceSizeAnimation(selectedChip);
            setRegainSizeAnimation(selectedChip);
            if (selectedChip != null) {
                String selectedArtist = selectedChip.getText().toString();
                if (selectedArtist.equals("All Artists")) {
                    songList.clear();
                    songList.addAll(mainSongList);
                    startRandomSongs();
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
                    startRandomSongs();
                }
            }
        });

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                Toast.makeText(SaregamaActivity.this, "Give a command", Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        speechRecognizer.stopListening();
                    }
                }, 1000);
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
                        nextSong();
                        Toast.makeText(SaregamaActivity.this, "Command: " + keeper, Toast.LENGTH_SHORT).show();
                        break;

                    case "previous":
                        previousSong();
                        Toast.makeText(SaregamaActivity.this, "Command: " + keeper, Toast.LENGTH_SHORT).show();
                        break;

                    case "play":
                        playPauseSong();
                        Toast.makeText(SaregamaActivity.this, "Command: " + keeper, Toast.LENGTH_SHORT).show();
                        break;

                    case "pause":
                        playPauseSong();
                        Toast.makeText(SaregamaActivity.this, "Command: " + keeper, Toast.LENGTH_SHORT).show();
                        break;

                    case "forward":
                        skip10SongsForward();
                        Toast.makeText(SaregamaActivity.this, "Command: " + keeper, Toast.LENGTH_SHORT).show();
                        break;

                    case "backward":
                        skip10SongsBackward();
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
                getArtists();
                songList.clear();
                songList.addAll(mainSongList);
                startRandomSongs();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(SaregamaActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void getArtists() {
        generateChip("All Artists");

        if (!artistsList.isEmpty()) {
            artistsList.clear();
        }

        for (SongModel song : mainSongList) {
            for (String artistName : song.getArtists())
                artistsList.add(artistName);
        }

        HashSet hs = new HashSet();
        hs.addAll(artistsList);
        artistsList.clear();
        artistsList.addAll(hs);

        for (String artistName : artistsList) {
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
        chip.setTextAppearance(R.style.chipTextAppearance);
        chip.setCloseIconEnabled(false);
        chip.setClickable(true);
        chip.setCheckable(true);
        if (title.equals("All Artists")) {
            chip.setChecked(true);
            chip.setTextAppearance(R.style.chipTextAppearanceBold);
        }
        chipGroup.addView(chip);
    }

    private void playPauseSong() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            btn_play_pause.setImageResource(R.drawable.pause_button);
            alphaAnimation(lottieAnimationView, 0, 1f);
            lottieAnimationView.playAnimation();
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

        } else {
            mediaPlayer.pause();
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
        }
    }

    private void nextSong() {
        loading_dialog.show();
        lottieAnimationView.pauseAnimation();
        index++;
        if (index >= songList.size()) {
            index = 0;
            Collections.shuffle(songList);
            playSong(songList.get(index));
        } else {
            playSong(songList.get(index));
        }
    }

    private void previousSong() {
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

    private void skip10SongsForward() {
        Toast.makeText(SaregamaActivity.this, "Skipping 10 songs in forward direction", Toast.LENGTH_SHORT).show();
        loading_dialog.show();
        lottieAnimationView.pauseAnimation();
        index += 10;
        if (index >= songList.size()) {
            index = 0;
            Collections.shuffle(songList);
            playSong(songList.get(index));
        } else {
            playSong(songList.get(index));
        }
    }

    private void skip10SongsBackward() {
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
        mediaPlayer.release();
        lottieAnimationView.pauseAnimation();
    }
}
