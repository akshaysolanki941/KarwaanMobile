package com.example.karwaan;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

public class ModeActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView toolbar_title;
    private Button btn_saregama_mode, btn_maunal_mode;
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

        btn_saregama_mode = (Button) findViewById(R.id.btn_saregama_mode);
        btn_maunal_mode = (Button) findViewById(R.id.btn_manual_mode);
        bg = findViewById(R.id.bg);

    }

    @Override
    protected void onStart() {
        super.onStart();

        Glide.with(this).load(R.drawable.bg).into(bg);

        btn_saregama_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ModeActivity.this, SaregamaActivity.class));
            }
        });

        btn_maunal_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ModeActivity.this, ManualActivity.class));
            }
        });
    }


}
