package com.example.karwaan;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ModeActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView toolbar_title;
    private Button btn_saregama_mode, btn_maunal_mode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode);

        toolbar = (Toolbar) findViewById(R.id.toolBar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);
        toolbar_title.setText("Karwaan");
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        btn_saregama_mode = (Button) findViewById(R.id.btn_saregama_mode);
        btn_maunal_mode = (Button) findViewById(R.id.btn_manual_mode);

    }

    @Override
    protected void onStart() {
        super.onStart();

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
