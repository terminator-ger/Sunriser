package com.example.sunriser;

public class ConfigurationManager{
    private static Configuration configuration;
    public static Configuration getConfiguration(){
        if (configuration==null){
            configuration = new Configuration();
        }
        return configuration;
    }
}