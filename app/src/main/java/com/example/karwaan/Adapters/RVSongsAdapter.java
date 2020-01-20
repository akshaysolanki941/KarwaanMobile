package com.example.karwaan.Adapters;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.downloader.Error;
import com.downloader.OnDownloadListener;
import com.downloader.OnProgressListener;
import com.downloader.PRDownloader;
import com.downloader.Progress;
import com.example.karwaan.Models.SongModel;
import com.example.karwaan.R;
import com.example.karwaan.Utils.EncryptDecryptUtils;
import com.example.karwaan.Utils.FilesUtil;
import com.example.karwaan.Utils.TinyDB;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import static android.content.Context.MODE_PRIVATE;

public class RVSongsAdapter extends RecyclerView.Adapter<RVSongsAdapter.ViewHolder> {

    private ArrayList<SongModel> songs;
    private Context context;
    private int lastPosition = 0;
    private TinyDB tinyDB;
    private ArrayList<Object> downloadedSongList = new ArrayList<>();
    private Drawable.ConstantState checkConstState, downloadConstState;
    private Boolean isStoragePermissionGranted;

    public RVSongsAdapter() {
    }

    public RVSongsAdapter(ArrayList<SongModel> songs, Context context) {
        this.context = context;
        this.songs = songs;
    }

    @NonNull
    @Override
    public RVSongsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.rv_songs_item, viewGroup, false);

        tinyDB = new TinyDB(context);
        downloadedSongList.clear();
        downloadedSongList = tinyDB.getListObject("downloadedSongList", SongModel.class);

        checkConstState = context.getDrawable(R.drawable.ic_check_black_24dp).getConstantState();
        downloadConstState = context.getDrawable(R.drawable.ic_file_download_black_24dp).getConstantState();

        isStoragePermissionGranted = context.getSharedPreferences("karvaanSharedPref", MODE_PRIVATE).getBoolean("storagePermissionGranted", false);

        return new RVSongsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RVSongsAdapter.ViewHolder holder, int position) {

        holder.pb_download.setVisibility(View.GONE);

        SongModel song = songs.get(position);
        holder.tv_song_name.setText(song.getSongName());

        File f = new File(FilesUtil.getFilePath(context, song.getSongName()));
        if (f.exists()) {
            holder.img_download.setImageResource(R.drawable.ic_check_black_24dp);
        } else {
            holder.img_download.setImageResource(R.drawable.ic_file_download_black_24dp);
        }

        SpannableStringBuilder artists = new SpannableStringBuilder();
        ArrayList<String> artistList = song.getArtists();
        for (int i = 0; i < artistList.size(); i++) {
            String a = artistList.get(i);
            if (i == artistList.size() - 1) {
                artists.append(a);
            } else {
                artists.append(a).append(", ");
            }
        }
        holder.tv_artist.setText(song.getMovie().concat(" | ").concat(String.valueOf(artists)));
        holder.tv_artist.setSelected(true);

        setEnterAnimation(holder.itemView, position);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setReduceSizeAnimation(holder.itemView);
                setRegainSizeAnimation(holder.itemView);

                Intent i = new Intent("holderItemOnClick");
                i.putExtra("holderItemOnClick", position);
                LocalBroadcastManager.getInstance(context).sendBroadcast(i);
            }
        });

        holder.img_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (holder.img_download.getDrawable().getConstantState().equals(downloadConstState)) {
                    if (isStoragePermissionGranted) {
                        holder.pb_download.setVisibility(View.VISIBLE);
                        holder.img_download.setVisibility(View.GONE);
                        PRDownloader.download(song.getUrl(), FilesUtil.getDirPath(context),
                                song.getSongName()).build().setOnProgressListener(new OnProgressListener() {
                            @Override
                            public void onProgress(Progress progress) {
                                holder.pb_download.setProgressMax(progress.totalBytes);
                                holder.pb_download.setProgress(progress.currentBytes);
                            }
                        }).start(new OnDownloadListener() {
                            @Override
                            public void onDownloadComplete() {
                                if (encrypt(song)) {
                                    holder.pb_download.setVisibility(View.GONE);
                                    holder.img_download.setImageResource(R.drawable.ic_check_black_24dp);
                                    holder.img_download.setVisibility(View.VISIBLE);
                                    downloadedSongList.add(song);
                                    tinyDB.putListObject("downloadedSongList", downloadedSongList);
                                    Toast.makeText(context, "Downloaded " + song.getSongName(), Toast.LENGTH_SHORT).show();

                                } else {
                                    holder.pb_download.setVisibility(View.GONE);
                                    holder.img_download.setVisibility(View.VISIBLE);
                                }
                            }

                            @Override
                            public void onError(Error error) {
                                Toast.makeText(context, error.toString(), Toast.LENGTH_SHORT).show();
                                holder.pb_download.setVisibility(View.GONE);
                                holder.img_download.setVisibility(View.VISIBLE);
                            }


                        });
                    } else {
                        Toast.makeText(context, "Please grant storage permission first", Toast.LENGTH_LONG).show();
                    }
                } else if (holder.img_download.getDrawable().getConstantState().equals(checkConstState)) {
                    Toast.makeText(context, "Already downloaded", Toast.LENGTH_SHORT).show();
                }
            }
        });

        holder.pb_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(context, "Downloading....", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView tv_song_name, tv_artist;
        public RelativeLayout foreground, background;
        ImageView img_download;
        CircularProgressBar pb_download;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);

            tv_song_name = (TextView) itemView.findViewById(R.id.tv_song_name);
            tv_artist = itemView.findViewById(R.id.tv_artist);
            background = (RelativeLayout) itemView.findViewById(R.id.background);
            foreground = (RelativeLayout) itemView.findViewById(R.id.foreground);
            img_download = itemView.findViewById(R.id.img_download);
            pb_download = itemView.findViewById(R.id.pb_download);

        }
    }

    private boolean encrypt(SongModel song) {
        try {
            byte[] fileData = FilesUtil.readFile(FilesUtil.getFilePath(context, song.getSongName()));
            byte[] encodedBytes = EncryptDecryptUtils.encode(EncryptDecryptUtils.getInstance(context).getSecretKey(), fileData);
            FilesUtil.saveFile(encodedBytes, FilesUtil.getFilePath(context, song.getSongName()));
            return true;
        } catch (Exception e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void setEnterAnimation(View viewToAnimate, int position) {
        // If the bound view wasn't previously displayed on screen, it's animated
        //if (position > lastPosition) {
        AlphaAnimation anim = new AlphaAnimation(0.0f, 1f);
        anim.setDuration(new Random().nextInt(501 - 51) + 51);//to make duration random number between [0,501)
        viewToAnimate.startAnimation(anim);
        //lastPosition = position;
        //}
    }

    private void setReduceSizeAnimation(View viewToAnimate) {
        AnimatorSet reducer = (AnimatorSet) AnimatorInflater.loadAnimator(context, R.animator.reduce_size);
        reducer.setTarget(viewToAnimate);
        reducer.start();
    }

    private void setRegainSizeAnimation(View viewToAnimate) {
        AnimatorSet regainer = (AnimatorSet) AnimatorInflater.loadAnimator(context, R.animator.regain_size);
        regainer.setTarget(viewToAnimate);
        regainer.start();
    }
}
