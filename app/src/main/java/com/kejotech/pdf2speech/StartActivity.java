package com.kejotech.pdf2speech;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;

import java.util.prefs.Preferences;

public class StartActivity extends AppCompatActivity {

    boolean isAndroidDone = false;
    Button cont;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Handle the splash screen transition.
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        cont = findViewById(R.id.button_continue);

        SharedPreferences sharedPreferences = getSharedPreferences("PREFERENCES",MODE_PRIVATE);
        String firstTime = sharedPreferences.getString("firstTimeInstall", "");

        // Set up an OnPreDrawListener to the root view.
        final View content = findViewById(android.R.id.content);
        content.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        // Check if the initial data is ready.
                        if (isAndroidDone) {
                            // The content is ready; start drawing.
                            content.getViewTreeObserver().removeOnPreDrawListener(this);
                            return true;
                        } else {
                            // The content is not ready; suspend.
                            dismissSplash();
                            return false;
                        }
                    }
                });

        if (firstTime.equals("yes")){
            Intent intent = new Intent(StartActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }else{
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("firstTimeInstall", "yes");
            editor.apply();
        }
        cont.setOnClickListener(v -> {
            Intent intent = new Intent(StartActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void dismissSplash() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isAndroidDone = true;
            }
        }, 3000);
    }
}