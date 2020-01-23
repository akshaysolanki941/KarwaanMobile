package com.akshay.karwaan;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import java.util.ArrayList;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SettingsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView toolbar_title, tv_open_source_licenses;
    private Switch switch_enable_voice_mode, switch_change_10;
    private ImageView bg;
    private ImageButton btn_help_voice, btn_help_10;
    private Dialog dialog_voice_help;
    private EditText et_suggestion_bug;
    private Button btn_sugggestion_bug_submit;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        toolbar = findViewById(R.id.toolBar);
        toolbar_title = findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);
        toolbar_title.setText(getString(R.string.settings_toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        bg = findViewById(R.id.bg);
        Glide.with(this).load(R.drawable.bg).into(bg);

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        tv_open_source_licenses = findViewById(R.id.tv_open_source_licenses);
        btn_help_voice = findViewById(R.id.btn_help_voice);
        btn_help_10 = findViewById(R.id.btn_help_10);
        et_suggestion_bug = findViewById(R.id.et_suggestion_bug);
        btn_sugggestion_bug_submit = findViewById(R.id.btn_sugggestion_bug_submit);
        dialog_voice_help = new Dialog(this);

        switch_enable_voice_mode = findViewById(R.id.switch_enable_voice_mode);
        switch_change_10 = findViewById(R.id.switch_change_10);
        boolean voiceModeEnable = getSharedPreferences("karvaanSharedPref", MODE_PRIVATE).getBoolean("voiceModeEnabled", false);
        boolean skip10SongsEnabled = getSharedPreferences("karvaanSharedPref", MODE_PRIVATE).getBoolean("skip10SongsEnabled", false);
        if (voiceModeEnable) {
            switch_enable_voice_mode.setChecked(true);
            btn_help_voice.setVisibility(View.VISIBLE);
        } else {
            switch_enable_voice_mode.setChecked(false);
            btn_help_voice.setVisibility(View.GONE);
        }

        if (skip10SongsEnabled) {
            switch_change_10.setChecked(true);
            btn_help_10.setVisibility(View.VISIBLE);
        } else {
            switch_change_10.setChecked(false);
            btn_help_10.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        switch_enable_voice_mode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    checkVoiceCommandPermission();
                } else {
                    btn_help_voice.setVisibility(View.GONE);
                    getSharedPreferences("karvaanSharedPref", MODE_PRIVATE).edit().putBoolean("voiceModeEnabled", false).apply();
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
                    btn_help_10.setVisibility(View.VISIBLE);
                    getSharedPreferences("karvaanSharedPref", MODE_PRIVATE).edit().putBoolean("skip10SongsEnabled", true).apply();
                    Toast.makeText(SettingsActivity.this, "10 songs skipping enabled", Toast.LENGTH_SHORT).show();
                } else {
                    btn_help_10.setVisibility(View.GONE);
                    getSharedPreferences("karvaanSharedPref", MODE_PRIVATE).edit().putBoolean("skip10SongsEnabled", false).apply();
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

        btn_sugggestion_bug_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isNetworkConnected()) {
                    String text = et_suggestion_bug.getText().toString();
                    if (!text.isEmpty()) {
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Suggestions_Bugs");
                        ref.push().child("response").setValue(text);
                        et_suggestion_bug.setText("");
                        et_suggestion_bug.clearFocus();
                        Toast.makeText(SettingsActivity.this, "Submitted, THANK YOU", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SettingsActivity.this, "Type Something....", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SettingsActivity.this, "No Internet", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void checkVoiceCommandPermission() {
        String[] permissions = {Manifest.permission.RECORD_AUDIO};
        String rationale = "Please provide record audio permission to activate voice mode";
        Permissions.Options options = new Permissions.Options()
                .setRationaleDialogTitle("Record Audio Permission")
                .setSettingsDialogTitle("Warning");

        Permissions.check(this, permissions, rationale, options, new PermissionHandler() {
            @Override
            public void onGranted() {
                // do your task.
                btn_help_voice.setVisibility(View.VISIBLE);
                getSharedPreferences("karvaanSharedPref", MODE_PRIVATE).edit().putBoolean("voiceModeEnabled", true).apply();
                Toast.makeText(SettingsActivity.this, "Voice Mode Enabled", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDenied(Context context, ArrayList<String> deniedPermissions) {
                // permission denied, block the feature.
                switch_enable_voice_mode.setChecked(false);
                btn_help_voice.setVisibility(View.GONE);
                getSharedPreferences("karvaanSharedPref", MODE_PRIVATE).edit().putBoolean("voiceModeEnabled", false).apply();
                Toast.makeText(SettingsActivity.this, "Voice Mode Disabled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
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

   /* private void checkForUpdates() {
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
    }*/


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
