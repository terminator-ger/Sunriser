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
import android.widget.EditText;
import android.widget.RemoteViews;
import android.window.OnBackInvokedCallback;

import androidx.core.app.ActivityCompat;

public class SunriserConfigurationActivity extends Activity {

    int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    public static String address = "192.168.1.70";
    public static int port = 8080;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get widget ID from intent
        setResult(RESULT_CANCELED);
        setContentView(R.layout.prefrences);
        EditText text_field_host = findViewById(R.id.config_host_field);
        EditText text_field_port = findViewById(R.id.config_port_field);
        text_field_host.setText(String.valueOf(address));
        text_field_port.setText(String.valueOf(port));

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
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_CANCELED, resultValue);

        // Example update: Create views and assign button handlers
        RemoteViews widgetView = new RemoteViews(getPackageName(), R.layout.sunriser);
        //this.update(this);

        // Return OK to system
        resultValue = new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0,
                new OnBackInvokedCallback() {
                    @Override
                    public void onBackInvoked() {
                        // update settings
                        EditText text_field_host = findViewById(R.id.config_host_field);
                        EditText text_field_port = findViewById(R.id.config_port_field);
                        address = text_field_host.getText().toString();
                        port = Integer.valueOf(text_field_port.getText().toString());
                        finish();
                    }
                });
    }

    public void onBack(){

    }

    //public void update(Context context){
    //    RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.sunriser);

    //    widgetView.setOnClickPendingIntent(R.id.toggle_button, getPendingSelfIntent(this, Sunriser.TOGGLEBUTTON));
    //    widgetView.setOnClickPendingIntent(R.id.incr_button, getPendingSelfIntent(this, Sunriser.INCRBUTTON));
    //    widgetView.setOnClickPendingIntent(R.id.decr_button, getPendingSelfIntent(this, Sunriser.DECRBUTTON));
    //    widgetView.setOnClickPendingIntent(R.id.sunrise_button, getPendingSelfIntent(this, Sunriser.SUNRISEBUTTON));


    //    // Apply update for widget!
    //    AppWidgetManager manager = AppWidgetManager.getInstance(this);
    //    manager.updateAppWidget(appWidgetId,  widgetView);

    //}
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
