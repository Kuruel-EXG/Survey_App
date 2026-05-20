package com.store.survey.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ResponseDao {

    @Insert
    void insert(SurveyResponse response);

    // Change "FROM responses" to "FROM survey_responses"
    @Query("SELECT * FROM survey_responses")
    List<SurveyResponse> getAllResponses();

    // Change "FROM responses" to "FROM survey_responses"
    @Query("DELETE FROM survey_responses")
    void deleteAll();
}