package com.bangtran.comapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bangtran.comclient.ComCall;
import com.bangtran.comclient.ComCallback;
import com.bangtran.comclient.ComClient;
import com.bangtran.comclient.ComError;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, LifecycleObserver {

    public static ComClient client;
    private String to;
    public static Map<String, ComCall> callsMap = new HashMap<>();
    private String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3MDM3NTg3MzYsInVzZXJfaWQiOiIxOTMiLCJpYXQiOjE2MTczNTg3MzZ9.17yHEnH9KqjUQYCigWfjfyYFO9fVPm1BMgR_PMRPb8k"; // replace your access token here.
    private String accessToken2 = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE3MDM3NTg3MzYsInVzZXJfaWQiOiIxOTkiLCJpYXQiOjE2MTczNTg3MzZ9.kRih8J4bI9PQeNPOeb9XXUr767AWu8msJ1kVrzBscGM"; // replace your access token here.

    private EditText etTo;
    private TextView tvUserId;
    private ProgressDialog progressDialog;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private final String PREF_NAME = "com.bangtran.comapp";
    private final String IS_TOKEN_REGISTERED = "is_token_registered";
    private final String TOKEN = "token";

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onAppBackgrounded() {
        Log.d("AppLifecycle", "App in background");
        Common.isAppInBackground = true;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onAppForegrounded() {
        Log.d("AppLifecycle", "App in foreground");
        Common.isAppInBackground = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvUserId = (TextView) findViewById(R.id.tv_userid);

        Button btnVoiceCall = (Button) findViewById(R.id.btn_voice_call);
        btnVoiceCall.setOnClickListener(this);
        Button btnVideoCall = (Button) findViewById(R.id.btn_video_call);
        btnVideoCall.setOnClickListener(this);
        etTo = (EditText) findViewById(R.id.et_to);

        Button btnUnregister = (Button) findViewById(R.id.btn_unregister);
        btnUnregister.setOnClickListener(this);

        progressDialog = ProgressDialog.show(this, "", "Connecting...");
        progressDialog.setCancelable(true);
        progressDialog.show();

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();

        initAndConnectStringee();
    }

    public void initAndConnectStringee() {
        client = new ComClient(this);
        client.setConnectionListener(new ComClient.ComConnectionListener() {
            @Override
            public void onComConnectionConnected(final ComClient client) {
                boolean isTokenRegistered = sharedPreferences.getBoolean(IS_TOKEN_REGISTERED, false);
                if (!isTokenRegistered) {
                    FirebaseMessaging.getInstance().getToken()
                            .addOnCompleteListener(new OnCompleteListener<String>() {
                                @Override
                                public void onComplete(@NonNull Task<String> task) {
                                    if (!task.isSuccessful()) {
                                        Log.w("MainActivity", "Fetching FCM registration token failed", task.getException());
                                        return;
                                    }

                                    // Get new FCM registration token
                                    String token = task.getResult();

                                    client.registerPushToken(token, new ComCallback() {
                                        @Override
                                        public void onSuccess() {
                                            Log.d("MainActivity", "Register push token successfully.");
                                            editor.putBoolean(IS_TOKEN_REGISTERED, true);
                                            editor.putString(TOKEN, token);
                                            editor.commit();
                                        }

                                        @Override
                                        public void onError(ComError error) {
                                            Log.d("MainActivity", "Register push token unsuccessfully: " + error.getMessage());
                                        }
                                    });
                                }
                            });



                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        tvUserId.setText("Connected as: " + client.getUserId());
                        Utils.reportMessage(MainActivity.this, "Client is connected.");
//                        client.disconnect();
                    }
                });
            }

            @Override
            public void onComConnectionDisconnected(ComClient client, boolean reconnecting) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvUserId.setText("");
                        Utils.reportMessage(MainActivity.this, "Client is disconnected.");
//                        client.disconnect();
                    }
                });
            }


            @Override
            public void onComIncommingCall(ComCall call) {
                JSONObject info = new JSONObject();
                try {
                    info.put("data", "test");
                    client.sendCustomMessage("193", info, new ComCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d("MainActivity", "sendCustomMessage ok");
                        }
                        @Override
                        public void onError(ComError error) {
                            Log.d("MainActivity", "sendCustomMessage error " + error.getMessage());

                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        callsMap.put(call.getCallId(), call);
                        Intent intent = new Intent(MainActivity.this, IncomingCallActivity.class);
                        intent.putExtra("call_id", call.getCallId());
                        startActivity(intent);
                    }
                });
            }

            @Override
            public void onComConnectionError(ComClient client, ComError error) {

            }

            @Override
            public void onCustomMessage(String from, JSONObject msg) {
                Log.d("onCustomMessage", "from " + from + ": " + msg.toString());
            }
        });
        client.connect(accessToken2);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_voice_call:
                to = etTo.getText().toString();
                if (to.trim().length() > 0) {
                    if (client.isConnected()) {
                        Intent intent = new Intent(this, OutgoingCallActivity.class);
                        intent.putExtra("from", client.getUserId());
                        intent.putExtra("to", to);
                        intent.putExtra("is_video_call", false);
                        startActivity(intent);
                    } else {
                        Utils.reportMessage(this, "Client session not connected");
                    }
                }
                break;
            case R.id.btn_video_call:
                to = etTo.getText().toString();
                if (to.trim().length() > 0) {
                    if (client.isConnected()) {
                        Intent intent = new Intent(this, OutgoingCallActivity.class);
                        intent.putExtra("from", client.getUserId());
                        intent.putExtra("to", to);
                        intent.putExtra("is_video_call", true);
                        startActivity(intent);
                    } else {
                        Utils.reportMessage(this, "Stringee session not connected");
                    }
                }
                break;

        }
    }
}