package com.example.sunriser;

/**
 * Created by Michael on 28 Jan 2018.
 */


import com.google.gson.annotations.SerializedName;

/**
 *  @brief: PostWake Class that encapulates the set wakeuptime
 */
public class PostWake{

    @SerializedName("time")
    private String time;

    public PostWake(String time){
        this.time = time;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}