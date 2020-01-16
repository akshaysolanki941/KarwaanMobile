package com.example.karwaan;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import mehdi.sakout.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        simulateDayNight(0);

        View aboutPage = new AboutPage(this)
                .isRTL(false)
                .setImage(R.mipmap.ic_launcher)
                .addItem(new Element().setTitle("KARVAAN MOBILE v2.0").setGravity(Gravity.CENTER_HORIZONTAL))
                .setDescription("A simple app designed for those who love old bollywood classics. It has two simple modes one of which functions in the same manner as Saregama Carvaan speakers do. Its basically a Carvaan speaker but more portable and free, that too without annoying ads.")
                .addGroup("What's new")
                .addItem(new Element().setTitle("1. Some bugs fixed.\n\n2. App is completely changed but you won't see the changes as these changes are made in backend. You will feel these changes.\n\n3. My Playlist option added. Add your favorite songs to the playlist and play it from any of the two modes.\n\n4. Movie names of the songs added. Now you can search for songs of a particular movie.\n\n5. Seekbar changed in Manual Mode.\n\n6. Added a new option in Settings page to change behavior or action of a button.\n\n7. Added Equalizer in both modes.\n\n8. Added About section."))
                .addGroup("Connect with me")
                .addEmail("akshaysolanki941@gmail.com")
                .addWebsite("https://sites.google.com/view/karvaanmobile")
                .addFacebook("akshaysolanki941")
                .addTwitter("akshay_941")
                .addGitHub("akshaysolanki941")
                .addInstagram("akshay._.67")
                .create();

        setContentView(aboutPage);
    }

    void simulateDayNight(int currentSetting) {
        final int DAY = 0;
        final int NIGHT = 1;
        final int FOLLOW_SYSTEM = 3;

        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (currentSetting == DAY && currentNightMode != Configuration.UI_MODE_NIGHT_NO) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (currentSetting == NIGHT && currentNightMode != Configuration.UI_MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if (currentSetting == FOLLOW_SYSTEM) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }
}
