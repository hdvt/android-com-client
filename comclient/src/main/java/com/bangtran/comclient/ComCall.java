package com.bangtran.comclient;

import android.content.Context;

import org.json.JSONObject;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SurfaceViewRenderer;

import java.util.List;

public class ComCall implements WebRTCListener {
    private ComClient client;
    private Context appContext;
    String callerId;
    String calleeId;
    ComCallListener callListener;
    boolean videoCall;
    int callId;
    SurfaceViewRenderer localView;
    SurfaceViewRenderer remoteView;
    MediaStream localStream;
    MediaStream remoteStream;
    WebRTCConnection webRTCConnection;

    public ComCall(ComClient client, Context appContext, String callerId, String calleeId) {
        this.client = client;
        this.appContext = appContext;
        this.callerId = callerId;
        this.calleeId = calleeId;
        videoCall = true;
        webRTCConnection = new WebRTCConnection();
    }

    @Override
    public void onLocalStream(MediaStream stream) {

    }

    @Override
    public void onRemoteStream(MediaStream stream) {

    }

    @Override
    public void onIceCandidate(IceCandidate candidates) {

    }

    @Override
    public void onMediaState(MediaState state) {

    }

    @Override
    public void onLocalSDP(JSONObject sdp) {

    }

    public void setCallListener(ComCallListener listener){
        this.callListener = listener;
    }

    public void setVideoCall(boolean video){
        this.videoCall = video;
    }

    public int getCallId() {
        return callId;
    }

    public SurfaceViewRenderer getLocalView() {
        if (this.localView == null)
            this.localView = new SurfaceViewRenderer(this.appContext);
        return this.localView;
    }

    public SurfaceViewRenderer getRemoteView() {
        if (this.remoteView == null)
            this.remoteView = new SurfaceViewRenderer(this.appContext);
        return this.remoteView;
    }

    public Context getAppContext() {
        return appContext;
    }

    public boolean isVideoCall(){
        return videoCall;
    }


    public void renderLocalView(boolean isOverlay) {
        if (this.localStream != null && this.localStream.videoTracks != null && this.localStream.videoTracks.size() > 0) {
            this.localView.init(this.webRTCConnection.getEglContext(), null);
            this.localView.setMirror(true);
            this.localView.setZOrderMediaOverlay(isOverlay);
            this.localStream.videoTracks.get(0).addSink(this.localView);
        }
    }

    public void renderRemoteView(boolean isOverlay) {
        if (this.remoteStream != null && this.remoteStream.videoTracks != null && this.remoteStream.videoTracks.size() > 0) {
            this.remoteView.init(this.webRTCConnection.getEglContext(), null);
            this.remoteView.setMirror(true);
            this.remoteView.setZOrderMediaOverlay(isOverlay);
            this.remoteStream.videoTracks.get(0).addSink(this.remoteView);
        }
    }

    public void makeCall() {
        webRTCConnection.initConnection(new HandleWebRTCCallback() {
            @Override
            public ComMediaConstraint getMediaConstraint() {
                return null;
            }

            @Override
            public List<PeerConnection.IceServer> getIceServers() {
                return null;
            }

            @Override
            public Context getAppContext() {
                return appContext;
            }

            @Override
            public JSONObject getRemoteSDP() {
                return null;
            }

            @Override
            public void onSuccess(JSONObject sdp) {

            }

            @Override
            public void onError(JSONObject error) {

            }
        });
    }





}
