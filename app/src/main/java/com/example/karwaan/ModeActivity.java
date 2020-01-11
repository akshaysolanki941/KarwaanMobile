package com.example.karwaan;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class ModeActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView toolbar_title;
    private RelativeLayout rl_saregama_mode, rl_manual_mode, rl_radio_mode;
    private ImageView bg;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode);

        toolbar = (Toolbar) findViewById(R.id.toolBar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);
        toolbar_title.setText("Karwaan Mobile");
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        rl_saregama_mode = findViewById(R.id.rl_saregama_mode);
        rl_manual_mode = findViewById(R.id.rl_manual_mode);
        rl_radio_mode = findViewById(R.id.rl_radio_mode);
        bg = findViewById(R.id.bg);

        setReduceSizeAnimation(toolbar_title);
        alphaAnimation(toolbar, 0, 1f);
        setRegainSizeAnimation(toolbar_title);

        setReduceSizeAnimation(rl_saregama_mode);
        alphaAnimation(rl_saregama_mode, 0, 1f);
        setRegainSizeAnimation(rl_saregama_mode);

        setReduceSizeAnimation(rl_manual_mode);
        alphaAnimation(rl_manual_mode, 0, 1f);
        setRegainSizeAnimation(rl_manual_mode);

        setReduceSizeAnimation(rl_radio_mode);
        alphaAnimation(rl_radio_mode, 0, 1f);
        setRegainSizeAnimation(rl_radio_mode);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Glide.with(this).load(R.drawable.bg).into(bg);

        rl_saregama_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isNetworkConnected()) {
                    startActivity(new Intent(ModeActivity.this, SaregamaActivity.class));
                } else {
                    Toast.makeText(ModeActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
                }
            }
        });

        rl_manual_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isNetworkConnected()) {
                    startActivity(new Intent(ModeActivity.this, ManualActivity.class));
                } else {
                    Toast.makeText(ModeActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
                }
            }
        });

        rl_radio_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isNetworkConnected()) {
                    startActivity(new Intent(ModeActivity.this, RadioActivity.class));
                } else {
                    Toast.makeText(ModeActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                startActivity(new Intent(ModeActivity.this, SettingsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void alphaAnimation(View view, float from, float to) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, "alpha", from, to);
        objectAnimator.setDuration(1200);
        objectAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        objectAnimator.start();
    }

    private void setReduceSizeAnimation(View viewToAnimate) {
        AnimatorSet reducer = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.reduce_size);
        reducer.setTarget(viewToAnimate);
        reducer.start();
    }

    private void setRegainSizeAnimation(View viewToAnimate) {
        AnimatorSet regainer = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.regain_size);
        regainer.setTarget(viewToAnimate);
        regainer.start();
    }
}
