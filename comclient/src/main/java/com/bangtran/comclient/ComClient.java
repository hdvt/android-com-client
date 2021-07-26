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
    private static final String TAG = ComClient.class.getSimpleName();
    public static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private Context appContext;
    private ComConnectionListener connectionListener;
    private SocketConnection socketConnection;

    int currentRequestId;
    boolean connected;
    private String serverURL;
    private String accessToken;
    private String sessionId;
    private String userId;

    private ConcurrentHashMap<Integer, RequestCallback> requestCallbacks;
    private Vector<ComCall> comCalls;

    private final String endpointUrl = "ws://203.113.138.21:4417";

    public ComClient(Context appContext) {
        this.appContext = appContext;
        socketConnection = new SocketConnection(endpointUrl, this);
        connected = false;
        sessionId = null;
        userId = null;
        comCalls = new Vector<ComCall>();
        requestCallbacks = new ConcurrentHashMap<Integer, RequestCallback>();
        currentRequestId = 0;
    }
    @Override
    public void onConnect() {
        executor.execute(() -> {
            JSONObject packet = new JSONObject();
            try {
                if (this.sessionId == null) {
                    packet.put("event", "authen");
                    JSONObject body = new JSONObject();
                    body.put("token", accessToken);
                    packet.put("body", body);
                    this.sendMessage(packet, new RequestCallback() {
                        @Override
                        public void onSuccess(JSONObject data) {
                            Log.i(TAG, "authen success " + data.toString());
                            try {
                                userId = data.getString("user_id");
                                sessionId = data.getString("session_id");
                                connected = true;
                            } catch (JSONException e) {
                                Log.e(TAG, e.toString());
                            }
                            connectionListener.onComConnectionConnected(ComClient.this);
                        }

                        @Override
                        public void onError(ComError error) {
                            Log.e(TAG, "authen error " + error.toString());
                            connectionListener.onComConnectionError(ComClient.this, error);
                        }
                    });
                }
                else {
                    packet.put("event", "claim");
                    JSONObject body = new JSONObject();
                    body.put("session_id", this.sessionId);
                    packet.put("body", body);
                    this.sendMessage(packet, new RequestCallback() {
                        @Override
                        public void onSuccess(JSONObject data) {
                            Log.i(TAG, "claimed the session " + ComClient.this.sessionId + " sucessfully");
                            connected = true;
                            connectionListener.onComConnectionConnected(ComClient.this);
                        }

                        @Override
                        public void onError(ComError error) {
                            Log.e(TAG, "claimed the session " + ComClient.this.sessionId + " failed: " + error.getMessage());
                            Log.d(TAG, "create new session");
                            ComClient.this.sessionId = null;
                            ComClient.this.userId = null;
                            ComClient.this.connect(ComClient.this.accessToken);
                        }
                    });
                }
            } catch (JSONException e) {
                Log.e(TAG, "onConnect error: " + e.getMessage());
            }
        });

    }

    @Override
    public void onDisconnect(boolean reconnecting) {
        this.connected = false;
        this.connectionListener.onComConnectionDisconnected(this, reconnecting);
    }

    @Override
    public void onError(ComError error) {

    }

    @Override
    public void onMessage(JSONObject packet) {
        Log.i(TAG, "onMessage: " + packet.toString());
        try {
            if (packet.getString("name").equalsIgnoreCase("event")) {
                JSONObject event = packet.getJSONObject("event");
                String eventName = event.getString("name");
                JSONObject data = event.getJSONObject("data");
                switch (eventName) {
                    case "custom_message": {
                        connectionListener.onCustomMessage(data.getString("from"), new JSONObject(data.getString("message")));
                        break;
                    }
                    case "call_start": {
                        ComCall newCall = new ComCall(this, data.getString("caller_id"), data.getString("callee_id"));
                        newCall.setCallID(data.getString("call_id"));
                        newCall.setIncomingCall(true);
                        newCall.setVideoCall(data.getBoolean("video_call"));
                        newCall.setCustomDataFromServer(data.getString("server_customdata"));
                        connectionListener.onComIncommingCall(newCall);
                        newCall.ringing();
                        break;
                    }
                    case "call_sdp":
                    case "call_state":
                    case "call_message":
                    case "call_other_device":
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
                        callback.onError(new ComError(error.getInt("err_code"), error.getString("message")));
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void connect(String accessToken) {
        if (!isConnected()) {
            this.accessToken = accessToken;
            executor.execute(() -> {
                socketConnection.connect();
            });
        }
    }
    public void disconnect() {
        this.socketConnection.disconnect();
    }
    public void sendMessage(JSONObject packet, RequestCallback callback) {
        int request_id = ++currentRequestId;
        requestCallbacks.put(request_id, callback);
        try {
            packet.put("request_id", request_id);
            socketConnection.send(packet);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
    }
    public void sendCustomMessage(String to, JSONObject msg, ComCallback callback){
        JSONObject packet = new JSONObject();
        try {
            packet.put("event", "custom_message");
            JSONObject body = new JSONObject();
            body.put("to", to);
            body.put("message", msg.toString());;
            packet.put("body", body);
            this.sendMessage(packet, new RequestCallback() {
                @Override
                public void onSuccess(JSONObject data) {
                    Log.i(TAG, "sendCustomMessage success" + data.toString());
                    callback.onSuccess();
                }

                @Override
                public void onError(ComError error) {
                    Log.e(TAG, "sendCustomMessage error" + error.toString());
                    callback.onError(new ComError(error.getCode(), error.getMessage()));
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
    }
    public void registerPushToken(String token, ComCallback callback){
        JSONObject packet = new JSONObject();
        try {
            packet.put("event", "register_push_token");
            JSONObject body = new JSONObject();
            body.put("token", token);;
            packet.put("body", body);
            this.sendMessage(packet, new RequestCallback() {
                @Override
                public void onSuccess(JSONObject data) {
                    Log.i(TAG, "registerPushToken success" + data.toString());
                    callback.onSuccess();
                }

                @Override
                public void onError(ComError error) {
                    Log.e(TAG, "Make call error" + error.toString());
                    callback.onError(new ComError(error.getCode(), error.getMessage()));
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
    }
    public void unregisterPushToken(String token, ComCallback callback){
        JSONObject packet = new JSONObject();
        try {
            packet.put("event", "unregister_push_token");
            JSONObject body = new JSONObject();
            body.put("token", token);;
            packet.put("body", body);
            this.sendMessage(packet, new RequestCallback() {
                @Override
                public void onSuccess(JSONObject data) {
                    Log.i(TAG, "unregisterPushToken success" + data.toString());
                    callback.onSuccess();
                }

                @Override
                public void onError(ComError error) {
                    Log.e(TAG, "unregisterPushToken error" + error.toString());
                    callback.onError(new ComError(error.getCode(), error.getMessage()));
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
    }
    public void addNewCall(ComCall call) {
        this.comCalls.add(call);
    }
    public void removeCall(ComCall call){
        this.comCalls.remove(call);
    }

    // getters
    public String getUserId() {
        return userId;
    }
    public String getSessionId() {
        return sessionId;
    }
    public boolean isConnected() {
        return connected;
    }
    public ComCall getCall(String callId) {
        for (ComCall comCall : comCalls) {
            if (comCall.getCallId().equalsIgnoreCase(callId))
                return comCall;
        }
        return null;
    }

    // setters
    public void setConnectionListener(ComConnectionListener listener) {
        connectionListener = listener;
    }


    public interface ComConnectionListener {
        void onComConnectionConnected(final ComClient client);

        void onComConnectionDisconnected(final ComClient client, boolean reconnect);

        void onComIncommingCall(ComCall call);

        void onComConnectionError(final ComClient client, ComError error);

        void onCustomMessage(String from, JSONObject msg);
    }

}
