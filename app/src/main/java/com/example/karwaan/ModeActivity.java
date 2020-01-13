package com.example.karwaan;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class ModeActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView toolbar_title;
    private LottieAnimationView lottie_animation_view;
    private RelativeLayout rl_saregama_mode, rl_manual_mode;
    private ImageView bg;
    private Dialog loading_dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode);

        toolbar = (Toolbar) findViewById(R.id.toolBar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);
        toolbar_title.setText("Karwaan Mobile");
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        loading_dialog = new Dialog(this);
        loading_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loading_dialog.setContentView(R.layout.loading_dialog);
        lottie_animation_view = loading_dialog.findViewById(R.id.lottie_animation_view);
        lottie_animation_view.playAnimation();
        loading_dialog.setCanceledOnTouchOutside(false);
        loading_dialog.setCancelable(false);

        rl_saregama_mode = findViewById(R.id.rl_saregama_mode);
        rl_manual_mode = findViewById(R.id.rl_manual_mode);
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

        setUpFirebaseMessaging();
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

    private void setUpFirebaseMessaging() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("Messaging", "Messaging", NotificationManager.IMPORTANCE_DEFAULT);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        FirebaseMessaging.getInstance().subscribeToTopic("general")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = "Successful";
                        if (!task.isSuccessful()) {
                            msg = "Failed";
                        }
                    }
                });
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
