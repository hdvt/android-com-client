package com.bangtran.comclient;

import org.json.JSONObject;

public interface SocketConnectionListener {
    void onConnect();
    void onDisconnect();
    void onError(JSONObject error);
    void onMessage(JSONObject packet);
}
