package com.bangtran.comclient;

import android.content.Context;

import org.json.JSONObject;
import org.webrtc.EglBase;
import org.webrtc.PeerConnection;

import java.util.List;

public interface HandleWebRTCCallback {
    ComMediaConstraint getMediaConstraint();
    List<PeerConnection.IceServer> getIceServers();
    Context getAppContext();
    JSONObject getRemoteSDP();
    void onSuccess(JSONObject sdp);
    void onError(JSONObject error);
}
