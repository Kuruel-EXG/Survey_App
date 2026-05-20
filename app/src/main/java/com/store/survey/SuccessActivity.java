package com.store.survey;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.app.R;

public class SuccessActivity extends AppCompatActivity {

    private Handler autoResetHandler;
    private Runnable autoResetRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);

        Button btnNewResponse = findViewById(R.id.btnNewResponse);

        // Explicit exit execution path
        btnNewResponse.setOnClickListener(v -> exitScreen());

        // Kiosk Auto-Reset Logic: Automatically returns to survey after 4 seconds
        autoResetHandler = new Handler(Looper.getMainLooper());
        autoResetRunnable = this::exitScreen;
        autoResetHandler.postDelayed(autoResetRunnable, 4000);
    }

    private void exitScreen() {
        // Remove pending delay callbacks to prevent duplicate thread triggers
        if (autoResetHandler != null && autoResetRunnable != null) {
            autoResetHandler.removeCallbacks(autoResetRunnable);
        }
        finish(); // Destroys this view and drops back to MainActivity
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        exitScreen();
    }
}