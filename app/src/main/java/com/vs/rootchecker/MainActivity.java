package com.vs.rootchecker;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import eu.chainfire.libsuperuser.Shell;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button verify_root = findViewById(R.id.verify_root);
        TextView root_status = findViewById(R.id.root_status);
        TextView android_version = findViewById(R.id.android_version);
        TextView device = findViewById(R.id.device);

        String version_text = "Android Version : " + android.os.Build.VERSION.RELEASE;
        android_version.setText(version_text);

        String device_model = "Device : " + Build.MODEL;
        device.setText(device_model);

        verify_root.setOnClickListener(v -> {
            if (Shell.SU.available()) {
                root_status.setText("Your Phone Is Rooted");
                root_status.setTextSize(24f);
                root_status.setTextColor(Color.parseColor("#00FF00"));
            } else {
                root_status.setText("Phone Is Not Rooted");
                root_status.setTextSize(24f);
                root_status.setTextColor(Color.parseColor("#FF0000"));
            }
        });
    }
}