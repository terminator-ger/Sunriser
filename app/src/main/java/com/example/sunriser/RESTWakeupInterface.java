package com.example.sunriser;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.PUT;
import retrofit2.http.Headers;

/**
 * Created by Michael on 28 Jan 2018.
 */
public interface RESTWakeupInterface {
    @Headers({"Content-Type: application/json"})
    @PUT("wakeuptime")
    Call<ResponseBody> RestWakeUp(@Body PostWake postWake);
    @Headers({"Content-Type: application/json"})
    @PUT("toggle")
    Call<ResponseBody> RestLightToggle();
    @Headers({"Content-Type: application/json"})
    @PUT("incr")
    Call<ResponseBody> RestLightIncr();
    @Headers({"Content-Type: application/json"})
    @PUT("decr")
    Call<ResponseBody> RestLightDecr();
    @Headers({"Content-Type: application/json"})
    @PUT("sunrise")
    Call<ResponseBody> RestSunrise();
}
