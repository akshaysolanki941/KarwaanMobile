package com.example.karwaan;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import androidx.appcompat.app.AppCompatActivity;
import believe.cht.fadeintextview.TextView;
import believe.cht.fadeintextview.TextViewListener;


public class SplashActivity extends AppCompatActivity {

    private ImageView bg, bg_alpha;
    TextView tv_app_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        tv_app_name = findViewById(R.id.tv_app_name);
        bg = findViewById(R.id.bg);
        bg_alpha = findViewById(R.id.bg_alpha);

        Glide.with(this).load(R.drawable.bg).into(bg);

        tv_app_name.setText("Karvaan\nMobile");
        tv_app_name.setLetterDuration(250);
        alphaAnimation(bg_alpha, 0, 1f, 3000);

        tv_app_name.setListener(new TextViewListener() {
            @Override
            public void onTextStart() {
            }

            @Override
            public void onTextFinish() {
                startActivity(new Intent(SplashActivity.this, ModeActivity.class));
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            }
        });
    }

    private void alphaAnimation(View view, float from, float to, int duration) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, "alpha", from, to);
        objectAnimator.setDuration(duration);
        objectAnimator.start();
    }
}
