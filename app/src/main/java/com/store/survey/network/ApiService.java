package com.store.survey.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface ApiService {
    @POST
    Call<Void> submitResponses(@Url String url, @Body SheetDbRequest request);
}