package com.range.task;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    public static ApiService getRetrofitInterface() {

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);

        return new Retrofit.Builder().baseUrl("https://unconnected-lane.000webhostapp.com/")
                .addConverterFactory(GsonConverterFactory.create(serializeNullsGson()))
                .client(new OkHttpClient.Builder().addInterceptor(logging).build())
                .build().create(ApiService.class);
    }

    private static Gson serializeNullsGson() {
        return new GsonBuilder().setLenient().create();
    }

}
