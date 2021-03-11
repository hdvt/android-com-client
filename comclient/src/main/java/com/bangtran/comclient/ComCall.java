package com.bangtran.comclient;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SurfaceViewRenderer;

import java.util.List;
import java.util.Vector;

public class ComCall implements WebRTCListener {
    String from;
    String to;
    ComCallListener callListener;
    boolean videoCall;
    int callId;
    SurfaceViewRenderer localView;
    SurfaceViewRenderer remoteView;
    MediaStream localStream;
    MediaStream remoteStream;
    WebRTCConnection webRTCConnection;
    boolean incomingCall;
    private ComClient client;
    private Context appContext;

    public ComCall(Context appContext, ComClient client, String from, String to) {
        this.client = client;
        this.appContext = appContext;
        this.from = from;
        this.to = to;
        videoCall = true;
        webRTCConnection = new WebRTCConnection(this);
        client.addNewCall(this);
    }

    public ComCall(ComClient client, String from, String to) {
        this.client = client;
        this.appContext = appContext;
        this.from = from;
        this.to = to;
        videoCall = true;
        webRTCConnection = new WebRTCConnection(this);
        client.addNewCall(this);
    }

    @Override
    public void onLocalStream(MediaStream stream) {
        localStream = stream;
        callListener.onLocalStream(this);
    }

    @Override
    public void onRemoteStream(MediaStream stream) {
        remoteStream = stream;
        callListener.onRemoteStream(this);
    }

    @Override
    public void onIceCandidate(IceCandidate candidates) {
        JSONObject packet = new JSONObject();
        try {
            packet.put("event", "call_candidate");
            JSONObject body = new JSONObject();
            body.put("session_id", client.getSessionId());
            body.put("call_id", getCallId());
            packet.put("body", body);
            JSONObject cand = new JSONObject();
            if (candidates == null) {
                cand.put("completed", true);
            } else {
                cand.put("candidate", candidates.sdp);
                cand.put("sdpMid", candidates.sdpMid);
                cand.put("sdpMLineIndex", candidates.sdpMLineIndex);
            }
            body.put("candidate", cand);
            Log.d("ComCall", "onIceCandidate" + packet.toString());
            client.sendMessage(packet, new RequestCallback() {
                @Override
                public void onSuccess(JSONObject data) {
                    Log.i("ComCall", "sendCandidate success" + data.toString());
                }
                @Override
                public void onError(JSONObject error) {
                    Log.e("ComCall", "sendCandidate error" + error.toString());
                }
            });


        } catch (JSONException e) {
            e.printStackTrace();
        }



    }

    @Override
    public void onMediaState(MediaState state) {

    }

    @Override
    public void onLocalSDP(JSONObject sdp) {

    }

    @Override
    public void onRemoteSDP(JSONObject jsep) {
        webRTCConnection.handleRemoteSDP(new HandleWebRTCCallback() {
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
                return null;
            }

            @Override
            public JSONObject getRemoteSDP() {
                return jsep;
            }

            @Override
            public void onSuccess(JSONObject sdp) {
                callListener.onSignalingStateChange(ComCall.this, SignalingState.ANSWERED);
            }

            @Override
            public void onError(JSONObject error) {

            }
        });
    }

    public void setCallListener(ComCallListener listener) {
        this.callListener = listener;
    }

    public int getCallId() {
        return callId;
    }

    public void setCallId(int callId) {
        this.callId = callId;
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

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public boolean isIncomingCall() {
        return incomingCall;
    }

    public void setIncomingCall(boolean incomingCall) {
        this.incomingCall = incomingCall;
    }

    public Context getAppContext() {
        return appContext;
    }

    public boolean isVideoCall() {
        return videoCall;
    }

    public void setVideoCall(boolean video) {
        this.videoCall = video;
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
                return new Vector<>();
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
            public void onSuccess(JSONObject jsep) {
                Log.d("ComCall", "Got sdp success: " + jsep.toString());
                JSONObject packet = new JSONObject();
                try {
                    packet.put("event", "call_start");
                    JSONObject body = new JSONObject();
                    body.put("session_id", client.getSessionId());
                    body.put("callee_user_id", getTo());
                    body.put("jsep", jsep);
                    packet.put("body", body);
                    client.sendMessage(packet, new RequestCallback() {
                        @Override
                        public void onSuccess(JSONObject data) {
                            Log.i("ComCall", "makeCall success" + data.toString());
                            try {
                                callId = data.getInt("call_id");
                                callListener.onSignalingStateChange(ComCall.this, SignalingState.CALLING);
                            } catch (JSONException e) {
                                Log.e("ComCall", e.getMessage());
                            }
                        }

                        @Override
                        public void onError(JSONObject error) {
                            Log.e("ComClient", "Authen error" + error.toString());
                        }
                    });
                } catch (JSONException e) {
                    Log.e("ComClient", e.getMessage());
                }
            }

            @Override
            public void onError(JSONObject error) {

            }
        });
    }

    public void answer(Context context) {
        appContext = context;
        webRTCConnection.initConnection(new HandleWebRTCCallback() {
            @Override
            public ComMediaConstraint getMediaConstraint() {
                return null;
            }

            @Override
            public List<PeerConnection.IceServer> getIceServers() {
                return new Vector<>();
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
            public void onSuccess(JSONObject jsep) {
                Log.d("ComCall", "Got sdp success: " + jsep.toString());
                JSONObject packet = new JSONObject();
                try {
                    packet.put("event", "call_answer");
                    JSONObject body = new JSONObject();
                    body.put("session_id", client.getSessionId());
                    body.put("call_id", getCallId());
                    body.put("jsep", jsep);
                    packet.put("body", body);
                    client.sendMessage(packet, new RequestCallback() {
                        @Override
                        public void onSuccess(JSONObject data) {
                            Log.i("ComCall", "answerCall success" + data.toString());
                            try {
                                JSONObject jsep = data.getJSONObject("jsep");
                                onRemoteSDP(jsep);
                            } catch (JSONException e) {
                                Log.e("ComCall", e.getMessage());
                            }
                        }

                        @Override
                        public void onError(JSONObject error) {
                            Log.e("ComClient", "Authen error" + error.toString());
                        }
                    });
                } catch (JSONException e) {
                    Log.e("ComClient", e.getMessage());
                }
            }

            @Override
            public void onError(JSONObject error) {

            }
        });
    }


}
