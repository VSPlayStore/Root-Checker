package com.vs.rootchecker;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.safetynet.SafetyNet;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;

import eu.chainfire.libsuperuser.Shell;

public class MainActivity extends AppCompatActivity {

    private final Random mRandom = new SecureRandom();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, initializationStatus -> {
        });

        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        Button verify_root = findViewById(R.id.verify_root);
        Button verify_safety_net = findViewById(R.id.verify_safety_net);
        TextView root_status = findViewById(R.id.root_status);
        TextView cts = findViewById(R.id.cts);
        TextView android_version = findViewById(R.id.android_version);
        TextView basic_int = findViewById(R.id.basic_int);
        TextView device = findViewById(R.id.device);
        ProgressBar progressBar = findViewById(R.id.progressBar);

        String version_text = "Android Version : " + android.os.Build.VERSION.RELEASE;
        android_version.setText(version_text);

        String device_model = "Device : " + Build.MODEL;
        device.setText(device_model);

        verify_root.setOnClickListener(v -> {
            String phone_rooted = "Your Phone Is Rooted";
            String phone_not_rooted = "Phone Is Not Rooted";
            if (Shell.SU.available()) {
                root_status.setText(phone_rooted);
                root_status.setTextSize(24f);
                root_status.setTextColor(Color.parseColor("#00FF00"));
            } else {
                root_status.setText(phone_not_rooted);
                root_status.setTextSize(24f);
                root_status.setTextColor(Color.parseColor("#FF0000"));
            }
        });

        String API_KEY = "AIzaSyAZXkw0rNIrz0wTgQ060ut_6GiAsVKgKkM";
        String nonceData = "Safety Net Sample: " + System.currentTimeMillis();
        byte[] nonce = getRequestNonce(nonceData);

        verify_safety_net.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            SafetyNet.getClient(this).attest(nonce, API_KEY)
                    .addOnSuccessListener(this,
                            response -> {
                                try {
                                    String pass = "Pass";
                                    String fail = "Fail";
                                    JSONObject obj = new JSONObject(decodeJws(response.getJwsResult()));
                                    if (Boolean.parseBoolean(obj.get("ctsProfileMatch").toString())) {
                                        cts.setText(pass);
                                        cts.setTextColor(Color.parseColor("#00FF00"));
                                    } else {
                                        cts.setText(fail);
                                        cts.setTextColor(Color.parseColor("#FF0000"));
                                    }

                                    if (Boolean.parseBoolean(obj.get("basicIntegrity").toString())) {
                                        basic_int.setText(pass);
                                        basic_int.setTextColor(Color.parseColor("#00FF00"));
                                    } else {
                                        basic_int.setText(fail);
                                        basic_int.setTextColor(Color.parseColor("#FF0000"));
                                    }

                                    progressBar.setVisibility(View.INVISIBLE);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            })
                    .addOnFailureListener(this, e -> {
                        if (e instanceof ApiException) {
                            ApiException apiException = (ApiException) e;
                            Log.e("TAG", apiException.getMessage());
                        } else {
                            Log.e("TAG", "Error: " + e.getMessage());
                        }
                    });
        });
    }

    public String decodeJws(String jwsResult) {
        if (jwsResult == null) {
            return null;
        }
        final String[] jwtParts = jwsResult.split("\\.");
        if (jwtParts.length == 3) {
            return new String(Base64.decode(jwtParts[1], Base64.DEFAULT));
        } else {
            return null;
        }
    }

    private byte[] getRequestNonce(String data) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byte[] bytes = new byte[24];
        mRandom.nextBytes(bytes);
        try {
            byteStream.write(bytes);
            byteStream.write(data.getBytes());
        } catch (IOException e) {
            return null;
        }

        return byteStream.toByteArray();
    }
}