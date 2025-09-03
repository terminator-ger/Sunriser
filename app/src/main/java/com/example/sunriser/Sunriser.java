package com.example.sunriser;

import static android.app.AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED;
import static com.example.sunriser.LEDDimmerAPIClient.getRestClient;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;

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
    public static final String CONNECTIONERROR = "com.example.con_error";
    private static long NEXTAlARM = 0;

    private static String ALARM_TIME = "";

    private static boolean IS_LINKED = true;    // local app variable
    private static boolean IS_TOGGLED = false;
    private static boolean IS_SUNRISING = false;
    private static boolean IS_LINKED_ALARM = false;


    void connection_error(Context context) {
        Log.i("connection", "error");
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.sunriser);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        remoteViews.setInt(R.id.connection_error, "setVisibility", View.VISIBLE);
        Handler handler = new Handler(Looper.getMainLooper());
        ComponentName thisWidget = new ComponentName(context, Sunriser.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        for (int appWidgetId : appWidgetIds) {
            handler.postDelayed(() -> {
                remoteViews.setViewVisibility(R.id.connection_error, View.INVISIBLE);
                appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
            }, 5000);
        }
    }


    void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.sunriser);

        remoteViews.setOnClickPendingIntent(R.id.toggle_button, getPendingSelfIntent(context, TOGGLEBUTTON));
        remoteViews.setOnClickPendingIntent(R.id.incr_button, getPendingSelfIntent(context, INCRBUTTON));
        remoteViews.setOnClickPendingIntent(R.id.decr_button, getPendingSelfIntent(context, DECRBUTTON));
        remoteViews.setOnClickPendingIntent(R.id.link_button, getPendingSelfIntent(context, LINKBUTTON));


        Log.i("updateAppWidget", "Setting incr/ decr buttons to " + String.valueOf(IS_TOGGLED));
        if (IS_TOGGLED){
            remoteViews.setImageViewResource(R.id.toggle_button, R.drawable.icn_on);
            //remoteViews.setInt(R.id.toggle_button, "setBackgroundResource", R.drawable.button_selector_green);
            remoteViews.setInt(R.id.incr_button, "setBackgroundResource", R.drawable.button_selector);
            remoteViews.setInt(R.id.decr_button, "setBackgroundResource", R.drawable.button_selector);
        }else{
            remoteViews.setImageViewResource(R.id.toggle_button, R.drawable.icn_off);
            //remoteViews.setInt(R.id.toggle_button, "setBackgroundResource", R.drawable.button_selector);
            remoteViews.setInt(R.id.incr_button, "setBackgroundResource", R.drawable.rounded_button_disabled);
            remoteViews.setInt(R.id.decr_button, "setBackgroundResource", R.drawable.rounded_button_disabled);
        }

        Log.i("updateAppWidget", "IS_LINKED " + String.valueOf(IS_LINKED));
        //if (IS_LINKED && IS_LINKED_ALARM) {
        //    remoteViews.setInt(R.id.link_button, "setBackgroundResource", R.drawable.button_selector_green);
        //} else if (IS_LINKED) {
        //    remoteViews.setInt(R.id.link_button, "setBackgroundResource", R.drawable.rounded_button_yellow);
        //} else {
        //    remoteViews.setInt(R.id.link_button, "setBackgroundResource", R.drawable.button_selector);
        //}
        remoteViews.setImageViewResource(R.id.link_button, IS_LINKED ? R.drawable.icn_linked : R.drawable.icn_unlinked);

        Log.i("UpdateAppWidget", "ALARM_TIME: " + ALARM_TIME);
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }


    public void pollStatus(Context context) {
        RESTWakeupInterface apiClient = getRestClient();

        apiClient.RestStatus().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                assert response.body() != null;
                String msg = null;
                try {
                    msg = response.body().string();
                    JSONObject json = new JSONObject(msg.replaceFirst("STATUS ", ""));
                    //Configuration config = ConfigurationManager.getConfiguration();
                    //config.readConfigFromJSON(json.toString());
                    Log.i("STATUS", json.toString());
                    if (json.getBoolean("wakeup_task_alive")) {
                        String type = json.getString("wakeup_type");

                        if (type.equals("alarm")) {
                            IS_LINKED_ALARM = true;
                            Log.i("STATUS", "Alarm was set");
                            update_time(json.getString("wakeup_time"), context);
                        }
                        if (type.equals("sunrise")) {
                            IS_SUNRISING = true;
                            Log.i("STATUS", "IS_SURISING was set");
                            update_time(json.getString("wakeup_time"), context);
                        }

                    } else {
                        IS_SUNRISING = false;
                        IS_LINKED_ALARM = false;
                        ALARM_TIME = "";
                        update_time(0, context);
                    }
                    double w_status = json.getDouble("w_status");
                    double r_status = json.getJSONArray("rgb_status").getDouble(0);
                    double g_status = json.getJSONArray("rgb_status").getDouble(1);
                    double b_status = json.getJSONArray("rgb_status").getDouble(2);
                    IS_TOGGLED = w_status > 0.0 || r_status > 0.0 || g_status > 0.0 || b_status > 0.0;
                } catch (Exception e) {
                    Log.i("Link", e.toString());
                    throw new RuntimeException(e);
                }
                forceOnUpdate(context);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                connection_error(context);
                //changeButton(context, R.id.link_button, "setActivated", false);
                IS_LINKED = false;
                IS_LINKED_ALARM = false;
                forceOnUpdate(context);
            }
        });

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

    public void updateConfiguration(){
        RESTWakeupInterface apiClient = getRestClient();
    }

    public void toggle_action(Context context, AppWidgetManager appWidgetManager, RESTWakeupInterface apiClient) {
        apiClient.RestLightToggle().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                assert response.body() != null;
                try {
                    String msg = response.body().string();
                    Log.i("response", msg.toString());
                    IS_TOGGLED = msg.startsWith("TOGGLE ON");
                    forceOnUpdate(context);
                } catch (IOException e) {
                    Log.i("response", e.toString());
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                connection_error(context);
                //changeButton(context, R.id.toggle_button, "isPressed", false);
                IS_TOGGLED = false;
                forceOnUpdate(context);
            }
        });
    }

    public void incr_action(Context context, AppWidgetManager appWidgetManager, RESTWakeupInterface apiClient) {
        apiClient.RestLightIncr().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                connection_error(context);
            }
        });
    }

    public void decr_action(Context context, AppWidgetManager appWidgetManager, RESTWakeupInterface apiClient) {
        //changeButton(context, R.id.decr_button, "setPressed", true);

        apiClient.RestLightDecr().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                connection_error(context);
            }
        });
    }

    public void sunrise_action(Context context, AppWidgetManager appWidgetManager, RESTWakeupInterface apiClient){
        if(IS_LINKED) {
            //changeButton(context, R.id.sunrise_button, "setPressed", true);

            apiClient.RestSunrise().enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    assert response.body() != null;
                    String msg = null;
                    try {
                        msg = response.body().string();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    IS_SUNRISING = String.valueOf(response.code()).equals("200") && !msg.startsWith("SUNRISE 0");
                    Log.i("onResponseTime", msg.split(" ")[1]);
                    update_time(msg.split(" ")[1], context);
                    forceOnUpdate(context);
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    IS_SUNRISING = false;
                    connection_error(context);
                    forceOnUpdate(context);
                    //changeButton(context, R.id.sunrise_button, "setActivated", false);
                }
            });
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
       super.onReceive(context, intent);
       RESTWakeupInterface apiClient = getRestClient();
       AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (TOGGLEBUTTON.equals(intent.getAction())) {
            toggle_action(context, appWidgetManager, apiClient);
            return;
        }

        if (INCRBUTTON.equals(intent.getAction())) {
            incr_action(context, appWidgetManager, apiClient);
            return;
        }

        if (DECRBUTTON.equals(intent.getAction())) {
            decr_action(context, appWidgetManager, apiClient);
            return;
        }

        if (SUNRISEBUTTON.equals(intent.getAction())) {
            sunrise_action(context, appWidgetManager, apiClient);
            return;
        }

        if (LINKBUTTON.equals(intent.getAction())) {
            IS_LINKED = !IS_LINKED;
            if (IS_LINKED) {
                pollStatus(context);
            }else {
                forceOnUpdate(context);
            }
            Log.i("onReceive", "LINKBUTTON: " + IS_LINKED);
            return;
        }

        if (intent.getAction().equals(ACTION_NEXT_ALARM_CLOCK_CHANGED)) {
            // update when the clock is changed
            if (!IS_LINKED) {
                Log.i("onReceive", "ACTION_NEXT_ALARM_CLOCK_CHANGED: NOT LINKED - update ui");
                return;
            }
            setAlarmTimeFromAlarmClock(context, apiClient);
        }
    }

    private void update_time(String alarm_time, Context context){
        /*
            called from onReceive when the clock is changed
            alarm_time: epoch time in seconds
         */
        update_time(Long.parseLong(alarm_time), context);
    }
    private void update_time(long alarm_time, Context context){
        /*
            alarm_time: epoch time in seconds
         */
        String vv = new SimpleDateFormat("EEEE hh:mm").format(new java.util.Date(alarm_time * 1000));
        NEXTAlARM = alarm_time;
        ALARM_TIME = alarm_time == 0 ? " - " : vv;
        Log.i("update_time: ", "SetALARM " + ALARM_TIME);
        IS_LINKED_ALARM = (alarm_time != 0);
    }

    public void setAlarmTimeFromAlarmClock(Context context, RESTWakeupInterface apiClient){
        /*
            called from onReceive when the clock is changed
         */
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        //changeButton(context, R.id.link_button, "isPressed", true);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        AlarmManager.AlarmClockInfo nextAlarm = alarm.getNextAlarmClock();
        NEXTAlARM = nextAlarm != null ? (nextAlarm.getTriggerTime() / 1000) : 0;

        Call<ResponseBody> resp = apiClient.RestWakeUp(Wakeup.createWakeup(String.valueOf(NEXTAlARM)));
        resp.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    assert response.body() != null;
                    String msg = response.body().string();
                    Log.i("onResponseTime", msg.split(" ")[1]);
                    IS_LINKED_ALARM = String.valueOf(response.code()).equals("200") && !msg.startsWith("WAKEUP 0");
                    update_time(msg.split(" ")[1], context);
                    forceOnUpdate(context);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.i("setAlarmTimeFromAlarmClock", t.toString());
                connection_error(context);
                IS_LINKED_ALARM = false;
                //changeButton(context, R.id.link_button, "setActivated", false);
                forceOnUpdate(context);
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