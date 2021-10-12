package com.bangtran.comclient;

import android.util.Log;


import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;


public class SocketConnection {
    private SocketConnectionListener connectionListener;
    private Socket socket;
    private  String serverUrl;
    private boolean connected;
    private boolean disconnectManual;

    private final int reconnectInternal = 3500; // milisecond
    private final int connectTimeout = 5000; // milisecond

    public SocketConnection(String url, SocketConnectionListener listener) {
        this.connectionListener = listener;
        this.serverUrl = url;
        connected = false;
    }
    public void connect(){
        IO.Options opts = new IO.Options();
        opts.transports = new String[] { WebSocket.NAME };
//        IO.Options options = IO.Options.builder()
//                .setTransports(new String[] { WebSocket.NAME }).setReconnection(true)
//                .build();
        try {
            this.socket = IO.socket(this.serverUrl,opts);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        Log.d("SocketConnection", "Connecting... "); //  + client.connectionPool().connectionCount() + " - " + client.connectionPool().idleConnectionCount()

        this.socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d("SocketConnection", "Connected... "); //  + client.connectionPool().connectionCount() + " - " + client.connectionPool().idleConnectionCount()

                connected = true;
                connectionListener.onConnect();
            }
        });
        this.socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d("SocketConnection", "Disconnected... "); //  + client.connectionPool().connectionCount() + " - " + client.connectionPool().idleConnectionCount()

                if (connected) {
                    connected = false;
                    connectionListener.onDisconnect(!disconnectManual);
                }
            }
        });
        this.socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject data = (JSONObject) args[0];
                Log.e("SocketConnection", "onError " + data.toString());
//                if (connected) {
//                    connected = false;
//                    connectionListener.onDisconnect(true);
//                }
            }
        });
        this.socket.on("message", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject data = (JSONObject) args[0];
                Log.d("SocketConnection", "onMessage " + data.toString());
                if (data != null)
                    connectionListener.onMessage(data);
            }
        });
        this.socket.connect();
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
        this.socket.close();
    }
    public void send(JSONObject packet){
        try {
            socket.emit("command", packet.getString("event"), packet);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

//    @Override
//    public void onOpen(WebSocket webSocket, Response response) {
//        connected = true;
//        connecting = false;
//        connectionListener.onConnect();
//        Log.d("SocketConnection", "onOpen... ");
//    }
//    @Override
//    public void onMessage(WebSocket webSocket, String text) {
//        try {
//            JSONObject data = new JSONObject(text);
//            if (data != null)
//                connectionListener.onMessage(data);
//        } catch (JSONException e) {
//            Log.d("SocketConnection" , "onMessage: " + e.toString());
//        }
//    }
//    @Override
//    public void onClosing(WebSocket webSocket, int code, String reason) {
//        Log.d("SocketConnection", "onClosing");
//    }
//    @Override
//    public void onClosed(WebSocket webSocket, int code, String reason) {
//        Log.d("SocketConnection", "onClosed: " + code + " - reason: " + reason);
//        if (connected) {
//            connected = false;
//            connectionListener.onDisconnect(!disconnectManual);
//        }
//    }
//    @Override
//    public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
//        Log.e("SocketConnection", "onFailure " + t.getMessage());
//        ws.close(1000, "timeout");
//        ws.cancel();
//        if (connected) {
//            connected = false;
//            connectionListener.onDisconnect(true);
//        }
//        connecting = false;
//        ComClient.executor.execute(() -> {
//            reconnect();
//        });
//    }
}
