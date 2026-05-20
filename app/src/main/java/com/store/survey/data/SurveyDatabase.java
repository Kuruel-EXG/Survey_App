package com.store.survey.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// Change version from 1 to 2 here:
@Database(entities = {SurveyResponse.class}, version = 2, exportSchema = false)
public abstract class SurveyDatabase extends RoomDatabase {

    public abstract ResponseDao responseDao();

    private static volatile SurveyDatabase INSTANCE;

    public static SurveyDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (SurveyDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    SurveyDatabase.class, "survey_database")
                            .fallbackToDestructiveMigration() // Clears old version 1 tables cleanly
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}