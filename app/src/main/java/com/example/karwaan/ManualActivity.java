package com.example.karwaan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.karwaan.Adapters.RVSongsAdapter;
import com.example.karwaan.Models.SongModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;

public class ManualActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView toolbar_title;
    private RecyclerView rv_songs;
    private DatabaseReference songsRef;
    private ArrayList<SongModel> songs = new ArrayList<>();
    private SlidingUpPanelLayout slidingUpPanelLayout;
    private ImageButton btn_play_pause;
    private SeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);

        toolbar = (Toolbar) findViewById(R.id.toolBar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);
        toolbar_title.setText("Maunal Mode");
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        slidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        btn_play_pause = (ImageButton) findViewById(R.id.btn_play_pause);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        rv_songs = (RecyclerView) findViewById(R.id.rv_songs);
        rv_songs.setHasFixedSize(true);
        rv_songs.setLayoutManager(new LinearLayoutManager(this));

    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!songs.isEmpty()) {
            songs.clear();
        }
        songsRef = FirebaseDatabase.getInstance().getReference("Songs");
        songsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    SongModel song = ds.getValue(SongModel.class);
                    if (song != null) {
                        songs.add(song);
                    }
                }
                rv_songs.setAdapter(new RVSongsAdapter(songs, ManualActivity.this, slidingUpPanelLayout, btn_play_pause, seekBar));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
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
}
