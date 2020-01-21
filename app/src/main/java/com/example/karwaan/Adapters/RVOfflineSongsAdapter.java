package com.example.karwaan.Adapters;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;
import android.widget.Toast;

import com.example.karwaan.ManualOfflineActivity;
import com.example.karwaan.Models.SongModel;
import com.example.karwaan.R;
import com.example.karwaan.Utils.FilesUtil;
import com.example.karwaan.Utils.TinyDB;

import java.util.ArrayList;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

public class RVOfflineSongsAdapter extends RecyclerView.Adapter<RVOfflineSongsAdapter.ViewHolder> {

    private ArrayList<Object> offlineSongList;
    private ArrayList<Object> list = new ArrayList<>();
    private Context context;
    private int lastPosition = 0;
    private TinyDB tinyDB;

    public RVOfflineSongsAdapter() {
    }

    public RVOfflineSongsAdapter(ArrayList<Object> offlineSongList, Context context) {
        this.context = context;
        this.offlineSongList = offlineSongList;
    }

    @NonNull
    @Override
    public RVOfflineSongsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.rv_offline_songs_item, viewGroup, false);

        tinyDB = new TinyDB(context);
        list.clear();
        list = tinyDB.getListObject("downloadedSongList", SongModel.class);

        return new RVOfflineSongsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RVOfflineSongsAdapter.ViewHolder holder, final int position) {

        SongModel song = (SongModel) offlineSongList.get(position);
        holder.tv_song_name_offline.setText(song.getSongName());

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
        holder.tv_artist_offline.setText(song.getMovie().concat(" | ").concat(String.valueOf(artists)));
        holder.tv_artist_offline.setSelected(true);

        setEnterAnimation(holder.itemView, position);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setReduceSizeAnimation(holder.itemView);
                setRegainSizeAnimation(holder.itemView);

                // ((ManualActivity) context).holderItemOnClick(position);
                Intent i = new Intent("holderItemOnClick");
                i.putExtra("holderItemOnClick", position);
                LocalBroadcastManager.getInstance(context).sendBroadcast(i);
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Dialog dialog_delete = new Dialog(context);
                dialog_delete.setContentView(R.layout.dialog_delete);
                TextView tv_dialog_delete_offline_song = dialog_delete.findViewById(R.id.tv_dialog_delete_offline_song);
                tv_dialog_delete_offline_song.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (FilesUtil.deleteDownloadedFile(context, song.getSongName())) {
                            removeItem(position);
                            ((ManualOfflineActivity) context).getTotalSize();
                            Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Some error occured", Toast.LENGTH_SHORT).show();
                        }
                        dialog_delete.dismiss();
                    }
                });
                dialog_delete.setCancelable(true);
                dialog_delete.setCanceledOnTouchOutside(true);
                dialog_delete.show();
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return offlineSongList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView tv_song_name_offline, tv_artist_offline;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);

            tv_song_name_offline = (TextView) itemView.findViewById(R.id.tv_song_name);
            tv_artist_offline = itemView.findViewById(R.id.tv_artist);

        }
    }

    public void removeItem(int position) {
        list.remove(position);
        tinyDB.putListObject("downloadedSongList", list);
        offlineSongList.remove(position);
        notifyItemRemoved(position);
        notifyDataSetChanged();
        Intent i = new Intent("itemDeletedUpdateList");
        LocalBroadcastManager.getInstance(context).sendBroadcast(i);
        ((ManualOfflineActivity) context).setTotalSongsCount();
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

