package com.bangtran.comclient;

import android.util.Log;


import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class SocketConnection extends WebSocketListener{
    private OkHttpClient client;
    private WebSocket ws;
    private SocketConnectionListener connectionListener;

    private  String serverUrl;
    private boolean connected;
    private boolean connecting;
    private boolean disconnectManual;

    private final int reconnectInternal = 3500; // milisecond
    private final int connectTimeout = 5000; // milisecond

    public SocketConnection(String url, SocketConnectionListener listener) {
        this.connectionListener = listener;
        this.serverUrl = url;
        client = new OkHttpClient.Builder().connectTimeout(5000, TimeUnit.MILLISECONDS).build();
        connected = false;
        connecting = false;
    }
    public synchronized void connect(){
        if (connecting){
            Log.d("SocketConnection", "Has connecting...");
            return;
        }
        if (connected) {
            Log.d("SocketConnection", "Has connected!");
            return;
        }
        Request request = new Request.Builder().url(serverUrl).build();
        ws = client.newWebSocket(request, this);
        Log.d("SocketConnection", "Connecting... "); //  + client.connectionPool().connectionCount() + " - " + client.connectionPool().idleConnectionCount()
    }
    public void reconnect(){
        try {
            Thread.sleep(reconnectInternal);
            Log.d("SocketConnection", "try to reconnect...");
            connect();
        } catch (InterruptedException e) {
            Log.e("SocketConnection", "reconnect " + e.toString());
        }
    }
    public void disconnect(){
        disconnectManual = true;
        this.ws.close(1000, "user disconnect");
    }
    public void send(JSONObject packet){
        ws.send(packet.toString());
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        connected = true;
        connecting = false;
        connectionListener.onConnect();
        Log.d("SocketConnection", "onOpen... ");
    }
    @Override
    public void onMessage(WebSocket webSocket, String text) {
        try {
            JSONObject data = new JSONObject(text);
            if (data != null)
                connectionListener.onMessage(data);
        } catch (JSONException e) {
            Log.d("SocketConnection" , "onMessage: " + e.toString());
        }
    }
    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        Log.d("SocketConnection", "onClosing");
    }
    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        Log.d("SocketConnection", "onClosed: " + code + " - reason: " + reason);
        if (connected) {
            connected = false;
            connectionListener.onDisconnect(!disconnectManual);
        }
    }
    @Override
    public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
        Log.e("SocketConnection", "onFailure " + t.getMessage());
        ws.close(1000, "timeout");
        ws.cancel();
        if (connected) {
            connected = false;
            connectionListener.onDisconnect(true);
        }
        connecting = false;
        ComClient.executor.execute(() -> {
            reconnect();
        });
    }
}
