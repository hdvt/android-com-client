package com.bangtran.comclient;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

public class ComClient implements SocketConnectionListener {
    private Context appContext;
    private String serverURL;
    private SocketConnection socketConnection;
    private String accessToken;
    private String session_id;
    private String user_id;
    private ConcurrentHashMap<Integer, RequestCallback> requestCallbacks;
    int currentRequestId;
    ComConnectionListener connectionListener;

    public ComClient(Context appContext) {
        this.appContext = appContext;
        this.serverURL = "ws://192.168.42.10:8080";
        socketConnection = new SocketConnection(serverURL, this);
        session_id = null;
        user_id = null;
        requestCallbacks = new ConcurrentHashMap<Integer, RequestCallback>();
        currentRequestId = 0;
    }

    @Override
    public void onConnect() {
        Log.d("ComClient", "socket connected");
        JSONObject packet = new JSONObject();
        try {
            packet.put("event", "authen");
            JSONObject body = new JSONObject();
            body.put("token", accessToken);
            packet.put("body", body);
            this.sendMessage(packet, new RequestCallback() {
                @Override
                public void onSuccess(JSONObject data) {
                    Log.i("ComClient", "Authen success" + data.toString());
                    connectionListener.onConnectionConnected(ComClient.this);
                }

                @Override
                public void onError(JSONObject error) {
                    Log.e("ComClient", "Authen error" + error.toString());
                }
            });
        } catch (JSONException e) {
            Log.e("ComClient", e.getMessage());
        }

    }

    @Override
    public void onDisconnect() {

    }

    @Override
    public void onError(JSONObject error) {

    }

    @Override
    public void onMessage(JSONObject packet) {
        Log.i("ComClient", "onMessage: " + packet.toString());
        try {
            if (packet.getString("name").equalsIgnoreCase("event")){

            }
            else if (!packet.isNull("request_id")) {
                int request_id = packet.getInt("request_id");
                if (requestCallbacks.containsKey(request_id)){
                    RequestCallback callback = requestCallbacks.get(request_id);
                    requestCallbacks.remove(request_id);
                    if (packet.getString("name").equalsIgnoreCase("success")){
                        callback.onSuccess(packet.getJSONObject("data"));
                    }
                    else {
                        callback.onSuccess(packet.getJSONObject("error"));
                    }
                }
            }
        } catch (JSONException e) {
            Log.e("ComClient", e.getMessage());
        }
    }

    public void connect(String accessToken){
        this.accessToken = accessToken;
        socketConnection.connect();
    }

    public void disconnect(){

    }

    public void setConnectionListener(ComConnectionListener listener){
        connectionListener = listener;
    }

    public void sendMessage(JSONObject packet, RequestCallback callback){
        int request_id = ++currentRequestId;
        requestCallbacks.put(request_id, callback);
        try {
            packet.put("request_id", request_id);
            socketConnection.send(packet);
        } catch (JSONException e) {
            Log.e("ComClient", e.getMessage());
        }
    }
}
