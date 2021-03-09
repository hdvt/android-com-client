package com.bangtran.comclient;

import android.util.Log;

import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
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


    public SocketConnection(String url, SocketConnectionListener listener) {
        this.connectionListener = listener;
        this.serverURL = url;
        client = new OkHttpClient();

    }
    public void connect(){
        Log.d("SocketConnection" , "Connecting...");

        Request request = new Request.Builder().url(serverURL).build();
        ws = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
//                super.onOpen(webSocket, response);
                connectionListener.onConnect();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
//                super.onMessage(webSocket, text);
                try {
                    connectionListener.onMessage(new JSONObject(text));
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
                Log.d("SocketConnection", "onClosed");

                connectionListener.onDisconnect();
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
//                super.onFailure(webSocket, t, response);
                Log.e("SocketConnection", t.getMessage());
            }
        });
        client.dispatcher().executorService().shutdown();
    }
    public void reconnect(){

    }
    public void disconnect(){

    }
    public void send(JSONObject packet){
        ws.send(packet.toString());
    }
}
