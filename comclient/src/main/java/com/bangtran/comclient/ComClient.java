package com.bangtran.comclient;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.PeerConnectionFactory;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ComClient implements SocketConnectionListener {
    public static final ExecutorService executor = Executors.newSingleThreadExecutor();
    int currentRequestId;
    ComConnectionListener connectionListener;
    boolean connected;
    private Context appContext;
    private String serverURL;
    private SocketConnection socketConnection;
    private String accessToken;
    private String sessionId;
    private String userId;
    private ConcurrentHashMap<Integer, RequestCallback> requestCallbacks;
    private Vector<ComCall> comCalls;

    public ComClient(Context appContext) {
        this.appContext = appContext;
        this.serverURL = "ws://192.168.42.192:8080";
        socketConnection = new SocketConnection(serverURL, this);
        sessionId = null;
        userId = null;
        requestCallbacks = new ConcurrentHashMap<Integer, RequestCallback>();
        comCalls = new Vector<ComCall>();
        currentRequestId = 0;
        connected = false;
        String fieldTrials = "WebRTC-IntelVP8/Enabled/";
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(appContext)
                        .setFieldTrials(fieldTrials)
                        .setEnableInternalTracer(true)
                        .createInitializationOptions());
    }

    @Override
    public void onConnect() {
        executor.execute(() -> {
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
                        try {
                            userId = data.getString("user_id");
                            sessionId = data.getString("session_id");
                            connected = true;
                        } catch (JSONException e) {
                            Log.e("ComClient", e.toString());
                        }
                        connectionListener.onConnectionConnected(ComClient.this);
                    }

                    @Override
                    public void onError(ComError error) {
                        Log.e("ComClient", "Authen error" + error.toString());
                    }
                });
            } catch (JSONException e) {
                Log.e("ComClient", e.getMessage());
            }
        });

    }

    @Override
    public void onDisconnect(boolean reconnecting) {
        this.connected = false;
        this.connectionListener.onConnectionDisconnected(this, reconnecting);
    }

    @Override
    public void onError(ComError error) {

    }

    @Override
    public void onMessage(JSONObject packet) {
        Log.i("ComClient", "onMessage: " + packet.toString());
        try {
            if (packet.getString("name").equalsIgnoreCase("event")) {
                JSONObject event = packet.getJSONObject("event");
                String eventName = event.getString("name");
                JSONObject data = event.getJSONObject("data");
                switch (eventName) {
                    case "call_start": {
                        ComCall newCall = new ComCall(this, data.getString("caller_user_id"), data.getString("callee_user_id"));
                        newCall.setCallID(data.getString("call_id"));
                        newCall.setIncomingCall(true);
                        connectionListener.onIncommingCall(newCall);
                        break;
                    }
                    case "call_sdp":
                    case "call_state":
                    case "call_stop": {
                        ComCall call = getCall(data.getString("call_id"));
                        if (call != null) {
                            call.handleEvent(eventName, data);
//                            call.onRemoteSDP(data.getJSONObject("jsep"));
                        }
                        break;
                    }
                }
            } else if (!packet.isNull("request_id")) {
                int request_id = packet.getInt("request_id");
                if (requestCallbacks.containsKey(request_id)) {
                    RequestCallback callback = requestCallbacks.get(request_id);
                    requestCallbacks.remove(request_id);
                    if (packet.getString("name").equalsIgnoreCase("success")) {
                        callback.onSuccess(packet.getJSONObject("data"));
                    } else {
                        JSONObject error = packet.getJSONObject("error");
                        callback.onError(new ComError(error.getInt("error_code"), error.getString("message")));
                    }
                }
            }
        } catch (JSONException e) {
            Log.e("ComClient", e.getMessage());
        }
    }

    public String getUserId() {
        return userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public boolean isConnected() {
        return connected;
    }

    public void connect(String accessToken) {
        this.accessToken = accessToken;
        executor.execute(() -> {
            socketConnection.connect();
        });
    }

    public void disconnect() {
        this.socketConnection.disconnect();
    }

    public void setConnectionListener(ComConnectionListener listener) {
        connectionListener = listener;
    }

    public void sendMessage(JSONObject packet, RequestCallback callback) {
        int request_id = ++currentRequestId;
        requestCallbacks.put(request_id, callback);
        try {
            packet.put("request_id", request_id);
            socketConnection.send(packet);
        } catch (JSONException e) {
            Log.e("ComClient", e.getMessage());
        }
    }

    public void addNewCall(ComCall call) {
        this.comCalls.add(call);
    }

    public void removeCall(ComCall call){
        this.comCalls.remove(call);
    }

    public ComCall getCall(String callId) {
        for (ComCall comCall : comCalls) {
            if (comCall.getCallId().equalsIgnoreCase(callId))
                return comCall;
        }
        return null;
    }

    public interface ComConnectionListener {
        void onConnectionConnected(final ComClient client);

        void onConnectionDisconnected(final ComClient client, boolean reconnecting);

        void onIncommingCall(ComCall call);

        void onConnectionError(final ComClient client, ComError error);
    }

}
