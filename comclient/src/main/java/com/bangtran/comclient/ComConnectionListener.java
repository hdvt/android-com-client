package com.bangtran.comclient;

import org.json.JSONObject;

public interface ComConnectionListener {
    void onConnectionConnected(final ComClient client);
//    void onConnectionDisconnected(final ComClient client);
    void onIncommingCall(ComCall call);
//    void onConnectionError(final ComClient client, JSONObject error);
}
