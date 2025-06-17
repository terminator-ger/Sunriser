package com.example.sunriser;

import android.app.Activity;
import android.os.Bundle;

public class DebugActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set a simple view or even finish immediately
        finish(); // If you just want the debugger attached
    }
}
