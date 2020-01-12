package com.example.karwaan;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SettingsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView toolbar_title, tv_open_source_licenses, tv_update;
    private Switch switch_enable_voice_mode, switch_change_10;
    private ImageView bg;
    private ImageButton btn_help_voice, btn_help_10;
    private Dialog dialog_voice_help;
    private static final int RECORD_AUDIO_REQUEST_CODE = 5486;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        toolbar = (Toolbar) findViewById(R.id.toolBar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);
        toolbar_title.setText("Settings");
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        bg = findViewById(R.id.bg);
        tv_open_source_licenses = findViewById(R.id.tv_open_source_licenses);
        tv_update = findViewById(R.id.tv_update);
        tv_update.setVisibility(View.GONE);
        btn_help_voice = findViewById(R.id.btn_help_voice);
        btn_help_10 = findViewById(R.id.btn_help_10);
        dialog_voice_help = new Dialog(this);

        switch_enable_voice_mode = findViewById(R.id.switch_enable_voice_mode);
        switch_change_10 = findViewById(R.id.switch_change_10);
        Boolean voiceModeEnable = getSharedPreferences("voiceModeEnabled", MODE_PRIVATE).getBoolean("voiceModeEnabled", false);
        Boolean skip10SongsEnabled = getSharedPreferences("skip10SongsEnabled", MODE_PRIVATE).getBoolean("skip10SongsEnabled", false);
        if (voiceModeEnable) {
            setMargins(switch_enable_voice_mode, 0, 20, 20, 20);
            switch_enable_voice_mode.setChecked(true);
            btn_help_voice.setVisibility(View.VISIBLE);
        } else {
            setMargins(switch_enable_voice_mode, 20, 20, 20, 20);
            switch_enable_voice_mode.setChecked(false);
            btn_help_voice.setVisibility(View.GONE);
        }

        if (skip10SongsEnabled) {
            setMargins(switch_change_10, 0, 20, 20, 20);
            switch_change_10.setChecked(true);
            btn_help_10.setVisibility(View.VISIBLE);
        } else {
            setMargins(switch_change_10, 20, 20, 20, 20);
            switch_change_10.setChecked(false);
            btn_help_10.setVisibility(View.GONE);
        }

        checkForUpdates();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Glide.with(this).load(R.drawable.bg).into(bg);

        switch_enable_voice_mode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    Boolean firstTimeEnable = getSharedPreferences("firstTimeEnable", MODE_PRIVATE).getBoolean("firstTimeEnable", true);
                    if (firstTimeEnable) {
                        checkVoiceCommandPermission();
                        getSharedPreferences("firstTimeEnable", MODE_PRIVATE).edit().putBoolean("firstTimeEnable", false).commit();
                    } else {
                        setMargins(switch_enable_voice_mode, 0, 20, 20, 20);
                        btn_help_voice.setVisibility(View.VISIBLE);
                        getSharedPreferences("voiceModeEnabled", MODE_PRIVATE).edit().putBoolean("voiceModeEnabled", true).commit();
                        Toast.makeText(SettingsActivity.this, "Voice Mode Enabled", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    setMargins(switch_enable_voice_mode, 20, 20, 20, 20);
                    btn_help_voice.setVisibility(View.GONE);
                    getSharedPreferences("voiceModeEnabled", MODE_PRIVATE).edit().putBoolean("voiceModeEnabled", false).commit();
                    Toast.makeText(SettingsActivity.this, "Voice Mode Disabled", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btn_help_voice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog_voice_help.setContentView(R.layout.dialog_voice_help);
                dialog_voice_help.setCanceledOnTouchOutside(true);
                dialog_voice_help.setCancelable(true);
                dialog_voice_help.show();
            }
        });

        switch_change_10.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    setMargins(switch_change_10, 0, 20, 20, 20);
                    btn_help_10.setVisibility(View.VISIBLE);
                    getSharedPreferences("skip10SongsEnabled", MODE_PRIVATE).edit().putBoolean("skip10SongsEnabled", true).commit();
                    Toast.makeText(SettingsActivity.this, "10 songs skipping enabled", Toast.LENGTH_SHORT).show();
                } else {
                    setMargins(switch_change_10, 20, 20, 20, 20);
                    btn_help_10.setVisibility(View.GONE);
                    getSharedPreferences("skip10SongsEnabled", MODE_PRIVATE).edit().putBoolean("skip10SongsEnabled", false).commit();
                    Toast.makeText(SettingsActivity.this, "10 songs skipping disabled", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btn_help_10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog_voice_help.setContentView(R.layout.dialog_skip_10_help);

                TextView tv_dialog_10_help = dialog_voice_help.findViewById(R.id.tv_dialog_10_help);
                SpannableString spannableString = new SpannableString(getString(R.string.skip_10_help));
                Drawable d = getResources().getDrawable(R.drawable.forward_10_black);
                d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                ImageSpan span = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
                spannableString.setSpan(span, spannableString.toString().indexOf("@"), spannableString.toString().indexOf("@") + 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                tv_dialog_10_help.setText(spannableString);

                dialog_voice_help.setCanceledOnTouchOutside(true);
                dialog_voice_help.setCancelable(true);
                dialog_voice_help.show();
            }
        });

        tv_open_source_licenses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayLicensesAlertDialog();
            }
        });
    }

    private void checkVoiceCommandPermission() {
        Toast.makeText(this, "Give the microphone permission", Toast.LENGTH_LONG).show();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            //When permission is not granted by user, show them message why this permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "Please grant permissions to record audio", Toast.LENGTH_LONG).show();

                //Give user option to still opt-in the permissions
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_REQUEST_CODE);

            } else {
                // Show user dialog to grant permission to record audio
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_REQUEST_CODE);
            }
        }
    }

    private void setMargins(View view, int left, int top, int right, int bottom) {
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            p.setMargins(left, top, right, bottom);
            view.requestLayout();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case RECORD_AUDIO_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    setMargins(switch_enable_voice_mode, 0, 20, 20, 20);
                    btn_help_voice.setVisibility(View.VISIBLE);
                    getSharedPreferences("voiceModeEnabled", MODE_PRIVATE).edit().putBoolean("voiceModeEnabled", true).commit();
                    Toast.makeText(SettingsActivity.this, "Voice Mode Enabled", Toast.LENGTH_SHORT).show();
                } else {
                    // permission denied, boo! Disable the functionality that depends on this permission.
                    Toast.makeText(this, "Permissions Denied to record audio", Toast.LENGTH_LONG).show();
                    switch_enable_voice_mode.setChecked(false);
                    setMargins(switch_enable_voice_mode, 20, 20, 20, 20);
                    btn_help_voice.setVisibility(View.GONE);
                    getSharedPreferences("voiceModeEnabled", MODE_PRIVATE).edit().putBoolean("voiceModeEnabled", false).commit();
                    Toast.makeText(SettingsActivity.this, "Voice Mode Disabled", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }


    private void displayLicensesAlertDialog() {
        WebView view = (WebView) LayoutInflater.from(this).inflate(R.layout.dialog_licenses, null);
        view.loadUrl("file:///android_asset/open_source_licenses.html");
        new AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog_Alert)
                .setTitle("Open Source Licenses")
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void checkForUpdates() {
        DatabaseReference updateRef = FirebaseDatabase.getInstance().getReference("Update");
        updateRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String available = dataSnapshot.child("available").getValue(String.class);
                if (!available.isEmpty()) {
                    if (available.equals("yes")) {
                        tv_update.setVisibility(View.VISIBLE);
                        tv_update.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                String link = dataSnapshot.child("googleSiteLink").getValue(String.class);
                                if (!link.startsWith("http://") && !link.startsWith("https://")) {
                                    link = "http://" + link;
                                }
                                Intent i = new Intent(Intent.ACTION_VIEW);
                                i.setData(Uri.parse(link));
                                startActivity(i);
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(SettingsActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
