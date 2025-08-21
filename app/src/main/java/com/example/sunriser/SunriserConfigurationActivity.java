package com.example.sunriser;

import static com.example.sunriser.LEDDimmerAPIClient.getRestClient;
import static com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_CLOCK;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class SunriserConfigurationActivity extends AppCompatActivity {

    int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get widget ID from intent
        setResult(RESULT_CANCELED);
        RESTWakeupInterface apiClient = getRestClient();

        apiClient.RestReadConfig().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                assert response.body() != null;
                String msg = null;
                try {
                    msg = response.body().string();
                    JSONObject json = new JSONObject(msg.replaceFirst("CONFIG", ""));
                    ConfigurationManager.getConfiguration().readConfigFromJSON(json);
                    Log.i("config", ConfigurationManager.getConfiguration().remote.toString());
                    init();
                } catch (Exception e) {
                    Log.i("onCreateConfig", e.toString());
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getBaseContext(), "Cannot connect to LED",
                        Toast.LENGTH_LONG).show();
                init();
            }
        });
    }
    private void pushConfigToRemote(){
        // update settings
        EditText text_field_host = findViewById(R.id.config_host_field);
        EditText text_field_port = findViewById(R.id.config_port_field);
        ConfigurationManager.getConfiguration().local.address = text_field_host.getText().toString();
        ConfigurationManager.getConfiguration().local.port = Integer.valueOf(text_field_port.getText().toString());
        String json = Configuration.writeConfigToJSON();
        updateRemoteConfig(json);
    }
    private void init(){
        setContentView(R.layout.prefrences);
        EditText text_field_host = findViewById(R.id.config_host_field);
        EditText text_field_port = findViewById(R.id.config_port_field);
        EditText text_wakeup_len = findViewById(R.id.config_wakeup_seq_len);
        TextInputLayout text_color =  findViewById(R.id.config_color_field);
        TextInputLayout text_interpolation =  findViewById(R.id.config_interpolation_field);
        ImageButton color_more = findViewById(R.id.color_more);
        text_field_host.setText(String.valueOf(ConfigurationManager.getConfiguration().local.address));
        text_field_port.setText(String.valueOf(ConfigurationManager.getConfiguration().local.port));
        Presets active_preset;
        try{
            active_preset = ConfigurationManager.getConfiguration().getActiveProfile();
            text_wakeup_len.setText(String.valueOf(active_preset.wakeup_sequence_len));
            //text_color.getEditText().setText(String.valueOf(active_preset.color));
            String[] options = ConfigurationManager.getConfiguration().remote.colors.keySet().toArray(new String[0]);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    R.layout.dropdown_menu_item, // simple dropdown item layout
                    options
            );

            AutoCompleteTextView autoCompleteTextView = findViewById(R.id.config_color_text_field);
            autoCompleteTextView.setAdapter(adapter);

        } catch (Resources.NotFoundException e){
            text_wakeup_len.setText("-");
            text_color.getEditText().setText("None");
            text_interpolation.getEditText().setText("None");
        }

        text_wakeup_len.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialTimePicker picker =
                        new MaterialTimePicker
                                .Builder()
                                .setTimeFormat(TimeFormat.CLOCK_24H)
                                .setHour(0)
                                .setMinute(10)
                                .setTitleText("Select length of wakeup sequence")
                                .setInputMode(INPUT_MODE_CLOCK)
                                .build();
                picker.addOnPositiveButtonClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ConfigurationManager.getConfiguration().getActiveProfile().wakeup_sequence_len = picker.getMinute();
                        text_wakeup_len.setText(String.valueOf(ConfigurationManager.getConfiguration().getActiveProfile().wakeup_sequence_len));
                    }
                });
                picker.show(getSupportFragmentManager(), "time_picker");
            }
        });
        color_more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

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

        ImageButton save = findViewById(R.id.settings_menu_edit_button_save);
        save.setOnClickListener(view -> {
            pushConfigToRemote();
        });

        // Return OK to system
        /*
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0,
                new OnBackInvokedCallback() {
                    @Override
                    public void onBackInvoked() {
                        // update settings
                        EditText text_field_host = findViewById(R.id.config_host_field);
                        EditText text_field_port = findViewById(R.id.config_port_field);
                        ConfigurationManager.getConfiguration().local.address = text_field_host.getText().toString();
                        ConfigurationManager.getConfiguration().local.port = Integer.valueOf(text_field_port.getText().toString());
                        String json = Configuration.writeConfigToJSON();
                        updateRemoteConfig(json);
                        finish();
                    }
                });
        */

        color_more.setOnClickListener(v -> {
                // Initializing the popup menu and giving the reference as current context
                PopupMenu popupMenu = new PopupMenu(this, color_more);

                // Inflating popup menu from popup_menu.xml file
                popupMenu.getMenuInflater().inflate(R.menu.color_menu, popupMenu.getMenu());

                // Handling menu item click events
                popupMenu.setOnMenuItemClickListener(menuItem -> {
                    // Handling menu item click events
                        int itemId = menuItem.getItemId();
                        if (itemId == R.id.color_menu_add) {
                            Intent myIntent = new Intent(this, ColorEdit.class);
                            myIntent.putExtra("color_name", "");
                            myIntent.putExtra("color_menu_edit_button_0_color", "#000000");
                            myIntent.putExtra("color_menu_edit_button_1_color", "#000000");
                            myIntent.putExtra("color_menu_edit_button_2_color", "#000000");
                            myIntent.putExtra("color_menu_edit_button_3_color", "#000000" );
                            myIntent.putExtra("color_menu_edit_button_4_color", "#000000" );
                            startActivity(myIntent);
                            return true;
                        } else if (itemId == R.id.color_menu_edit) {
                            Intent myIntent = new Intent(this, ColorEdit.class);
                            AutoCompleteTextView color_profile = findViewById(R.id.config_color_text_field);
                            String profile = color_profile.getText().toString();
                            Log.i("Configuration", profile  + " was selected");
                            Map<String, List<String>> colors = ConfigurationManager.getConfiguration().remote.colors;
                            if (!colors.containsKey(profile)){
                                Log.i("Configuration", "Profile " + profile + " does not exist");
                                return false;
                            }
                            List<String> selected_color = colors.get(profile);
                            myIntent.putExtra("color_name", profile);
                            if (selected_color.size() < 5){
                                Log.i("Configuration", "Profile " + profile + " has less than 5 colors");
                                return false;
                            }
                            myIntent.putExtra("color_menu_edit_button_0_color", selected_color.get(0));
                            myIntent.putExtra("color_menu_edit_button_1_color", selected_color.get(1));
                            myIntent.putExtra("color_menu_edit_button_2_color", selected_color.get(2));
                            myIntent.putExtra("color_menu_edit_button_3_color", selected_color.get(3));
                            myIntent.putExtra("color_menu_edit_button_4_color", selected_color.get(4));
                            startActivity(myIntent);
                            // Handle edit action
                            return true;
                        } else {
                            return false;
                        }
                    });
            popupMenu.show();
        });
        setResult(RESULT_OK, resultValue);
    }

    public void updateRemoteConfig(String json){
        RESTWakeupInterface apiClient = getRestClient();
        Call<ResponseBody> resp = apiClient.RestWriteConfig(Config.createConfig(json));
        resp.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.i("updateConfig", "done");
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("updateConfig", t.toString());
            }
        });

    }
}
