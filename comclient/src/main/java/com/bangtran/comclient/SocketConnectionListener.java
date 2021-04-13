package com.bangtran.comclient;

import org.json.JSONObject;

public interface SocketConnectionListener {
    void onConnect();
    void onDisconnect(boolean reconnecting);
    void onError(ComError error);
    void onMessage(JSONObject packet);
}
