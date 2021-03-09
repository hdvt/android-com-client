package com.bangtran.comclient;

import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;

public interface WebRTCListener {
    void onLocalStream(MediaStream stream);
    void onRemoteStream(MediaStream stream);
    void onIceCandidate(IceCandidate candidates);
    void onMediaState(MediaState state);
    void onLocalSDP(JSONObject sdp);
}
