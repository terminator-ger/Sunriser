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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.json.*;

import java.io.IOException;
import java.net.ConnectException;
import java.text.SimpleDateFormat;
import java.util.Objects;

import okhttp3.Connection;
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


    void fadeOut(AppWidgetManager appWidgetManager, RemoteViews remoteViews, int[] appWidgetIds, int idx, int maxSteps) {
        remoteViews.setViewVisibility(R.id.connection_error, View.VISIBLE);
        remoteViews.setFloat(R.id.connection_error, "setAlpha", (float) (1.0 - (idx * 0.1)));
        Log.i("fadeOut", String.valueOf(idx));
        for (int appWidgetId : appWidgetIds) {
            if (idx == maxSteps - 1) {
                // Hide the ImageView by setting visibility gone
                remoteViews.setViewVisibility(R.id.connection_error, View.INVISIBLE);
                appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
                return;
            }
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }

    void connection_error(Context context) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.sunriser);
        //remoteViews.setOnClickPendingIntent(R.id.connection_error, getPendingSelfIntent(context, CONNECTIONERROR));
        remoteViews.setInt(R.id.connection_error, "setVisibility", View.VISIBLE);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, Sunriser.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        Handler handler = new Handler(Looper.getMainLooper());
        int steps = 10;
        for (int i = 0; i < steps; i++) {
            final int stepIdx = i;
            handler.postDelayed(() -> fadeOut(appWidgetManager, remoteViews, appWidgetIds, stepIdx, steps), steps * 5000); // Adjust timing for fade speed
        }
    }

    void changeButton(Context context, AppWidgetManager appWidgetManager, int id, int drawable) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.sunriser);
        ComponentName thisWidget = new ComponentName(context, Sunriser.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        //remoteViews.setOnClickPendingIntent(id,  getPendingSelfIntent(context, button));
        remoteViews.setInt(id, "setBackgroundResource", drawable);
        for (int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }

    void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.sunriser);

        remoteViews.setOnClickPendingIntent(R.id.toggle_button, getPendingSelfIntent(context, TOGGLEBUTTON));
        remoteViews.setOnClickPendingIntent(R.id.incr_button, getPendingSelfIntent(context, INCRBUTTON));
        remoteViews.setOnClickPendingIntent(R.id.decr_button, getPendingSelfIntent(context, DECRBUTTON));
        remoteViews.setOnClickPendingIntent(R.id.sunrise_button, getPendingSelfIntent(context, SUNRISEBUTTON));
        remoteViews.setOnClickPendingIntent(R.id.link_button, getPendingSelfIntent(context, LINKBUTTON));

        int sunrise_background = IS_SUNRISING ? R.drawable.rounded_button_green : R.drawable.rounded_button;
        //remoteViews.setInt(R.id.sunrise_button, "setBackgroundResource", sunrise_background);
        changeButton(context, appWidgetManager, R.id.sunrise_button, sunrise_background);

        int toggle_background = IS_TOGGLED ? R.drawable.rounded_button_green : R.drawable.rounded_button;
        //remoteViews.setInt(R.id.toggle_button, "setBackgroundResource", toggle_background);
        changeButton(context, appWidgetManager, R.id.toggle_button, toggle_background);
        remoteViews.setBoolean(R.id.incr_button, "setEnabled", IS_TOGGLED);
        remoteViews.setBoolean(R.id.decr_button, "setEnabled", IS_TOGGLED);

        if (IS_LINKED && IS_LINKED_ALARM) {
            //remoteViews.setInt(R.id.link_button, "setBackgroundResource", R.drawable.rounded_button_green);
            changeButton(context, appWidgetManager, R.id.link_button, R.drawable.rounded_button_green);
        } else if (IS_LINKED) {
            //remoteViews.setInt(R.id.link_button, "setBackgroundResource", R.drawable.rounded_button_yellow);
            changeButton(context, appWidgetManager, R.id.link_button, R.drawable.rounded_button_yellow);
        } else {
            //remoteViews.setInt(R.id.link_button, "setBackgroundResource", R.drawable.rounded_button);
            changeButton(context, appWidgetManager, R.id.link_button, R.drawable.rounded_button);
        }
        remoteViews.setImageViewResource(R.id.link_button, IS_LINKED ? R.drawable.link_intact : R.drawable.link_broken);
        remoteViews.setTextViewText(R.id.txt_next_alarm, ALARM_TIME);

        Log.i("UpdateAppWidget", "ALARM_TIME: " + ALARM_TIME);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.txt_next_alarm);
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    public void pollStatus(Context context) {
        String destinationAddress = "http://" + SunriserConfigurationActivity.address + ":" + SunriserConfigurationActivity.port;
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        RESTWakeupInterface apiClient = LEDDimmerAPIClient.getClient(destinationAddress).create(RESTWakeupInterface.class);

        apiClient.RestStatus().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                assert response.body() != null;
                String msg = null;
                try {
                    msg = response.body().string();
                    JSONObject json = new JSONObject(msg.replaceFirst("STATUS ", ""));
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
                    throw new RuntimeException(e);
                }
                forceOnUpdate(context);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                connection_error(context);
                changeButton(context, appWidgetManager, R.id.link_button, R.drawable.rounded_button);
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

    public void toggle_action(Context context, AppWidgetManager appWidgetManager, RESTWakeupInterface apiClient) {
        changeButton(context, appWidgetManager, R.id.toggle_button, R.drawable.rounded_button_yellow);
        apiClient.RestLightToggle().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                assert response.body() != null;
                String msg = null;
                try {
                    msg = response.body().string();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                IS_TOGGLED = msg.startsWith("TOGGLE ON");
                forceOnUpdate(context);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                connection_error(context);
                changeButton(context, appWidgetManager, R.id.toggle_button, R.drawable.rounded_button);
            }
        });
    }

    public void incr_action(Context context, AppWidgetManager appWidgetManager, RESTWakeupInterface apiClient) {
        changeButton(context, appWidgetManager, R.id.incr_button, R.drawable.rounded_button_yellow);
        apiClient.RestLightIncr().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                changeButton(context, appWidgetManager, R.id.incr_button, R.drawable.rounded_button_green);
                changeButton(context, appWidgetManager, R.id.incr_button, R.drawable.rounded_button);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                connection_error(context);
                changeButton(context, appWidgetManager, R.id.incr_button, R.drawable.rounded_button);
            }
        });
    }

    public void decr_action(Context context, AppWidgetManager appWidgetManager, RESTWakeupInterface apiClient) {
        changeButton(context, appWidgetManager, R.id.decr_button, R.drawable.rounded_button_yellow);
        apiClient.RestLightDecr().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                changeButton(context, appWidgetManager, R.id.incr_button, R.drawable.rounded_button_green);
                changeButton(context, appWidgetManager, R.id.incr_button, R.drawable.rounded_button);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                connection_error(context);
                changeButton(context, appWidgetManager, R.id.decr_button, R.drawable.rounded_button);
            }
        });
    }

    public void sunrise_action(Context context, AppWidgetManager appWidgetManager, RESTWakeupInterface apiClient){
        if(IS_LINKED) {
            changeButton(context, appWidgetManager, R.id.sunrise_button, R.drawable.rounded_button_yellow);
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
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    changeButton(context, appWidgetManager, R.id.sunrise_button, R.drawable.rounded_button);
                }
            });
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String destinationAddress = "http://" + SunriserConfigurationActivity.address + ":" + SunriserConfigurationActivity.port;
        Log.i("HOST", destinationAddress);
        RESTWakeupInterface apiClient = LEDDimmerAPIClient.getClient(destinationAddress).create(RESTWakeupInterface.class);
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
                setAlarmTimeFromAlarmClock(context, apiClient);
            } else {
                update_time(0, context);
            }
            pollStatus(context);
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
        forceOnUpdate(context);
    }

    public void setAlarmTimeFromAlarmClock(Context context, RESTWakeupInterface apiClient){
        /*
            called from onReceive when the clock is changed
         */
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        changeButton(context, appWidgetManager, R.id.link_button, R.drawable.rounded_button_yellow);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        AlarmManager.AlarmClockInfo nextAlarm = alarm.getNextAlarmClock();
        NEXTAlARM = nextAlarm != null ? (nextAlarm.getTriggerTime() / 1000) : 0;

        Call<ResponseBody> resp = apiClient.RestWakeUp(new PostWake(String.valueOf(NEXTAlARM)));
        resp.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    assert response.body() != null;
                    String msg = response.body().string();
                    Log.i("onResponseTime", msg.split(" ")[1]);
                    IS_LINKED_ALARM = String.valueOf(response.code()).equals("200") && !msg.startsWith("WAKEUP 0");
                    update_time(msg.split(" ")[1], context);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.i("setAlarmTimeFromAlarmClock", t.toString());
                connection_error(context);
                changeButton(context, appWidgetManager, R.id.link_button, R.drawable.rounded_button);
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