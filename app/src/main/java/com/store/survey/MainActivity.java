package com.store.survey;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.app.R;
import com.store.survey.data.SurveyDatabase;
import com.store.survey.data.SurveyResponse;
import com.store.survey.network.ApiClient;
import com.store.survey.network.ApiService;
import com.store.survey.network.SheetDbRequest;

import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private LinearLayout btnGood, btnNeutral, btnBad;
    private Button btnSubmit;
    private FrameLayout storeBadge;

    private String selectedRating = "";
    private SurveyDatabase db;

    // Timer Loop Fields
    private Handler syncHandler;
    private Runnable syncRunnable;
    private static final long SYNC_INTERVAL = 10800000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = SurveyDatabase.getDatabase(this);

        storeBadge = findViewById(R.id.storeBadge);
        btnGood = findViewById(R.id.btnGood);
        btnNeutral = findViewById(R.id.btnNeutral);
        btnBad = findViewById(R.id.btnBad);
        btnSubmit = findViewById(R.id.btnSubmit);

        btnGood.setOnClickListener(v -> selectRating("Good", btnGood, btnNeutral, btnBad));
        btnNeutral.setOnClickListener(v -> selectRating("Needs improvement", btnNeutral, btnGood, btnBad));
        btnBad.setOnClickListener(v -> selectRating("Bad", btnBad, btnGood, btnNeutral));

        btnSubmit.setOnClickListener(v -> submitFeedback());

        storeBadge.setOnLongClickListener(v -> {
            showPinDialog();
            return true;
        });

        startAutomationLoop();
    }

    private void startAutomationLoop() {
        syncHandler = new Handler(Looper.getMainLooper());
        syncRunnable = new Runnable() {
            @Override
            public void run() {
                Executors.newSingleThreadExecutor().execute(() -> {
                    triggerAutomaticCloudSync();
                });

                syncHandler.postDelayed(this, SYNC_INTERVAL);
            }
        };

        syncHandler.postDelayed(syncRunnable, 15000);
    }

    private void triggerAutomaticCloudSync() {
        List<SurveyResponse> offlineResponses = db.responseDao().getAllResponses();
        if (offlineResponses.isEmpty()) return;

        SheetDbRequest requestPayload = new SheetDbRequest(offlineResponses);

        ApiClient.getClient().create(ApiService.class)
                .submitResponses(Config.CLOUD_ENDPOINT, requestPayload)
                .enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                        if (response.isSuccessful()) {
                            Executors.newSingleThreadExecutor().execute(() -> {
                                db.responseDao().deleteAll();
                            });
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                    }
                });
    }

    private void selectRating(String rating, LinearLayout selected, LinearLayout unselected1, LinearLayout unselected2) {
        selectedRating = rating;
        selected.setSelected(true);
        unselected1.setSelected(false);
        unselected2.setSelected(false);
    }

    private void submitFeedback() {
        if (selectedRating.isEmpty()) {
            Toast.makeText(this, "Please select an option before submitting", Toast.LENGTH_SHORT).show();
            return;
        }

        SurveyResponse response = new SurveyResponse();
        response.deviceId = Config.DEVICE_ID;
        response.timestamp = System.currentTimeMillis();
        response.rating = selectedRating;
        response.shift = "Standard";

        Executors.newSingleThreadExecutor().execute(() -> {
            db.responseDao().insert(response);

            runOnUiThread(() -> {
                resetSurveyForm();
                Intent intent = new Intent(MainActivity.this, SuccessActivity.class);
                startActivity(intent);
            });
        });
    }

    private void resetSurveyForm() {
        selectedRating = "";
        btnGood.setSelected(false);
        btnNeutral.setSelected(false);
        btnBad.setSelected(false);
    }

    private void showPinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Staff Access Required");
        builder.setMessage("Enter dashboard security PIN:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String enteredPin = input.getText().toString();
            if (enteredPin.equals(Config.DASHBOARD_PIN)) {
                Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Invalid PIN. Access Denied.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (syncHandler != null && syncRunnable != null) {
            syncHandler.removeCallbacks(syncRunnable);
        }
    }
}