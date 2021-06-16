package com.bangtran.comclient;

import android.util.Log;

import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class SocketConnection{
    private OkHttpClient client;
    private WebSocket ws;
    private SocketConnectionListener connectionListener;
    private  String serverURL;
    private boolean connected;
    private boolean disconnectManual;
    private final int reconnectInternal = 3000; // milisecond
    private final int connectTimeout = 5000; // milisecond



    public SocketConnection(String url, SocketConnectionListener listener) {
        this.connectionListener = listener;
        this.serverURL = url;
        client = new OkHttpClient.Builder().connectTimeout(2000, TimeUnit.MILLISECONDS).pingInterval(1000, TimeUnit.MILLISECONDS).build();
    }
    public void connect(){
        Log.d("SocketConnection" , "Connecting...");
        Request request = new Request.Builder().url(serverURL).build();

        ws = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
//                super.onOpen(webSocket, response);
                Log.d("SocketConnection" , "onOpen...");
                connected = true;
                connectionListener.onConnect();
            }
            @Override
            public void onMessage(WebSocket webSocket, String text) {
//                super.onMessage(webSocket, text);
                try {
                    JSONObject data = new JSONObject(text);
                    if (data != null)
                        connectionListener.onMessage(data);
                } catch (JSONException e) {
                    Log.d("Error" , e.toString());
                }
            }
            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
//                super.onClosing(webSocket, code, reason);
                Log.d("SocketConnection", "onClosing");

            }
            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
//                super.onClosed(webSocket, code, reason);
                Log.d("SocketConnection", "onClosed: " + code + " - reason: " + reason);
                if (connected) {
                    connected = false;
                    connectionListener.onDisconnect(false);
                }
            }
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
//                super.onFailure(webSocket, t, response);
                Log.e("SocketConnection", "onFailure " + t.getMessage());
                ws.close(1000, "timeout");
                ws.cancel();
                if (connected) {
                    connected = false;
                    connectionListener.onDisconnect(true);
                }
                ComClient.executor.execute(() -> {
                    reconnect();
                });
            }
        });
//        client.dispatcher().executorService().shutdown();
    }
    public void reconnect(){
        try {
            Thread.sleep(reconnectInternal);
            Log.d("SocketConnection", "try to reconnecting...");
            connect();
        } catch (InterruptedException e) {
            Log.e("SocketConnection", "reconnect " + e.toString());        }
    }
    public void disconnect(){
        disconnectManual = true;
        this.ws.close(1000, "user disconnect");
    }
    public void send(JSONObject packet){
        ws.send(packet.toString());
    }
}
