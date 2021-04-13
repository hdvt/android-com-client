package com.bangtran.comapp;

import android.app.NotificationManager;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

public class MyFirebaseMessagingService extends FirebaseMessagingService {


    @Override
    public void onNewToken(@NonNull String s) {
        Log.d("Firebase", "New token: " + s);
        super.onNewToken(s);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            Log.d("Firebase", "Message data payload: " + remoteMessage.getData());
            String pushFromCom = remoteMessage.getData().get("comPushNotification");
            if (pushFromCom != null) {
                if (MainActivity.client == null || Common.isAppInBackground) {
                    try {
                        JSONObject jsonObject = new JSONObject(remoteMessage.getData().get("data"));
                        String callStatus = jsonObject.optString("callStatus", null);
                        String callId = jsonObject.optString("callId", null);
                        String callerId = jsonObject.getString("callerId");
                        if (callId != null && callStatus != null) {
                            switch (callStatus) {
                                case "started":
                                    //make a notification when app in background or killed
                                    Notification.notifyIncomingCall(getApplicationContext(), callerId);
                                    break;
                                case "ended":
                                    //remove notification
                                    NotificationManager nm = (NotificationManager) getSystemService
                                            (NOTIFICATION_SERVICE);
                                    if (nm != null) {
                                        nm.cancel(44448888);
                                    }
                                    break;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}