package com.example.sunriser;

import android.util.Log;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Michael on 28 Jan 2018.
 */

public class LEDDimmerAPIClient {

    private static Retrofit retrofit = null;

    public static Retrofit getClient(String BASE_URL) {
        if (retrofit==null || (!(retrofit.baseUrl().toString().equals(BASE_URL)))){
            Log.i("RetroFit", "new client");
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
