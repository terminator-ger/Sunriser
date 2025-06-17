package com.example.sunriser;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.ActivityCompat;

public class SunriserConfigurationActivity extends Activity {

    int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get widget ID from intent
        Intent intent = getIntent();
        Log.d("WIDGET_CONFIG", "Intent: " + intent);
        Log.d("WIDGET_CONFIG", "Extras: " + intent.getExtras());
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish(); // No valid widget ID
            return;
        }

        // Example update: Create views and assign button handlers
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.sunriser);

        remoteViews.setOnClickPendingIntent(R.id.toggle_button, getPendingSelfIntent(this, Sunriser.TOGGLEBUTTON));
        remoteViews.setOnClickPendingIntent(R.id.incr_button, getPendingSelfIntent(this, Sunriser.INCRBUTTON));
        remoteViews.setOnClickPendingIntent(R.id.decr_button, getPendingSelfIntent(this, Sunriser.DECRBUTTON));
        remoteViews.setOnClickPendingIntent(R.id.sunrise_button, getPendingSelfIntent(this, Sunriser.SUNRISEBUTTON));

        // Apply update for widget!
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        manager.updateAppWidget(appWidgetId, remoteViews);



        // Return OK to system
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    public String getCurrentSsid(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                return "Permission not granted";
            }
        }

        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) return "No WifiManager";

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID();

        if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }

        return ssid;
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
