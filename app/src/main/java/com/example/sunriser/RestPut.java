package com.example.sunriser;

/**
 * Created by Michael on 28 Jan 2018.
 */


import com.google.gson.annotations.SerializedName;

    class Config{
        @SerializedName("config")
        private String config;
        public Config(String json){
            this.config = json;
        }

        public static Config createConfig(String json){
            return new Config(json);
        }
    }
    class Wakeup {
        public static Wakeup createWakeup(String time){
            return new Wakeup(time);
        }

        @SerializedName("time")
        private String time;

        public Wakeup(String time) {
            this.time = time;
        }
    }
