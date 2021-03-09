package com.bangtran.comclient;

import org.json.JSONObject;

public interface ComCallListener {
    void onSignalingStateChange(ComCall call, SignalingState state);
    void onMediaStateChange(ComCall call, MediaState state);
    void onLocalStream(ComCall call);
    void onRemoteStream(ComCall call);
    void onError(ComCall call, JSONObject error);
}
