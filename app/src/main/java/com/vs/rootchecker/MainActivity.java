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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.play.core.integrity.IntegrityManager;
import com.google.android.play.core.integrity.IntegrityManagerFactory;
import com.google.android.play.core.integrity.IntegrityTokenRequest;
import com.google.android.play.core.integrity.IntegrityTokenResponse;

import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwx.JsonWebStructure;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Random;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

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
        Button verify_play_integrity = findViewById(R.id.verify_play_integrity);
        TextView root_status = findViewById(R.id.root_status);
        TextView cts = findViewById(R.id.cts);
        TextView android_version = findViewById(R.id.android_version);
        TextView basic_int = findViewById(R.id.basic_int);
        TextView meets_basic_integrity = findViewById(R.id.meets_basic_integrity);
        TextView meets_device_integrity = findViewById(R.id.meets_device_integrity);
        TextView meets_strong_integrity = findViewById(R.id.meets_strong_integrity);
        TextView device = findViewById(R.id.device);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        ProgressBar play_integrity_progress = findViewById(R.id.play_integrity_progress);

        String pass = "Pass";
        String fail = "Fail";

        String redColorCode = "#FF0000";
        String greenColorCode = "#00FF00";

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
                root_status.setTextColor(Color.parseColor(greenColorCode));
            } else {
                root_status.setText(phone_not_rooted);
                root_status.setTextSize(24f);
                root_status.setTextColor(Color.parseColor(redColorCode));
            }
        });

        verify_safety_net.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            String API_KEY = "";
            String nonceData = "Safety Net Sample: " + System.currentTimeMillis();
            byte[] nonce = getRequestNonce(nonceData);

            if (nonce == null) {
                Toast.makeText(this, "Error Please Try again!", Toast.LENGTH_SHORT).show();
                return;
            }

            SafetyNet.getClient(this).attest(nonce, API_KEY)
                    .addOnSuccessListener(this,
                            response -> {
                                try {
                                    JSONObject obj = new JSONObject(decodeJws(response.getJwsResult()));
                                    if (Boolean.parseBoolean(obj.get("ctsProfileMatch").toString())) {
                                        cts.setText(pass);
                                        cts.setTextColor(Color.parseColor(greenColorCode));
                                    } else {
                                        cts.setText(fail);
                                        cts.setTextColor(Color.parseColor(redColorCode));
                                    }

                                    if (Boolean.parseBoolean(obj.get("basicIntegrity").toString())) {
                                        basic_int.setText(pass);
                                        basic_int.setTextColor(Color.parseColor(greenColorCode));
                                    } else {
                                        basic_int.setText(fail);
                                        basic_int.setTextColor(Color.parseColor(redColorCode));
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
                        Toast.makeText(this, "Error getting safety net status", Toast.LENGTH_SHORT).show();
                    });
        });
        verify_play_integrity.setOnClickListener(v -> {
            play_integrity_progress.setVisibility(View.VISIBLE);
            final String nonceData = "Safety Net Sample: " + System.currentTimeMillis();
            final String nonce = Base64.encodeToString(nonceData.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
            final String DECRYPTION_KEY = "";
            final String VERIFICATION_KEY = "";

            IntegrityManager integrityManager =
                    IntegrityManagerFactory.create(getApplicationContext());

            integrityManager
                    .requestIntegrityToken(IntegrityTokenRequest.builder()
                            .setNonce(nonce)
                            .build())
                    .addOnSuccessListener(
                            (OnSuccessListener<IntegrityTokenResponse>)
                                    response -> {
                                        String integrityToken = response.token();

                                        byte[] decryptionKeyBytes =
                                                Base64.decode(DECRYPTION_KEY, Base64.DEFAULT);

                                        // SecretKey
                                        SecretKey decryptionKey =
                                                new SecretKeySpec(
                                                        decryptionKeyBytes,
                                                        0,
                                                        decryptionKeyBytes.length,
                                                        "AES");

                                        byte[] encodedVerificationKey =
                                                Base64.decode(VERIFICATION_KEY, Base64.DEFAULT);

                                        // PublicKey
                                        PublicKey verificationKey = null;

                                        try {
                                            verificationKey = KeyFactory.getInstance("EC")
                                                    .generatePublic(new X509EncodedKeySpec(encodedVerificationKey));

                                        } catch (Exception e) {
                                            Log.e("Error", e.getMessage());
                                        }

                                        // JsonWebEncryption
                                        JsonWebEncryption jwe = null;
                                        try {
                                            jwe = (JsonWebEncryption) JsonWebStructure
                                                    .fromCompactSerialization(integrityToken);

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                        if (jwe == null) {
                                            Toast.makeText(this, "Error Please Try again!", Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        jwe.setKey(decryptionKey);

                                        String compactJws = null;

                                        try {
                                            compactJws = jwe.getPayload();
                                        } catch (Exception e) {
                                            Log.d("Error", e.getMessage());
                                        }

                                        if (compactJws == null) {
                                            Toast.makeText(this, "Error Please Try again!", Toast.LENGTH_SHORT).show();
                                            return;
                                        }


                                        // JsonWebSignature
                                        JsonWebSignature jws = null;

                                        try {
                                            jws = (JsonWebSignature) JsonWebStructure
                                                    .fromCompactSerialization(compactJws);
                                        } catch (Exception e) {
                                            Log.d("Error", e.getMessage());
                                        }

                                        if (jws == null) {
                                            Toast.makeText(this, "Error Please Try again!", Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        jws.setKey(verificationKey);

                                        // get the json human readable string
                                        String jsonPlainVerdict = "";

                                        try {
                                            jsonPlainVerdict = jws.getPayload();
                                        } catch (Exception e) {
                                            Log.d("Error", e.getMessage());
                                        }

                                        try {
                                            JSONObject obj = new JSONObject(jsonPlainVerdict);
                                            JSONObject deviceIntegrity = new JSONObject(obj.get("deviceIntegrity").toString());
                                            JSONArray deviceRecognitionVerdict = new JSONArray(deviceIntegrity.get("deviceRecognitionVerdict").toString());

                                            meets_basic_integrity.setText(fail);
                                            meets_basic_integrity.setTextColor(Color.parseColor(redColorCode));
                                            meets_device_integrity.setText(fail);
                                            meets_device_integrity.setTextColor(Color.parseColor(redColorCode));
                                            meets_strong_integrity.setText(fail);
                                            meets_strong_integrity.setTextColor(Color.parseColor(redColorCode));

                                            for (int i = 0; i < deviceRecognitionVerdict.length(); i++) {
                                                if (deviceRecognitionVerdict.get(i).toString().equals("MEETS_BASIC_INTEGRITY")) {
                                                    meets_basic_integrity.setText(pass);
                                                    meets_basic_integrity.setTextColor(Color.parseColor(greenColorCode));
                                                } else if (deviceRecognitionVerdict.get(i).toString().equals("MEETS_DEVICE_INTEGRITY")) {
                                                    meets_device_integrity.setText(pass);
                                                    meets_device_integrity.setTextColor(Color.parseColor(greenColorCode));
                                                } else if (deviceRecognitionVerdict.get(i).toString().equals("MEETS_STRONG_INTEGRITY")) {
                                                    meets_strong_integrity.setText(pass);
                                                    meets_strong_integrity.setTextColor(Color.parseColor(greenColorCode));
                                                }
                                            }
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        play_integrity_progress.setVisibility(View.INVISIBLE);
                                    })
                    .addOnFailureListener((OnFailureListener) ex -> {
                        Log.e("Error", ex.getMessage());
                        Toast.makeText(this, "Error getting play integrity status", Toast.LENGTH_SHORT).show();
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