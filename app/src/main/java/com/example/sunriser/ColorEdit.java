package com.example.sunriser;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.skydoves.colorpickerview.ColorEnvelope;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.util.ArrayList;
import java.util.List;

public class ColorEdit extends AppCompatActivity {
    List<Button> buttons = new ArrayList<>();
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }
    private void init() {
        setResult(RESULT_CANCELED);
        setContentView(R.layout.color_edit);
        Intent myIntent = getIntent(); // gets the previously created intent

        int[] button_ids = {
                R.id.color_menu_edit_button_0,
                R.id.color_menu_edit_button_1,
                R.id.color_menu_edit_button_2,
                R.id.color_menu_edit_button_3,
                R.id.color_menu_edit_button_4
        };

        for (int idx = 0; idx < button_ids.length; idx++) {
            Button btn = findViewById(button_ids[idx]);
            String bg_color = myIntent.getStringExtra(
                    "color_menu_edit_button_" + idx + "_color"
            );
            btn.setBackgroundColor(Color.parseColor(bg_color));
            btn.setOnClickListener(view -> {
                new ColorPickerDialog.Builder(this)
                        .setTitle("ColorPicker Dialog")
                        .setPreferenceName("MyColorPickerDialog")
                        .setPositiveButton(getString(R.string.confirm),
                                new ColorEnvelopeListener() {
                                    @Override
                                    public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                                        btn.setBackgroundColor(Color.parseColor("#"+envelope.getHexCode()));
                                    }
                                })
                        .setNegativeButton(getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                })
                        .attachAlphaSlideBar(false) // the default value is true.
                        .attachBrightnessSlideBar(true)  // the default value is true.
                        .setBottomSpace(12) // set a bottom space between the last slidebar and buttons.
                        .show();

            });
            buttons.add(btn);
        }
        ImageButton save = findViewById(R.id.color_menu_edit_button_save);
        TextInputLayout edit_name = findViewById(R.id.color_menu_edit_name);
        // set default name
        edit_name.getEditText().setText(myIntent.getStringExtra("color_name"));
        boolean is_connected = myIntent.getBooleanExtra("is_connected", false);
        save.setEnabled(is_connected);
        save.setVisibility(is_connected ? ImageButton.VISIBLE : ImageButton.INVISIBLE);

        save.setOnClickListener(view -> {
            String name = edit_name.getEditText().getText().toString();
            if (name.isEmpty()){
                edit_name.setError("Color profile name is missing!");
                return;
            }
            List<String> colors = new ArrayList<>();
            for (int idx = 0; idx < buttons.size(); idx++) {
                ColorStateList colorStateList = buttons.get(0).getBackgroundTintList();
                int colorInt = colorStateList.getDefaultColor();
                String hexColor = String.format("#%08X", colorInt);
                colors.add(hexColor);
            }
            ConfigurationManager.getConfiguration().remote.colors.put(
                    name, colors
            );
            finish();
        });
    }
}
