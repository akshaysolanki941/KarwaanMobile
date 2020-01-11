package com.example.karwaan.Adapters;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import com.example.karwaan.Models.RadioModel;
import com.example.karwaan.R;
import com.example.karwaan.RadioActivity;

import java.util.ArrayList;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RVRadioAdapter extends RecyclerView.Adapter<RVRadioAdapter.ViewHolder> {

    private ArrayList<RadioModel> radioList;
    private Context context;

    public RVRadioAdapter() {
    }

    public RVRadioAdapter(ArrayList<RadioModel> radioList, Context context) {
        this.radioList = radioList;
        this.context = context;
    }

    @NonNull
    @Override
    public RVRadioAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.rv_radio_item, viewGroup, false);
        return new RVRadioAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RVRadioAdapter.ViewHolder holder, int position) {

        RadioModel radio = radioList.get(position);

        setEnterAnimation(holder.itemView, position);

        holder.tv_radio_name.setText(radio.getRadioName());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setReduceSizeAnimation(holder.itemView);
                setRegainSizeAnimation(holder.itemView);

                ((RadioActivity) context).holderItemOnClick(position);
            }
        });

    }

    @Override
    public int getItemCount() {
        return radioList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView tv_radio_name;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);

            tv_radio_name = (TextView) itemView.findViewById(R.id.tv_radio_name);

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
