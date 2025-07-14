package com.example.sunriser;

import static android.app.AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.json.*;

import java.text.SimpleDateFormat;
import java.util.Objects;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
/**
 * Implementation of App Widget functionality.
 */
public class Sunriser extends AppWidgetProvider {
    public static final String TOGGLEBUTTON = "com.example.toggle";
    public static final String INCRBUTTON = "com.example.incr";
    public static final String DECRBUTTON = "com.example.decr";
    public static final String SUNRISEBUTTON = "com.example.sunrise";
    public static final String LINKBUTTON = "com.example.link";
    private static long NEXTAlARM = 0;

    private static String ALARM_TIME = "";

    private static boolean IS_LINKED = true;    // local app variable
    private static boolean IS_TOGGLED = false;
    private static boolean IS_SUNRISING = false;
    private static boolean IS_LINKED_ALARM = false;

    void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.sunriser);

        remoteViews.setOnClickPendingIntent(R.id.toggle_button,  getPendingSelfIntent(context, TOGGLEBUTTON   ));
        remoteViews.setOnClickPendingIntent(R.id.incr_button,    getPendingSelfIntent(context, INCRBUTTON     ));
        remoteViews.setOnClickPendingIntent(R.id.decr_button,    getPendingSelfIntent(context, DECRBUTTON     ));
        remoteViews.setOnClickPendingIntent(R.id.sunrise_button, getPendingSelfIntent(context, SUNRISEBUTTON  ));
        remoteViews.setOnClickPendingIntent(R.id.link_button,    getPendingSelfIntent(context, LINKBUTTON     ));

        int sunrise_background = IS_SUNRISING ? R.drawable.rounded_button_green : R.drawable.rounded_button;
        remoteViews.setInt(R.id.sunrise_button, "setBackgroundResource", sunrise_background);

        int toggle_background = IS_TOGGLED ? R.drawable.rounded_button_green : R.drawable.rounded_button;
        remoteViews.setInt(R.id.toggle_button, "setBackgroundResource", toggle_background);
        //remoteViews.setBoolean(R.id.incr_button, "setEnabled", IS_TOGGLED);
        //remoteViews.setBoolean(R.id.decr_button, "setEnabled", IS_TOGGLED);

        if (IS_LINKED && IS_LINKED_ALARM){
            remoteViews.setInt(R.id.link_button, "setBackgroundResource", R.drawable.rounded_button_green);
        }else if (IS_LINKED) {
            remoteViews.setInt(R.id.link_button, "setBackgroundResource", R.drawable.rounded_button_yellow);
        } else {
            remoteViews.setInt(R.id.link_button, "setBackgroundResource", R.drawable.rounded_button);
        }
        remoteViews.setImageViewResource(R.id.link_button, IS_LINKED ? R.drawable.link_intact : R.drawable.link_broken);
        remoteViews.setTextViewText(R.id.txt_next_alarm, ALARM_TIME);

        Log.i("UpdateAppWidget", "ALARM_TIME: " + ALARM_TIME);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.txt_next_alarm);
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    public void pollStatus(Context context){
        Intent intent = new Intent(context, Sunriser.class);
        String destinationAddress = "http://"+SunriserConfigurationActivity.address+":"+SunriserConfigurationActivity.port;
        RESTWakeupInterface apiClient = LEDDimmerAPIClient.getClient(destinationAddress).create(RESTWakeupInterface.class);
        send(apiClient.RestStatus(), context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        pollStatus(context);
        Log.d("onUpdate", "Widget updated!");
        forceOnUpdate(context);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        try {
            String destinationAddress = "http://"+SunriserConfigurationActivity.address+":"+SunriserConfigurationActivity.port;
            Log.i("HOST", destinationAddress);
            RESTWakeupInterface apiClient = LEDDimmerAPIClient.getClient(destinationAddress).create(RESTWakeupInterface.class);

            if (TOGGLEBUTTON.equals(intent.getAction())) {
                send(apiClient.RestLightToggle(), context, intent);
                return;
            }

            if (INCRBUTTON.equals(intent.getAction())) {
                send(apiClient.RestLightIncr(), context, intent);
                return;
            }

            if (DECRBUTTON.equals(intent.getAction())) {
                send(apiClient.RestLightDecr(), context, intent);
                return;
            }

            if (SUNRISEBUTTON.equals(intent.getAction())) {
                if (IS_LINKED) {
                    send(apiClient.RestSunrise(), context, intent);
                }
                return;
            }

            if (LINKBUTTON.equals(intent.getAction())) {
                IS_LINKED = !IS_LINKED;
                if (IS_LINKED) {
                    setAlarmTimeFromAlarmClock(context, intent, apiClient);
                } else {
                    update_time(0);
                }
                pollStatus(context);
                Log.i("onReceive", "LINKBUTTON: " + IS_LINKED);
                return;
            }

            if (intent.getAction().equals(ACTION_NEXT_ALARM_CLOCK_CHANGED)) {
                // update when the clock is changed
                if (!IS_LINKED){
                    Log.i("onReceive", "ACTION_NEXT_ALARM_CLOCK_CHANGED: NOT LINKED - update ui");
                    return;
                }
                Log.i("onReceive", "ACTION_NEXT_ALARM_CLOCK_CHANGED");
                setAlarmTimeFromAlarmClock(context, intent, apiClient);
            }
        }
        catch(Exception e){
            Log.e("onReceive", e.toString());
        }
    }

    private void update_time(String alarm_time){
        /*
            called from onReceive when the clock is changed
            alarm_time: epoch time in seconds
         */
        update_time(Long.parseLong(alarm_time));
    }
    private void update_time(long alarm_time){
        /*
            alarm_time: epoch time in seconds
         */
        String vv = new SimpleDateFormat("EEEE hh:mm").format(new java.util.Date(alarm_time * 1000));
        NEXTAlARM = alarm_time;
        ALARM_TIME = alarm_time == 0 ? " - " : vv;
        Log.i("update_time: ", "SetALARM " + ALARM_TIME);
        IS_LINKED_ALARM = (alarm_time != 0);
    }

    public void setAlarmTimeFromAlarmClock(Context context, Intent intent, RESTWakeupInterface apiClient){
        /*
            called from onReceive when the clock is changed
         */
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        AlarmManager.AlarmClockInfo nextAlarm = alarm.getNextAlarmClock();

        NEXTAlARM = nextAlarm != null ? (nextAlarm.getTriggerTime() / 1000) : 0;

        Call<ResponseBody> resp = apiClient.RestWakeUp(new PostWake(String.valueOf(NEXTAlARM)));
        send(resp, context, intent);
    }

    private void send(Call<ResponseBody> resp, Context context, Intent intent){

        resp.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    assert response.body() != null;
                    String msg = response.body().string();
                    Log.i("onResponseCode", String.valueOf(response.code()));
                    Log.i("onResponseHeader", response.headers().toString());
                    Log.i("onResponseBody", msg);

                    if (msg.startsWith("WAKEUP")){
                        IS_LINKED_ALARM = String.valueOf(response.code()).equals("200") && !msg.startsWith("WAKEUP 0");
                        Log.i("onResponseTime", msg.split(" ")[1]);
                        update_time(msg.split(" ")[1]);
                    }

                    if (msg.startsWith("SUNRISE")) {
                        IS_SUNRISING = String.valueOf(response.code()).equals("200") && !msg.startsWith("SUNRISE 0");
                        Log.i("onResponseTime", msg.split(" ")[1]);
                        update_time(msg.split(" ")[1]);
                    }

                    if (msg.startsWith("TOGGLE")) {
                        IS_TOGGLED = msg.startsWith("TOGGLE ON");
                    }

                    if (msg.startsWith("STATUS")) {
                        JSONObject json = new JSONObject(msg.replaceFirst("STATUS ", ""));
                        Log.i("STATUS", json.toString());
                        if (json.getBoolean("wakeup_task_alive")){
                            String type = json.getString("wakeup_type");

                            if (type.equals("alarm")) {
                                IS_LINKED_ALARM = true;
                                Log.i("STATUS", "Alarm was set");
                                update_time(json.getString("wakeup_time"));
                            }
                            if (type.equals("sunrise")) {
                                IS_SUNRISING = true;
                                Log.i("STATUS", "IS_SURISING was set");
                                update_time(json.getString("wakeup_time"));
                            }
                        }else{
                            IS_SUNRISING = false;
                            IS_LINKED_ALARM = false;
                            ALARM_TIME = "";
                            update_time(0);
                        }
                        double w_status = json.getDouble("w_status");
                        double r_status = json.getJSONArray("rgb_status").getDouble(0);
                        double g_status = json.getJSONArray("rgb_status").getDouble(1);
                        double b_status = json.getJSONArray("rgb_status").getDouble(2);
                        IS_TOGGLED = w_status > 0.0 || r_status > 0.0 || g_status > 0.0 || b_status > 0.0;
                    }
                }catch(Exception e){
                    Log.i("onRespone: ", e.toString());
                }
                // update ui
                forceOnUpdate(context);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.i("Send:","sending failure " + call.toString() + t.toString());
            }
        });
    }
    private void forceOnUpdate(Context context){
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, Sunriser.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int appWidgetId : appWidgetIds) {
            Log.i("forceOnUpdate", String.valueOf(appWidgetId));
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }
    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, Sunriser.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}