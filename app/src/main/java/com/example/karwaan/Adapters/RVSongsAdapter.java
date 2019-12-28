package com.example.karwaan.Adapters;

import android.app.Dialog;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.karwaan.Models.SongModel;
import com.example.karwaan.R;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import static android.content.Context.MODE_PRIVATE;

public class RVSongsAdapter extends RecyclerView.Adapter<RVSongsAdapter.ViewHolder> {

    private ArrayList<SongModel> songs;
    private Context context;
    private SlidingUpPanelLayout slidingUpPanelLayout;
    private ImageButton btn_play_pause, btn_next_song, btn_prev_song;
    private SeekBar seekBar;
    private TextView tv_sliding_view_song_name, tv_current_time, tv_total_time;
    private ImageView loading_gif_imageView;
    private Dialog loading_dialog;

    private MediaPlayer mediaPlayer;
    private int mediaFileLengthInMilliseconds;
    private final Handler handler = new Handler();
    int index;

    public RVSongsAdapter() {
    }

    public RVSongsAdapter(ArrayList<SongModel> songs, Context context, MediaPlayer mediaPlayer, SlidingUpPanelLayout slidingUpPanelLayout,
                          ImageButton btn_play_pause, ImageButton btn_next_song, ImageButton btn_prev_song, SeekBar seekBar, TextView tv_sliding_view_song_name,
                          TextView tv_current_time, TextView tv_total_time) {
        this.songs = songs;
        this.context = context;
        this.mediaPlayer = mediaPlayer;
        this.slidingUpPanelLayout = slidingUpPanelLayout;
        this.btn_play_pause = btn_play_pause;
        this.btn_next_song = btn_next_song;
        this.btn_prev_song = btn_prev_song;
        this.seekBar = seekBar;
        this.tv_sliding_view_song_name = tv_sliding_view_song_name;
        this.tv_current_time = tv_current_time;
        this.tv_total_time = tv_total_time;
    }

    @NonNull
    @Override
    public RVSongsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.rv_songs_item, viewGroup, false);

        loading_dialog = new Dialog(context);
        loading_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loading_dialog.setContentView(R.layout.loading_dialog);
        loading_gif_imageView = (ImageView) loading_dialog.findViewById(R.id.loading_gif_imageView);
        Glide.with(context).load(R.drawable.loading).placeholder(R.drawable.loading).into(loading_gif_imageView);
        loading_dialog.setCanceledOnTouchOutside(false);
        loading_dialog.setCancelable(false);

        return new RVSongsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final RVSongsAdapter.ViewHolder holder, final int position) {

        final SongModel song = songs.get(position);

        holder.tv_song_name.setText(song.getSongName());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loading_dialog.show();
                index = position;

                ArrayList<String> artistList = song.getArtists();
                SpannableStringBuilder artists = new SpannableStringBuilder();

                for (int i = 0; i < artistList.size(); i++) {
                    String artist = artistList.get(i);
                    if (i == artistList.size() - 1) {
                        artists.append(artist);
                    } else {
                        artists.append(artist).append(" | ");
                    }
                }

                tv_sliding_view_song_name.setText(song.getSongName() + " - " + artists);
                tv_sliding_view_song_name.setSelected(true);
                setUpMediaPlayer(song.getUrl());
                slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            }
        });

        btn_next_song.setOnClickListener(v -> {
            loading_dialog.show();
            if (index + 1 >= songs.size()) {
                Toast.makeText(context, "This is the last song", Toast.LENGTH_SHORT).show();
            } else {
                index++;
                SongModel nextSong = songs.get(index);
                ArrayList<String> artistList = nextSong.getArtists();
                SpannableStringBuilder artists = new SpannableStringBuilder();

                for (int i = 0; i < artistList.size(); i++) {
                    String artist = artistList.get(i);
                    if (i == artistList.size() - 1) {
                        artists.append(artist);
                    } else {
                        artists.append(artist).append(" | ");
                    }
                }

                tv_sliding_view_song_name.setText(nextSong.getSongName() + " - " + artists);
                tv_sliding_view_song_name.setSelected(true);
                setUpMediaPlayer(nextSong.getUrl());
            }
        });

        btn_prev_song.setOnClickListener(v -> {
            loading_dialog.show();
            if (index - 1 < 0) {
                Toast.makeText(context, "This is the first song", Toast.LENGTH_SHORT).show();
            } else {
                index--;
                SongModel prevSong = songs.get(index);
                ArrayList<String> artistList = prevSong.getArtists();
                SpannableStringBuilder artists = new SpannableStringBuilder();

                for (int i = 0; i < artistList.size(); i++) {
                    String artist = artistList.get(i);
                    if (i == artistList.size() - 1) {
                        artists.append(artist);
                    } else {
                        artists.append(artist).append(" | ");
                    }
                }

                tv_sliding_view_song_name.setText(prevSong.getSongName() + " - " + artists);
                tv_sliding_view_song_name.setSelected(true);
                setUpMediaPlayer(prevSong.getUrl());
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
        if (!context.getSharedPreferences("released", MODE_PRIVATE).getBoolean("released", true))
            if (mediaPlayer.isPlaying()) {
                seekBar.setProgress((int) (((float) mediaPlayer.getCurrentPosition() / mediaFileLengthInMilliseconds) * 100));
                tv_current_time.setText(milliSecondsToTimer(mediaPlayer.getCurrentPosition()));
                Runnable notification = new Runnable() {
                    public void run() {
                        primarySeekBarProgressUpdater();
                    }
                };
                handler.postDelayed(notification, 1000);
            }
    }

    private void setUpMediaPlayer(final String song_url) {
        btn_play_pause.setImageResource(R.drawable.pause_btn_black);

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        } else if (!mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.reset();
        }

        try {
            mediaPlayer.setDataSource(song_url);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaFileLengthInMilliseconds = mediaPlayer.getDuration();
                tv_total_time.setText(milliSecondsToTimer(mediaFileLengthInMilliseconds));
                mediaPlayer.start();
                primarySeekBarProgressUpdater();
                loading_dialog.dismiss();
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
                btn_play_pause.setImageResource(R.drawable.play_btn_black);
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
                    btn_play_pause.setImageResource(R.drawable.pause_btn_black);

                } else {
                    mediaPlayer.pause();
                    btn_play_pause.setImageResource(R.drawable.play_btn_black);
                }
                primarySeekBarProgressUpdater();
            }
        });
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
}
