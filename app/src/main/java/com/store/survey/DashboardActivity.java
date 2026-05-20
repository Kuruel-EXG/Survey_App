package com.store.survey;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.app.R;
import com.store.survey.data.SurveyDatabase;
import com.store.survey.data.SurveyResponse;
import com.store.survey.network.ApiClient;
import com.store.survey.network.ApiService;
import com.store.survey.network.SheetDbRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity {

    private TextView textTotalResponses, textStatsBreakdown;
    private Button btnSync, btnSendReport, btnExportCsv;
    private ListView listViewResponses;
    private SurveyDatabase db;
    private List<SurveyResponse> allResponses = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        db = SurveyDatabase.getDatabase(this);

        textTotalResponses = findViewById(R.id.textTotalResponses);
        textStatsBreakdown = findViewById(R.id.textStatsBreakdown);
        btnSync = findViewById(R.id.btnSync);
        btnSendReport = findViewById(R.id.btnSendReport);
        btnExportCsv = findViewById(R.id.btnExportCsv);
        listViewResponses = findViewById(R.id.listViewResponses);

        btnSync.setOnClickListener(v -> syncDataToCloud());
        btnSendReport.setOnClickListener(v -> triggerManualEmailReport());
        btnExportCsv.setOnClickListener(v -> exportResponsesToCsv());

        loadDashboardData();
    }

    private void loadDashboardData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            allResponses = db.responseDao().getAllResponses();

            int total = allResponses.size();
            int goodCount = 0;
            int neutralCount = 0;
            int badCount = 0;

            // Calculate percentage blocks
            for (SurveyResponse response : allResponses) {
                if ("Good".equalsIgnoreCase(response.rating)) goodCount++;
                else if ("Needs improvement".equalsIgnoreCase(response.rating)) neutralCount++;
                else if ("Bad".equalsIgnoreCase(response.rating)) badCount++;
            }

            final int finalTotal = total;
            final int gPct = total > 0 ? (goodCount * 100) / total : 0;
            final int nPct = total > 0 ? (neutralCount * 100) / total : 0;
            final int bPct = total > 0 ? (badCount * 100) / total : 0;

            // Prepare list entries for the last 20 items
            List<String> logDisplayList = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            int limit = Math.min(total, 20);

            for (int i = 0; i < limit; i++) {
                SurveyResponse res = allResponses.get(i);
                String dateStr = sdf.format(new Date(res.timestamp));
                logDisplayList.add(String.format("[%s] %s (Shift: %s)", dateStr, res.rating, res.shift));
            }

            runOnUiThread(() -> {
                textTotalResponses.setText("Total Responses: " + finalTotal);
                textStatsBreakdown.setText(String.format("Good: %d%%  |  Needs Improvement: %d%%  |  Bad: %d%%", gPct, nPct, bPct));

                ArrayAdapter<String> adapter = new ArrayAdapter<>(DashboardActivity.this,
                        android.R.layout.simple_list_item_1, logDisplayList);
                listViewResponses.setAdapter(adapter);
            });
        });
    }

    private void syncDataToCloud() {
        if (allResponses.isEmpty()) {
            Toast.makeText(this, "No data available to sync.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSync.setEnabled(false);
        btnSync.setText("SYNCING...");

        SheetDbRequest requestPayload = new SheetDbRequest(allResponses);
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.submitResponses(Config.CLOUD_ENDPOINT, requestPayload).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(DashboardActivity.this, "Sync Successful!", Toast.LENGTH_SHORT).show();
                    Executors.newSingleThreadExecutor().execute(() -> {
                        db.responseDao().deleteAll();
                        loadDashboardData();
                    });
                } else {
                    // This replaces the generic "Server error" message with the exact HTTP code from SheetDB
                    String detailedError = "Sync Failed: Code " + response.code() + " (" + response.message() + ")";
                    Toast.makeText(DashboardActivity.this, detailedError, Toast.LENGTH_LONG).show();

                    btnSync.setEnabled(true);
                    btnSync.setText("SYNC NOW");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(DashboardActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                btnSync.setEnabled(true);
                btnSync.setText("SYNC NOW");
            }
        });
    }

    private void triggerManualEmailReport() {
        Toast.makeText(this, "Preparing summary email report request...", Toast.LENGTH_SHORT).show();
    }

    private void exportResponsesToCsv() {
        if (allResponses.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csvContent = new StringBuilder();
        csvContent.append("ID,Rating,Timestamp,Device_ID,Shift\n");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        for (SurveyResponse res : allResponses) {
            csvContent.append(res.id).append(",")
                    .append(res.rating).append(",")
                    .append(sdf.format(new Date(res.timestamp))).append(",")
                    .append(res.deviceId).append(",")
                    .append(res.shift).append("\n");
        }

        try {
            File csvFile = new File(getCacheDir(), "survey_export.csv");
            FileOutputStream out = new FileOutputStream(csvFile);
            out.write(csvContent.toString().getBytes());
            out.close();

            Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", csvFile);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Store Survey Export Data");
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, "Share CSV file via:"));

        } catch (Exception e) {
            Toast.makeText(this, "Export error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}