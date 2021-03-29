package com.bangtran.comapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.bangtran.comclient.ComCall;
import com.bangtran.comclient.ComClient;
import com.bangtran.comclient.ComError;
import com.bangtran.comclient.utils.Utils;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static ComClient client;
    private String to;
    public static Map<String, ComCall> callsMap = new HashMap<>();
    private String accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY2NvdW50X2lkIjozMjYsImxvZ2luX25hbWUiOiIwMzg3NTEwMTg3Iiwicm9sZSI6IlBBVElFTlQiLCJzdGF0ZSI6IkFDVElWRSIsImlhdCI6MTYxNTg4MjIyNywiZXhwIjoxNzAyMjgyMjI3fQ.z9MWafPQ36y4g2wvRHC5mBHtlhdyao61qj2j-DWP9Ls"; // replace your access token here.
    private String accessToken2 = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhY2NvdW50X2lkIjoxOTAsImxvZ2luX25hbWUiOiIwMzk3NzA2MjI4Iiwicm9sZSI6Ik5VUlNFIiwic3RhdGUiOiJBQ1RJVkUiLCJpYXQiOjE2MTU4ODM0ODQsImV4cCI6MTcwMjI4MzQ4NH0.Sx6uQvk5Fyh24nmpjTj-1DpoeiTgbtJq6FYFxr4fczE"; // replace your access token here.

    private EditText etTo;
    private TextView tvUserId;
    private ProgressDialog progressDialog;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private final String PREF_NAME = "com.stringee.onetoonecallsample";
    private final String IS_TOKEN_REGISTERED = "is_token_registered";
    private final String TOKEN = "token";

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
            public void onConnectionConnected(final ComClient client) {

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
            public void onConnectionDisconnected(ComClient client, boolean reconnecting) {
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
            public void onIncommingCall(ComCall call) {
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
            public void onConnectionError(ComClient client, ComError error) {

            }

//            @Override
//            public void onConnectionDisconnected(ComClient client) {
//
//            }
//
//            @Override
//            public void onConnectionError(ComClient client, JSONObject error) {
//
//            }


        });
        client.connect(accessToken);
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