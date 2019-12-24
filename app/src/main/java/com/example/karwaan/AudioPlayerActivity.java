package com.example.karwaan;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;

public class AudioPlayerActivity extends AppCompatActivity implements View.OnClickListener,
        View.OnTouchListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener {

    private ProgressDialog progressBar;

    private ImageButton buttonPlayPause;
    private SeekBar seekBarProgress;

    private MediaPlayer mediaPlayer;
    private int mediaFileLengthInMilliseconds; // this value contains the song
    // duration in milliseconds.
    // Look at getDuration() method
    // in MediaPlayer class

    private final Handler handler = new Handler();

    String audioName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);


        initilizeUI();

        audioName = getIntent().getStringExtra("audioName");

    }

    /**
     * This method is used to Initialize UI Components.
     */
    private void initilizeUI() {
        buttonPlayPause = (ImageButton) findViewById(R.id.ButtonTestPlayPause);
        buttonPlayPause.setImageResource(R.drawable.play_button);
        buttonPlayPause.setOnClickListener(this);
        seekBarProgress = (SeekBar) findViewById(R.id.SeekBarTestPlay);
        seekBarProgress.setMax(99);
        seekBarProgress.setOnTouchListener(this);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnCompletionListener(this);
    }

    /**
     * Method which updates the SeekBar primary progress by current song playing
     * position
     */
    private void primarySeekBarProgressUpdater() {
        seekBarProgress.setProgress((int) (((float) mediaPlayer.getCurrentPosition() / mediaFileLengthInMilliseconds) * 100)); // This
        // math
        // construction
        // give
        // a
        // percentage
        // of
        // "was playing"/"song length"
        if (mediaPlayer.isPlaying()) {
            Runnable notification = new Runnable() {
                public void run() {
                    primarySeekBarProgressUpdater();
                }
            };
            handler.postDelayed(notification, 1000);
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        /**
         * Method which updates the SeekBar secondary progress by current song
         * loading from URL position
         */
        seekBarProgress.setSecondaryProgress(percent);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        /**
         * MediaPlayer onCompletion event handler. Method which calls then song
         * playing is complete
         */
        // buttonPlayPause.setImageResource(R.drawable.button_play);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.SeekBarTestPlay) {
            /**
             * Seekbar onTouch event handler. Method which seeks MediaPlayer to
             * seekBar primary progress position
             */
            if (mediaPlayer.isPlaying()) {
                SeekBar sb = (SeekBar) v;
                int playPositionInMillisecconds = (mediaFileLengthInMilliseconds / 100)
                        * sb.getProgress();
                mediaPlayer.seekTo(playPositionInMillisecconds);
            }
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.ButtonTestPlayPause) {
            /**
             * ImageButton onClick event handler. Method which start/pause
             * mediaplayer playing
             */
            try {
                mediaPlayer.setDataSource(audioName); // setup
                // song
                // from
                // http://www.hrupin.com/wp-content/uploads/mp3/testsong_20_sec.mp3
                // URL
                // to
                // mediaplayer
                // data
                // source
                mediaPlayer.prepare(); // you must call this method after setup
                // the datasource in setDataSource
                // method. After calling prepare() the
                // instance of MediaPlayer starts load
                // data from URL to internal buffer.
            } catch (Exception e) {
                e.printStackTrace();
            }

            mediaFileLengthInMilliseconds = mediaPlayer.getDuration(); // gets
            // the
            // song
            // length
            // in
            // milliseconds
            // from
            // URL

            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                buttonPlayPause.setImageResource(R.drawable.pause_button);

            } else {
                mediaPlayer.pause();
                buttonPlayPause.setImageResource(R.drawable.play_button);
            }
            primarySeekBarProgressUpdater();
        }
    }

    @Override
    public void onBackPressed() {
        // TODO Auto-generated method stub
        super.onBackPressed();
        mediaPlayer.stop();
        this.finish();
    }

}
