package com.example.karwaan;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Dialog;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.karwaan.Adapters.RVSongsAdapter;
import com.example.karwaan.Models.SongModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mancj.materialsearchbar.MaterialSearchBar;
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
    private TextView toolbar_title, tv_sliding_view_song_name, tv_current_time, tv_total_time;
    private RecyclerView rv_songs;
    private DatabaseReference songsRef;
    private ArrayList<SongModel> songs = new ArrayList<>();
    private ArrayList<String> artistsList = new ArrayList<>();
    private SlidingUpPanelLayout slidingUpPanelLayout;
    private ImageButton btn_play_pause, btn_next_song, btn_prev_song;
    private SeekBar seekBar;
    private Dialog loading_dialog;
    private ImageView loading_gif_imageView, bg;
    private MaterialSearchBar searchBar;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private ChipGroup chipGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);

        getSharedPreferences("released", MODE_PRIVATE).edit().putBoolean("released", false).commit();

        toolbar = (Toolbar) findViewById(R.id.toolBar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);
        toolbar_title.setText("Maunal Mode");
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
        searchBar = findViewById(R.id.searchBar);
        slidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        btn_play_pause = (ImageButton) findViewById(R.id.btn_play_pause);
        btn_next_song = findViewById(R.id.btn_next_song);
        btn_prev_song = findViewById(R.id.btn_prev_song);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        tv_current_time = findViewById(R.id.tv_current_time);
        tv_total_time = findViewById(R.id.tv_total_time);
        tv_sliding_view_song_name = findViewById(R.id.tv_sliding_view_song_name);
        tv_sliding_view_song_name.setText("Select a song to play");
        chipGroup = findViewById(R.id.chipGroup);
        rv_songs = (RecyclerView) findViewById(R.id.rv_songs);
        rv_songs.setHasFixedSize(true);
        rv_songs.setLayoutManager(new LinearLayoutManager(this));

        mediaPlayer.setScreenOnWhilePlaying(true);
        getSongs();

    }

    @Override
    protected void onStart() {
        super.onStart();

        Glide.with(this).load(R.drawable.bg).into(bg);

        searchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {

            }

            @Override
            public void onSearchConfirmed(CharSequence text) {
                search(text);
            }

            @Override
            public void onButtonClicked(int buttonCode) {
                if (buttonCode == MaterialSearchBar.BUTTON_BACK) {

                }
            }
        });

        /*searchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                ArrayList<String> suggestions = new ArrayList<>();
                for (SongModel model : songs) {
                    if (model.getSongName().toLowerCase().contains(charSequence.toString().toLowerCase())) {
                        suggestions.add(model.getSongName());
                    }
                }
                searchBar.setLastSuggestions(suggestions);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });*/

        chipGroup.setOnCheckedChangeListener((chipGroup, i) -> {
            loading_dialog.show();
            ArrayList<SongModel> artistSongsList = new ArrayList<>();
            Chip selectedChip = chipGroup.findViewById(i);
            setReduceSizeAnimation(selectedChip);
            setRegainSizeAnimation(selectedChip);
            if (selectedChip != null) {
                String selectedArtist = selectedChip.getText().toString();
                if (selectedArtist.equals("All Artists")) {
                    rv_songs.setAdapter(new RVSongsAdapter(songs, ManualActivity.this, mediaPlayer, slidingUpPanelLayout, btn_play_pause, btn_next_song, btn_prev_song,
                            seekBar, tv_sliding_view_song_name, tv_current_time, tv_total_time, rv_songs));
                } else {
                    for (SongModel song : songs) {
                        for (String artistName : song.getArtists()) {
                            if (artistName.equals(selectedArtist)) {
                                artistSongsList.add(song);
                                break;
                            }
                        }
                    }
                    rv_songs.setAdapter(new RVSongsAdapter(artistSongsList, ManualActivity.this, mediaPlayer, slidingUpPanelLayout, btn_play_pause, btn_next_song, btn_prev_song,
                            seekBar, tv_sliding_view_song_name, tv_current_time, tv_total_time, rv_songs));
                }
            }
            loading_dialog.dismiss();
        });
    }

    private void getSongs() {
        loading_dialog.show();

        if (!songs.isEmpty()) {
            songs.clear();
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
                    songs.add(new SongModel(url, songName, artistsList));
                }
                if (!songs.isEmpty()) {
                    ArrayList<String> songNames = new ArrayList<>();
                    /*for (SongModel model : songs) {
                        songNames.add(model.getSongName());
                    }
                    searchBar.setLastSuggestions(songNames);*/
                    getArtists();
                    rv_songs.setAdapter(new RVSongsAdapter(songs, ManualActivity.this, mediaPlayer, slidingUpPanelLayout, btn_play_pause, btn_next_song, btn_prev_song,
                            seekBar, tv_sliding_view_song_name, tv_current_time, tv_total_time, rv_songs));
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
        String query = text.toString().toLowerCase();
        ArrayList<SongModel> searchSongNames = new ArrayList<>();
        for (SongModel song : songs) {
            if (song.getSongName().toLowerCase().contains(query)) {
                searchSongNames.add(song);
            }
        }
        rv_songs.setAdapter(new RVSongsAdapter(searchSongNames, ManualActivity.this, mediaPlayer, slidingUpPanelLayout, btn_play_pause, btn_next_song, btn_prev_song,
                seekBar, tv_sliding_view_song_name, tv_current_time, tv_total_time, rv_songs));
    }

    private void getArtists() {
        generateChip("All Artists");

        if (!artistsList.isEmpty()) {
            artistsList.clear();
        }

        for (SongModel song : songs) {
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
        mediaPlayer.release();
    }
}
