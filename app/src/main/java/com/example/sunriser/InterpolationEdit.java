package com.example.sunriser;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class InterpolationEdit extends AppCompatActivity {
    List<Slider> sliders = new ArrayList<Slider>();
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }
    private void init() {
        setResult(RESULT_CANCELED);
        setContentView(R.layout.interpolation_edit);
        Intent myIntent = getIntent(); // gets the previously created intent

        int[]  slider_ids = {
                R.id.seekBar_0,
                R.id.seekBar_1,
                R.id.seekBar_2,
                R.id.seekBar_3,
                R.id.seekBar_4,
        };

        for (int idx = 0; idx < slider_ids.length; idx++) {
            Slider slider = findViewById( slider_ids[idx]);
            slider.setValue(myIntent.getFloatExtra("interpolation_" + idx + "_value", 0.0f));
            sliders.add(slider);
        }
        ImageButton save = findViewById(R.id.interpolation_menu_edit_button_save);
        TextInputLayout edit_name = findViewById(R.id.interpolation_menu_edit_name);
        // set default name
        edit_name.getEditText().setText(myIntent.getStringExtra("interpolation_name"));
        boolean is_connected = Boolean.parseBoolean(myIntent.getStringExtra("is_connected"));
        save.setEnabled(is_connected);
        save.setVisibility(is_connected ? ImageButton.VISIBLE : ImageButton.INVISIBLE);

        save.setOnClickListener(view -> {
            String name = edit_name.getEditText().getText().toString();
            if (name.isEmpty()){
                edit_name.setError("Interpolation profile name is missing!");
                return;
            }
            List<Float> interpolation_scheme = new ArrayList<>();
            for (int idx = 0; idx <  slider_ids.length; idx++) {
                Slider slider = findViewById( slider_ids[idx]);
                float progress = slider.getValue();
                interpolation_scheme.add(Float.valueOf(progress));
            }
            ConfigurationManager.getConfiguration().remote.gradient.put(
                    name, interpolation_scheme
            );
            finish();
        });
    }
}
