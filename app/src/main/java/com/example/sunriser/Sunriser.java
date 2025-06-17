package com.example.sunriser;

import static android.app.AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.text.SimpleDateFormat;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
/**
 * Implementation of App Widget functionality.
 */
public class Sunriser extends AppWidgetProvider {
    private static String destinationAdress = "http://led.local:8080";
    public static final String TOGGLEBUTTON = "com.example.toggle";
    public static final String INCRBUTTON = "com.example.incr";
    public static final String DECRBUTTON = "com.example.decr";
    public static final String SUNRISEBUTTON = "com.example.sunrise";
    public static final String LINKBUTTON = "com.example.link";
    private static long NEXTAlARM = 0;

    private static String ALARM_TIME = "";
    private static long NEXTAlARM_MS = 0;

    private static boolean IS_LINKED = true;


    void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.sunriser);

        remoteViews.setOnClickPendingIntent(R.id.toggle_button,  getPendingSelfIntent(context, TOGGLEBUTTON   ));
        remoteViews.setOnClickPendingIntent(R.id.incr_button,    getPendingSelfIntent(context, INCRBUTTON     ));
        remoteViews.setOnClickPendingIntent(R.id.decr_button,    getPendingSelfIntent(context, DECRBUTTON     ));
        remoteViews.setOnClickPendingIntent(R.id.sunrise_button, getPendingSelfIntent(context, SUNRISEBUTTON  ));
        remoteViews.setOnClickPendingIntent(R.id.link_button,    getPendingSelfIntent(context, LINKBUTTON     ));
        Log.i("UpdateAppWidget", "ALARM_TIME: " + ALARM_TIME);
        remoteViews.setTextViewText(R.id.txt_next_alarm, ALARM_TIME);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.txt_next_alarm);

        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d("onUpdate", "Widget updated!");
        for (int appWidgetId : appWidgetIds) {
           updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        Toast.makeText(context, "Widget onUpdate called", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    public void setAlarmTimeFromAlarmClock(Context context, Intent intent, RESTWakeupInterface apiClient){
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        AlarmManager.AlarmClockInfo nextAlarm = alarm.getNextAlarmClock();

        NEXTAlARM_MS = nextAlarm != null ? (nextAlarm.getTriggerTime()) : 0;
        NEXTAlARM = NEXTAlARM_MS / 1000;

        Call<ResponseBody> resp = apiClient.RestWakeUp(new PostWake(String.valueOf(NEXTAlARM)));
        send(resp, context, intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        try {
            RESTWakeupInterface apiClient = LEDDimmerAPIClient.getClient(destinationAdress).create(RESTWakeupInterface.class);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, Sunriser.class);

            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

            if (TOGGLEBUTTON.equals(intent.getAction())) {
                send(apiClient.RestLightToggle(), context, intent);
            }

            if (INCRBUTTON.equals(intent.getAction())) {
                send(apiClient.RestLightIncr(), context, intent);
            }

            if (DECRBUTTON.equals(intent.getAction())) {
                send(apiClient.RestLightDecr(), context, intent);
            }

            if (SUNRISEBUTTON.equals(intent.getAction())) {
                send(apiClient.RestSunrise(), context, intent);
            }

            if (LINKBUTTON.equals(intent.getAction())) {
                IS_LINKED = !IS_LINKED;
                if (IS_LINKED) {
                    setAlarmTimeFromAlarmClock(context, intent, apiClient);
                } else {
                    update_time(0);
                }

                Log.i("onReceive", "LINKBUTTON: " + IS_LINKED);
                // if link is reestablished, check for next alarm
                for (int appWidgetId: appWidgetIds) {
                    RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.sunriser);
                    remoteViews.setTextViewText(R.id.txt_next_alarm, ALARM_TIME);
                    remoteViews.setImageViewResource(R.id.link_button, IS_LINKED ? R.drawable.link_intact : R.drawable.link_broken);
                    appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
                }
            }

            if (intent.getAction().equals(ACTION_NEXT_ALARM_CLOCK_CHANGED)) {
                if (!IS_LINKED){
                    Log.i("onReceive", "ACTION_NEXT_ALARM_CLOCK_CHANGED: NOT LINKED");
                    return;
                }
                Log.i("onReceive", "ACTION_NEXT_ALARM_CLOCK_CHANGED");
                setAlarmTimeFromAlarmClock(context, intent, apiClient);
                //AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                //AlarmManager.AlarmClockInfo nextAlarm = alarm.getNextAlarmClock();

                //NEXTAlARM_MS = nextAlarm != null ? (nextAlarm.getTriggerTime()) : 0;
                //NEXTAlARM = NEXTAlARM_MS / 1000;

                //Call<ResponseBody> resp = apiClient.RestWakeUp(new PostWake(String.valueOf(NEXTAlARM)));
                //send(resp, context, intent);
            }

        }
        catch(Exception e){
            Log.e("onReceive", e.toString());
        }
    }

    private void update_time(String alarm_time){
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

    }

    private void send(Call<ResponseBody> resp, Context context, Intent intent){

        resp.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String msg = response.body().string();
                    if(msg.contains("WAKEUP") || msg.contains("SUNRISE")){
                        update_time(msg.split(" ")[1]);

                        // ui update
                        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                        ComponentName thisWidget = new ComponentName(context, Sunriser.class);
                        Log.i("onResponse: ", "Message: " + msg);
                        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
                        for (int appWidgetId : appWidgetIds) {
                            updateAppWidget(context, appWidgetManager, appWidgetId);
                        }
                    }
                }catch(Exception e){
                    Log.i("onRespone: ", e.toString());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

                Log.i("Send:","sending failure " + call.toString() + t.toString());
            }

        });
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