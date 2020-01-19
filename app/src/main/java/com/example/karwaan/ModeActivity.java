package com.example.karwaan;

import android.Manifest;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class ModeActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView toolbar_title, tv_offline;
    private LottieAnimationView lottie_animation_view;
    private RelativeLayout rl_saregama_mode, rl_manual_mode, rl_saregama_mode_offline, rl_manual_mode_offline;
    private ImageView bg;
    private Dialog loading_dialog;
    private Switch switch_offline;
    private Boolean isOfflineActivated;
    private String version = "0";
    private String link;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode);

        toolbar = (Toolbar) findViewById(R.id.toolBar);
        toolbar_title = (TextView) findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);
        toolbar_title.setText(getString(R.string.app_name));
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
        rl_saregama_mode_offline = findViewById(R.id.rl_saregama_mode_offline);
        rl_manual_mode_offline = findViewById(R.id.rl_manual_mode_offline);
        switch_offline = findViewById(R.id.switch_offline);
        tv_offline = findViewById(R.id.tv_offline);
        bg = findViewById(R.id.bg);

        Glide.with(this).load(R.drawable.bg).into(bg);

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
        requestStoragePermission();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (isNetworkConnected()) {
            checkForUpdates();
        }

        isOfflineActivated = getSharedPreferences("karvaanSharedPref", MODE_PRIVATE).getBoolean("isOfflineActivated", false);
        switch_offline.setChecked(isOfflineActivated);
        if (isOfflineActivated) {
            rl_saregama_mode_offline.setVisibility(View.VISIBLE);
            rl_manual_mode_offline.setVisibility(View.VISIBLE);
            rl_saregama_mode.setVisibility(View.GONE);
            rl_manual_mode.setVisibility(View.GONE);
            tv_offline.setVisibility(View.VISIBLE);
        } else {
            rl_saregama_mode_offline.setVisibility(View.GONE);
            rl_manual_mode_offline.setVisibility(View.GONE);
            rl_saregama_mode.setVisibility(View.VISIBLE);
            rl_manual_mode.setVisibility(View.VISIBLE);
            tv_offline.setVisibility(View.GONE);
        }

        switch_offline.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                getSharedPreferences("karvaanSharedPref", MODE_PRIVATE).edit().putBoolean("isOfflineActivated", b).commit();
                if (b) {
                    rl_saregama_mode_offline.setVisibility(View.VISIBLE);
                    rl_manual_mode_offline.setVisibility(View.VISIBLE);
                    rl_saregama_mode.setVisibility(View.GONE);
                    rl_manual_mode.setVisibility(View.GONE);
                    tv_offline.setVisibility(View.VISIBLE);
                } else {
                    rl_saregama_mode_offline.setVisibility(View.GONE);
                    rl_manual_mode_offline.setVisibility(View.GONE);
                    rl_saregama_mode.setVisibility(View.VISIBLE);
                    rl_manual_mode.setVisibility(View.VISIBLE);
                    tv_offline.setVisibility(View.GONE);
                }
            }
        });

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

        rl_saregama_mode_offline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ModeActivity.this, SaregamaOfflineActivity.class));
            }
        });

        rl_manual_mode_offline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ModeActivity.this, ManualOfflineActivity.class));
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

    private void checkForUpdates() {
        loading_dialog.show();
        //int verCode=0;
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
            //verCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (!version.equals("0")) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Update");
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String latestVersion = dataSnapshot.child("latestVersion").getValue(String.class);
                    if (latestVersion.equals(version)) {
                        loading_dialog.dismiss();
                    } else {
                        link = dataSnapshot.child("googleSiteLink").getValue(String.class);
                        loading_dialog.dismiss();
                        showUpdateDialog();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(ModeActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    loading_dialog.dismiss();
                }
            });
        } else {
            loading_dialog.dismiss();
        }
    }

    private void showUpdateDialog() {
        Dialog dialog_update = new Dialog(this);
        dialog_update.setContentView(R.layout.dialog_update);

        Button btn_update = dialog_update.findViewById(R.id.btn_update);
        btn_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!link.startsWith("http://") && !link.startsWith("https://")) {
                    link = "http://" + link;
                }
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(link));
                startActivity(i);
            }
        });

        dialog_update.setCanceledOnTouchOutside(false);
        dialog_update.setCancelable(false);
        dialog_update.show();
    }

    private void requestStoragePermission() {
        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        String rationale = "Please provide storage permission so that you can save songs for offline listening";
        Permissions.Options options = new Permissions.Options()
                .setRationaleDialogTitle("Storage Permission")
                .setSettingsDialogTitle("Warning");

        Permissions.check(this, permissions, rationale, options, new PermissionHandler() {
            @Override
            public void onGranted() {
                // do your task.
                getSharedPreferences("karvaanSharedPref", MODE_PRIVATE).edit().putBoolean("storagePermissionGranted", true).apply();
            }

            @Override
            public void onDenied(Context context, ArrayList<String> deniedPermissions) {
                // permission denied, block the feature.
                getSharedPreferences("karvaanSharedPref", MODE_PRIVATE).edit().putBoolean("storagePermissionGranted", false).apply();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                startActivity(new Intent(ModeActivity.this, SettingsActivity.class));
                break;

            case R.id.menu_about:
                startActivity(new Intent(ModeActivity.this, AboutActivity.class));
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
