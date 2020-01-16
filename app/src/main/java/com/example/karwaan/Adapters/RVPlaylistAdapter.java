package com.example.karwaan.Adapters;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.content.Intent;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.karwaan.Models.SongModel;
import com.example.karwaan.R;

import java.util.ArrayList;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

public class RVPlaylistAdapter extends RecyclerView.Adapter<RVPlaylistAdapter.ViewHolder> {

    private ArrayList<SongModel> songsPlaylist;
    private Context context;
    private int lastPosition = 0;

    public RVPlaylistAdapter() {
    }

    public RVPlaylistAdapter(ArrayList<SongModel> songsPlaylist, Context context) {
        this.context = context;
        this.songsPlaylist = songsPlaylist;
    }

    @NonNull
    @Override
    public RVPlaylistAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.rv_playlist_item, viewGroup, false);
        return new RVPlaylistAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RVPlaylistAdapter.ViewHolder holder, int position) {

        SongModel song = songsPlaylist.get(position);
        holder.tv_song_name_playlist.setText(song.getSongName());

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
        holder.tv_artist_playlist.setText(song.getMovie().concat(" | ").concat(String.valueOf(artists)));
        holder.tv_artist_playlist.setSelected(true);

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
    }

    @Override
    public int getItemCount() {
        return songsPlaylist.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView tv_song_name_playlist, tv_artist_playlist;
        public RelativeLayout foreground, background;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);

            tv_song_name_playlist = (TextView) itemView.findViewById(R.id.tv_song_name_playlist);
            tv_artist_playlist = itemView.findViewById(R.id.tv_artist_playlist);
            background = (RelativeLayout) itemView.findViewById(R.id.background);
            foreground = (RelativeLayout) itemView.findViewById(R.id.foreground);

        }
    }

    public void removeItem(int position) {
        songsPlaylist.remove(position);
        notifyItemRemoved(position);
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
