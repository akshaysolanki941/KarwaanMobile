package com.example.karwaan.Adapters;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import com.example.karwaan.ManualActivity;
import com.example.karwaan.Models.SongModel;
import com.example.karwaan.R;

import java.util.ArrayList;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RVSongsAdapter extends RecyclerView.Adapter<RVSongsAdapter.ViewHolder> {

    private ArrayList<SongModel> songs;
    private Context context;
    private int lastPosition = 0;

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
        return new RVSongsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final RVSongsAdapter.ViewHolder holder, final int position) {

        SongModel song = songs.get(position);
        holder.tv_song_name.setText(song.getSongName());

        SpannableStringBuilder artists = new SpannableStringBuilder();
        ArrayList<String> artistList = song.getArtists();
        for (int i = 0; i < artistList.size(); i++) {
            String a = artistList.get(i);
            if (i == artistList.size() - 1) {
                artists.append(a);
            } else {
                artists.append(a).append(" | ");
            }
        }
        holder.tv_artist.setText(artists);
        holder.tv_artist.setSelected(true);

        setEnterAnimation(holder.itemView, position);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                setReduceSizeAnimation(holder.itemView);
                setRegainSizeAnimation(holder.itemView);

                ((ManualActivity) context).holderItemOnClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView tv_song_name, tv_artist;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);

            tv_song_name = (TextView) itemView.findViewById(R.id.tv_song_name);
            tv_artist = itemView.findViewById(R.id.tv_artist);

        }
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
