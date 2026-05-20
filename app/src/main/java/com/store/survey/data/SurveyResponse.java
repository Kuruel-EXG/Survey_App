package com.store.survey.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "survey_responses")
public class SurveyResponse {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String deviceId;
    public long timestamp; // long numerical format required for new Date(res.timestamp)
    public String rating;
    public String shift = "Standard"; // Restored field for dashboard logs and CSV files
}