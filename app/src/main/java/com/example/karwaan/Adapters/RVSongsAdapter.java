package com.example.karwaan.Adapters;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.karwaan.Models.SongModel;
import com.example.karwaan.R;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RVSongsAdapter extends RecyclerView.Adapter<RVSongsAdapter.ViewHolder> {

    private ArrayList<SongModel> songs;
    private Context context;
    private SlidingUpPanelLayout slidingUpPanelLayout;
    private ImageButton btn_play_pause;
    private SeekBar seekBar;

    private MediaPlayer mediaPlayer = new MediaPlayer();
    private int mediaFileLengthInMilliseconds;
    private final Handler handler = new Handler();

    public RVSongsAdapter(ArrayList<SongModel> songs, Context context, SlidingUpPanelLayout slidingUpPanelLayout, ImageButton btn_play_pause,
                          SeekBar seekBar) {
        this.songs = songs;
        this.context = context;
        this.slidingUpPanelLayout = slidingUpPanelLayout;
        this.btn_play_pause = btn_play_pause;
        this.seekBar = seekBar;
    }

    @NonNull
    @Override
    public RVSongsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.rv_songs_item, viewGroup, false);
        return new RVSongsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final RVSongsAdapter.ViewHolder holder, final int position) {
        final SongModel song = songs.get(position);

        holder.tv_song_name.setText(song.getSongName());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setUpMediaPlayer(song.getUrl());
                slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView tv_song_name;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);

            tv_song_name = (TextView) itemView.findViewById(R.id.tv_song_name);

        }
    }

    private void primarySeekBarProgressUpdater() {
        seekBar.setProgress((int) (((float) mediaPlayer.getCurrentPosition() / mediaFileLengthInMilliseconds) * 100));
        if (mediaPlayer.isPlaying()) {
            Runnable notification = new Runnable() {
                public void run() {
                    primarySeekBarProgressUpdater();
                }
            };
            handler.postDelayed(notification, 10);
        }
    }

    private void setUpMediaPlayer(final String song_url) {
        btn_play_pause.setImageResource(R.drawable.pause_button);

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        } else if (!mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }

        try {
            mediaPlayer.setDataSource(song_url);
            mediaPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaFileLengthInMilliseconds = mediaPlayer.getDuration();
                mediaPlayer.start();
                primarySeekBarProgressUpdater();
            }
        });

        mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
                seekBar.setSecondaryProgress(i);
            }
        });

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                btn_play_pause.setImageResource(R.drawable.play_button);
            }
        });


        seekBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (mediaPlayer.isPlaying()) {
                    SeekBar sb = (SeekBar) view;
                    int playPositionInMillisecconds = (mediaFileLengthInMilliseconds / 100) * sb.getProgress();
                    mediaPlayer.seekTo(playPositionInMillisecconds);
                }
                return false;
            }
        });

        btn_play_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                    btn_play_pause.setImageResource(R.drawable.pause_button);

                } else {
                    mediaPlayer.pause();
                    btn_play_pause.setImageResource(R.drawable.play_button);
                }
                primarySeekBarProgressUpdater();
            }
        });
    }
}
