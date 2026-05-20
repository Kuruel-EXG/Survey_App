package com.store.survey.network;

import com.store.survey.data.SurveyResponse;
import java.util.List;

public class SheetDbRequest {
    public List<SurveyResponse> data;

    public SheetDbRequest(List<SurveyResponse> data) {
        this.data = data;
    }
}