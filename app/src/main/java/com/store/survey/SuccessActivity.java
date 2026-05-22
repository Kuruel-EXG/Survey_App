package com.store.survey;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.example.app.R;

public class SuccessActivity extends AppCompatActivity {

    private Handler autoResetHandler;
    private Runnable autoResetRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);

        // Hide bars immediately on creation
        enableFullscreenKioskMode();

        View btnNewResponse = findViewById(R.id.btnNewResponse);
        View btnDone = findViewById(R.id.btnArrow);

        if (btnNewResponse != null) {
            btnNewResponse.setOnClickListener(v -> exitScreen());
        }
        if (btnDone != null) {
            btnDone.setOnClickListener(v -> exitScreen());
        }

        autoResetHandler = new Handler(Looper.getMainLooper());
        autoResetRunnable = this::exitScreen;
        autoResetHandler.postDelayed(autoResetRunnable, 4000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-apply fullscreen whenever this screen comes to the foreground
        enableFullscreenKioskMode();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // Force bars to stay hidden even if a dialog box opens or closes
        if (hasFocus) {
            enableFullscreenKioskMode();
        }
    }

    private void enableFullscreenKioskMode() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        if (controller != null) {
            // Hide both the status bar and the navigation bar
            controller.hide(WindowInsetsCompat.Type.systemBars());
            // Prevent the bars from permanently reappearing when swiped
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    private void exitScreen() {
        if (autoResetHandler != null && autoResetRunnable != null) {
            autoResetHandler.removeCallbacks(autoResetRunnable);
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        exitScreen();
    }
}