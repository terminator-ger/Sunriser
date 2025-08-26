package com.example.sunriser;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class InterpolationEdit extends AppCompatActivity {
    List<SeekBar> seekBars = new ArrayList<SeekBar>();
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }
    private void init() {
        setResult(RESULT_CANCELED);
        setContentView(R.layout.interpolation_edit);
        Intent myIntent = getIntent(); // gets the previously created intent

        int[] seekbars = {
                R.id.seekBar_0,
                R.id.seekBar_1,
                R.id.seekBar_2,
                R.id.seekBar_3,
                R.id.seekBar_4,
        };

        for (int idx = 0; idx < seekbars.length; idx++) {
            SeekBar seekBar = findViewById(seekbars[idx]);
            seekBar.setProgress((int)(myIntent.getFloatExtra("interpolation_" + idx + "_value", 0.0f) * 100.0f));
            seekBars.add(seekBar);
        }
        ImageButton save = findViewById(R.id.interpolation_menu_edit_button_save);
        TextInputLayout edit_name = findViewById(R.id.interpolation_menu_edit_name);
        // set default name
        edit_name.getEditText().setText(myIntent.getStringExtra("interpolation_name"));

        save.setOnClickListener(view -> {
            String name = edit_name.getEditText().getText().toString();
            if (name.isEmpty()){
                edit_name.setError("Interpolation profile name is missing!");
                return;
            }
            List<Float> interpolation_scheme = new ArrayList<>();
            for (int idx = 0; idx < seekbars.length; idx++) {
                SeekBar seek = findViewById(seekbars[idx]);
                int progress = seek.getProgress();
                interpolation_scheme.add(Float.valueOf(progress/100));
            }
            ConfigurationManager.getConfiguration().remote.gradient.put(
                    name, interpolation_scheme
            );
            finish();
        });
    }
}
