package com.example.sunriser;

import android.content.res.Resources;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


//class NamedArray<T>{
//    String key;
//    T[] values;
//}
@JsonIgnoreProperties(ignoreUnknown = true)
class Presets{
    public String color = "sunrise_01";
    public String color_interpolation = "linear";
    public String gradient = "";
    public String gradient_interpolation = "linear";
    public int wakeup_sequence_len = 30;
    public int pwm_steps = 300;
}
class Local {
    public String address = "192.168.0.108";
    public int port = 8080;
}
@JsonIgnoreProperties(ignoreUnknown = true)
class Remote {

    public Remote(){
        sunrise_profile = "";
        presets = new HashMap<String, Presets>();
        //colors = new NamedArray<String>[];
        colors = new HashMap<String, List<String>>();
        //gradients = new NamedArray[];
        gradient = new HashMap<String, List<Float>>();
    }
    public Map<String, Presets> presets;
    public Map<String, List<String>> colors;
    public Map<String, List<Float>> gradient;
    public String sunrise_profile;
    public boolean has_w;
    public boolean has_rgb;
    public float latitude;
    public float longitude;
    public String time_zone;
    public int GPIO_W;
    public int GPIO_R;
    public int GPIO_G;
    public int GPIO_B;
}

@JsonIgnoreProperties(ignoreUnknown = true)
public class Configuration {
    public Configuration(){
        local = new Local();
        remote = new Remote();
    }
    public Local local;
    public Remote remote;

    public static Presets getActiveProfile(){

        Configuration config = ConfigurationManager.getConfiguration();
        if (config.remote.presets.containsKey(config.remote.sunrise_profile)) {
            return config.remote.presets.get(config.remote.sunrise_profile);
        }else{
            Log.e("getActiveProfile", "Could not find " + config.remote.sunrise_profile + " in profiles");
            throw new Resources.NotFoundException("Could not find " + config.remote.sunrise_profile + " in profiles");
        }
    }
    public static void readConfigFromJSON(JSONObject json){
        readConfigFromJSON(json.toString());
    }
    public static void readConfigFromJSON(String json){
        try {
            ObjectMapper mapper = new ObjectMapper();
            ConfigurationManager.getConfiguration().remote = mapper.readValue(json, new TypeReference<Remote>() {
            });
        } catch (JsonProcessingException e) {
            Log.e("JSON", e.toString());
            throw new RuntimeException(e);
        }
    }
    public static String writeConfigToJSON(){
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(ConfigurationManager.getConfiguration());
        }catch (JsonProcessingException e){
            throw new RuntimeException(e);
        }
    }

}
