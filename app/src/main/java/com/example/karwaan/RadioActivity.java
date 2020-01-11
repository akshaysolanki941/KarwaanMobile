package com.example.karwaan;

import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.karwaan.Adapters.RVRadioAdapter;
import com.example.karwaan.Models.RadioModel;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class RadioActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView toolbar_title, tv_radio_details;
    private ImageView bg, loading_gif_imageView;
    private Dialog loading_dialog;
    private RecyclerView rv_radio;
    private ArrayList<RadioModel> radioList = new ArrayList<>();
    private NotificationManager notificationManager;
    private int index = 0;

    private ExoPlayer exoPlayer;
    private BandwidthMeter bandwidthMeter;
    private ExtractorsFactory extractorsFactory;
    private TrackSelection.Factory trackSelectionfactory;
    private DataSource.Factory dataSourceFactory;

    MediaPlayer mediaPlayer = new MediaPlayer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radio);

        toolbar = (Toolbar) findViewById(R.id.toolBar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);
        toolbar_title.setText(getString(R.string.radio_toolbar));
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
        Glide.with(this).load(R.drawable.bg).into(bg);
        tv_radio_details = findViewById(R.id.tv_radio_details);

        rv_radio = findViewById(R.id.rv_radio);
        rv_radio.setHasFixedSize(true);
        rv_radio.setLayoutManager(new LinearLayoutManager(this));

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        registerReceiver(broadcastReceiver, new IntentFilter("SONGS"));
        startService(new Intent(getBaseContext(), OnClearFromRecentService.class));

        initExoPlayer();
        getRadios();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void getRadios() {
        loading_dialog.show();
        if (!radioList.isEmpty()) {
            radioList.clear();
        }
        DatabaseReference radioRef = FirebaseDatabase.getInstance().getReference("Radio");
        radioRef.keepSynced(true);
        radioRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    radioList.add(ds.getValue(RadioModel.class));
                }
                if (!radioList.isEmpty()) {
                    rv_radio.setAdapter(new RVRadioAdapter(radioList, RadioActivity.this));
                } else {
                    Toast.makeText(RadioActivity.this, "No radios found", Toast.LENGTH_SHORT).show();
                }
                loading_dialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(RadioActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                loading_dialog.dismiss();
            }
        });
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

                }

                if (playbackState == Player.STATE_READY) {

                }

                if (playbackState == Player.STATE_ENDED) {

                }

                if (playWhenReady && playbackState == Player.STATE_READY) {
                    // media actually playing
                    loading_dialog.dismiss();

                } else if (playWhenReady) {
                    // might be idle (plays after prepare()),
                    // buffering (plays when data available)
                    // or ended (plays when seek away from end)
                    // Toast.makeText(ManualActivity.this, "Buffering....", Toast.LENGTH_SHORT).show();
                } else {
                    // player paused in any state

                }
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                loading_dialog.dismiss();
                Toast.makeText(RadioActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setUpExoPlayer(RadioModel radio) {
        MediaSource mediaSource = new ExtractorMediaSource(Uri.parse(radio.getRadioUrl()), dataSourceFactory, extractorsFactory, null, Throwable::printStackTrace);
        exoPlayer.prepare(mediaSource);
        exoPlayer.setPlayWhenReady(true);
    }

    public void holderItemOnClick(int position) {
        RadioModel radio = radioList.get(position);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                loading_dialog.show();
                index = position;

                setUpExoPlayer(radio);
            }
        }, 100);
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getExtras().getString("actionname");

            if (action.equals(CreateNotification.ACTION_PLAY)) {

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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pausePlayer();
        if (exoPlayer != null) {
            exoPlayer.release();
        }
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
        unregisterReceiver(broadcastReceiver);
    }

  /*  private static class GetMetaData extends AsyncTask<Void, Void, Void> {

        RadioModel radio;
        String text = "";
        TextView v;

        GetMetaData(RadioModel radio, TextView v) {
            this.radio = radio;
            this.v = v;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(radio.getRadioUrl(), new HashMap<String, String>());
            text = mmr.extractMetadata(MediaMetadataRetriever.M);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            v.setText(text);
        }
    }*/
}
