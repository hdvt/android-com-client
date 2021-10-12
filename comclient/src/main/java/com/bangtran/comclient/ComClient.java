package com.bangtran.comclient;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.bangtran.comclient.call.ComCall;
import com.bangtran.comclient.chat.ComChatEvent;
import com.bangtran.comclient.chat.Conversation;
import com.bangtran.comclient.chat.ConversationOption;
import com.bangtran.comclient.chat.Message;
import com.bangtran.comclient.chat.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.List;
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

    private ConcurrentHashMap<Integer, ComCallback<JSONObject>> requestCallbacks;
    private Vector<ComCall> comCalls;

//    private final String endpointUrl = "ws://203.113.138.21:4417";
    private final String endpointUrl = "http://192.168.31.113";

    public ComClient(Context appContext) {
        this.appContext = appContext;
        socketConnection = new SocketConnection(endpointUrl, this);
        connected = false;
        sessionId = null;
        userId = null;
        comCalls = new Vector<ComCall>();
        requestCallbacks = new ConcurrentHashMap<Integer, ComCallback<JSONObject>>();
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
                    this.sendMessage(packet, new ComCallback<JSONObject>() {
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
                    body.put("token", this.accessToken);
                    packet.put("body", body);
                    this.sendMessage(packet, new ComCallback<JSONObject>() {
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
        if (this.connected) {
            this.connected = false;
            this.connectionListener.onComConnectionDisconnected(this, reconnecting);
        }
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
                    case "chat_message": {
                        Message msg = Message.parseFrom(data.getJSONObject("message"));
                        ComChatEvent chatEvent = new ComChatEvent(ComChatEvent.EventType.values()[data.getInt("type")], msg);
                        if (chatEvent.getEventType() == ComChatEvent.EventType.INSERT){
                            msg.updateState(this, Message.State.DELIVERED, new ComCallback<JSONObject>() {
                                @Override
                                public void onSuccess(@Nullable @org.jetbrains.annotations.Nullable JSONObject data) {
                                    Log.d(TAG, "update message state success");
                                }

                                @Override
                                public void onError(ComError error) {
                                    Log.e(TAG, "update message state error " + error.getMessage());

                                }
                            });
                        }
                        this.connectionListener.onComChatEvent(this, chatEvent);
                        break;
                    }
                    case "chat_conversation": {
                        Conversation conversation = Conversation.parseFrom((data.getJSONObject("conversation")));
                        ComChatEvent chatEvent = new ComChatEvent(ComChatEvent.EventType.values()[data.getInt("type")], conversation);
                        this.connectionListener.onComChatEvent(this, chatEvent);
                        break;
                    }
                }
            } else if (!packet.isNull("request_id")) {
                int request_id = packet.getInt("request_id");
                if (requestCallbacks.containsKey(request_id)) {
                    ComCallback<JSONObject> callback = requestCallbacks.get(request_id);
                    requestCallbacks.remove(request_id);
                    if (packet.getString("name").equalsIgnoreCase("success")) {
                        callback.onSuccess(packet.getJSONObject("data"));
                    } else {
                        JSONObject error = packet.getJSONObject("error");
                        callback.onError(new ComError(error.getInt("err_code"), error.getString("message")));
                    }
                }
            }
        } catch (JSONException | ParseException e) {
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
    public void sendMessage(JSONObject packet, ComCallback<JSONObject> callback) {
        int request_id = ++currentRequestId;
        requestCallbacks.put(request_id, callback);
        try {
            packet.put("request_id", request_id);
            socketConnection.send(packet);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
    }
    public void sendCustomMessage(String to, JSONObject msg, ComCallback<JSONObject> callback){
        JSONObject packet = new JSONObject();
        try {
            packet.put("event", "custom_message");
            JSONObject body = new JSONObject();
            body.put("to", to);
            body.put("message", msg.toString());;
            packet.put("body", body);
            this.sendMessage(packet, new ComCallback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject data) {
                    Log.i(TAG, "sendCustomMessage success" + data.toString());
                    callback.onSuccess(data);
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
    public void registerPushToken(String token, ComCallback<JSONObject> callback){
        JSONObject packet = new JSONObject();
        try {
            packet.put("event", "register_push_token");
            JSONObject body = new JSONObject();
            body.put("token", token);;
            packet.put("body", body);
            this.sendMessage(packet, new ComCallback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject data) {
                    Log.i(TAG, "registerPushToken success" + data.toString());
                    callback.onSuccess(data);
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
    public void unregisterPushToken(String token, ComCallback<JSONObject> callback){
        JSONObject packet = new JSONObject();
        try {
            packet.put("event", "unregister_push_token");
            JSONObject body = new JSONObject();
            body.put("token", token);;
            packet.put("body", body);
            this.sendMessage(packet, new ComCallback<JSONObject>() {
                @Override
                public void onSuccess(JSONObject data) {
                    Log.i(TAG, "unregisterPushToken success" + data.toString());
                    callback.onSuccess(data);
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

    public ComConnectionListener getConnectionListener() {
        return connectionListener;
    }

    public void getLastConversations(int count, ComCallback<List<Conversation>> callback){
        loadConversations(-1, -1, count, callback);
    }

    public void getConversationsBefore(long date, int count, ComCallback<List<Conversation>> callback){
        loadConversations(-1, date, count, callback);
    }

    private void loadConversations(long greaterDate, long smallerDate, int limit, ComCallback<List<Conversation>> callback ){
        executor.execute(() -> {
            JSONObject packet = new JSONObject();
            try {
                packet.put("event", "chat_load_conversations");
                JSONObject body = new JSONObject();
                if (greaterDate > -1)
                    body.put("lastupdate_greater", greaterDate);
                if (smallerDate > -1)
                    body.put("lastupdate_smaller", smallerDate);
                body.put("limit", limit);
                packet.put("body", body);
                this.sendMessage(packet, new ComCallback<JSONObject>() {
                    @Override
                    public void onSuccess(JSONObject data) {
                        Log.d(TAG, "conversation " + data.toString());
                        try {
                            List<Conversation> conversations = new Vector<>();
                            JSONArray convJson = data.getJSONArray("conversations");
                            for(int i = 0; i < convJson.length(); i++){
                                conversations.add(Conversation.parseFrom(convJson.getJSONObject(i)));
                            }
                            for (int i = 0; i < conversations.size(); i++){
                                if (conversations.get(i).getLastMessage(null).getState() == Message.State.SENT){
                                    conversations.get(i).getLastMessage(null).updateState(ComClient.this, Message.State.DELIVERED, new ComCallback<JSONObject>() {
                                        @Override
                                        public void onSuccess(@Nullable @org.jetbrains.annotations.Nullable JSONObject data) {
                                            Log.d(TAG, "update message state success");
                                        }
                                        @Override
                                        public void onError(ComError error) {
                                            Log.e(TAG, "update message state error " + error.getMessage());

                                        }
                                    });
                                }
                            }
                            callback.onSuccess(conversations);
                        } catch (JSONException | ParseException e) {
                            Log.e(TAG, e.toString());
                        }

                    }
                    @Override
                    public void onError(ComError error) {
                        Log.e(TAG, "get conversations error " + error.toString());
                    }
                });


            } catch (JSONException e) {
                Log.e(TAG, "get conversations error: " + e.getMessage());
            }
        });
    }

    public void getConversationByUserId(String userId, ComCallback<Conversation> callback){
        executor.execute(() -> {
            JSONObject packet = new JSONObject();
            try {
                packet.put("event", "chat_get_conversation_info");
                JSONObject body = new JSONObject();
                JSONArray participants = new JSONArray();
                participants.put(userId);
                body.put("participants", participants);
                packet.put("body", body);
                this.sendMessage(packet, new ComCallback<JSONObject>() {
                    @Override
                    public void onSuccess(JSONObject data) {
                        Log.d(TAG, "getConversationByUserId " + data.toString());
                        try {
                            callback.onSuccess(Conversation.parseFrom(data.getJSONObject("conversation")));
                        } catch (JSONException | ParseException e) {
                            Log.e(TAG, e.toString());
                        }

                    }
                    @Override
                    public void onError(ComError error) {
                        Log.e(TAG, "getConversationByUserId error " + error.toString());
                        callback.onError(error);
                    }
                });


            } catch (JSONException e) {
                Log.e(TAG, "get conversations error: " + e.getMessage());
            }
        });
    }

    public void getTotalUnread(ComCallback<Integer> callback){
        //// TODO: 13/08/2021
        executor.execute(() -> {
            JSONObject packet = new JSONObject();
            try {
                packet.put("event", "chat_unread_conversation_count");
                JSONObject body = new JSONObject();
                packet.put("body", body);
                this.sendMessage(packet, new ComCallback<JSONObject>() {
                    @Override
                    public void onSuccess(JSONObject data) {
                        try {
                            callback.onSuccess(data.getInt("total_unread"));
                        } catch (JSONException e) {
                            Log.e(TAG, e.toString());
                        }

                    }
                    @Override
                    public void onError(ComError error) {
                        Log.e(TAG, "getTotalUnread error " + error.toString());
                    }
                });


            } catch (JSONException e) {
                Log.e(TAG, "getTotalUnread error: " + e.getMessage());
            }
        });
    }


    // setters
    public void setConnectionListener(ComConnectionListener listener) {
        connectionListener = listener;
    }

    public void createConversation(List<User> participants, ConversationOption options, ComCallback<Conversation> callback){
        executor.execute(() -> {
            JSONObject packet = new JSONObject();
            try {
                packet.put("event", "chat_create_conversation");
                JSONObject body = new JSONObject();
                body.put("name", options.getName());
                body.put("group", options.isGroup());
                JSONArray participantsJs = new JSONArray();
                for (int i = 0; i < participants.size(); i++){
                    JSONObject participant = new JSONObject();
                    participant.put("user_id", participants.get(i).getUserId());
                    participant.put("name", participants.get(i).getName().isEmpty() ? null : participants.get(i).getName());
                    participantsJs.put(participant);
                }
                body.put("participants", participantsJs);
                packet.put("body", body);
                this.sendMessage(packet, new ComCallback<JSONObject>() {
                    @Override
                    public void onSuccess(JSONObject data) {
                        Log.d(TAG, "createConversation " + data.toString());
                        try {
                            callback.onSuccess(Conversation.parseFrom(data.getJSONObject("conversation")));
                        } catch (JSONException | ParseException e) {
                            Log.e(TAG, e.toString());
                        }

                    }
                    @Override
                    public void onError(ComError error) {
                        Log.e(TAG, "createConversation error " + error.toString());
                    }
                });


            } catch (JSONException e) {
                Log.e(TAG, "createConversation error: " + e.getMessage());
            }
        });
    }

    public interface ComConnectionListener {
        void onComConnectionConnected(final ComClient client);

        void onComConnectionDisconnected(final ComClient client, boolean reconnect);

        void onComIncommingCall(ComCall call);

        void onComConnectionError(final ComClient client, ComError error);

        void onCustomMessage(String from, JSONObject msg);

        void onComChatEvent(final ComClient client, ComChatEvent event);
    }

}
