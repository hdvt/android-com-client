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
    String callID;
    String callerID;
    String calleeID;
    boolean incomingCall;
    boolean videoCall;
    ComCallListener callListener;
    WebRTCConnection webRTCConnection;
    SurfaceViewRenderer localView;
    SurfaceViewRenderer remoteView;
    MediaStream localStream;
    MediaStream remoteStream;
    private ComClient client;
    private String customData;
    private String customDataFromServer;
    private Context appContext;
    private Vector<IceCandidate> candidates;


    public ComCall(Context appContext, ComClient client, String from, String to) {
        this.callerID = from;
        this.calleeID = to;
        videoCall = true;
        this.customData = null;
        this.customDataFromServer = null;
        callListener = null;
        webRTCConnection = new WebRTCConnection(this);
        this.appContext = appContext;
        this.client = client;
        this.candidates = new Vector<IceCandidate>();
        this.client.addNewCall(this);
    }

    public ComCall(ComClient client, String from, String to) {
        this(null, client, from, to);
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
    public void onIceCandidate(IceCandidate candidate) {
        if (getCallId() == null){
            this.candidates.add(candidate);
        }
        else {
            this.sendIceCandidate(candidate);
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
//                callListener.onSignalingStateChange(ComCall.this, ComCall.SignalingState.ANSWERED);
            }

            @Override
            public void onError(JSONObject error) {

            }
        });
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

    public String getCallId() {
        return callID;
    }

    public String getCallerID() {
        return callerID;
    }

    public String getCalleeID() {
        return calleeID;
    }

    public boolean isIncomingCall() {
        return incomingCall;
    }

    public void setIncomingCall(boolean incomingCall) {
        this.incomingCall = incomingCall;
    }

    public String getCustomData() {
        return customData;
    }

    // setters

    public void setCustomData(String customData) {
        this.customData = customData;
    }

    public String getCustomDataFromServer() {
        return customDataFromServer;
    }

    public void setCustomDataFromServer(String customDataFromServer) {
        this.customDataFromServer = customDataFromServer;
    }

    public void setCallListener(ComCallListener listener) {
        this.callListener = listener;
    }

    public void setCallID(String callID) {
        this.callID = callID;
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
                    body.put("caller_id", getCallerID());
                    body.put("callee_id", getCalleeID());
                    body.put("video_call", isVideoCall());
                    body.put("offer_jsep", jsep);
                    body.put("custom_data", getCustomData());
                    packet.put("body", body);
                    client.sendMessage(packet, new RequestCallback() {
                        @Override
                        public void onSuccess(JSONObject data) {
                            Log.i("ComCall", "makeCall success" + data.toString());
                            try {
                                setCallID(data.getString("call_id"));
                                setCustomDataFromServer(data.getString("server_customdata"));
                                for (int i = 0; i < candidates.size(); i++){
                                    sendIceCandidate(candidates.elementAt(i));
                                }
                                candidates.removeAllElements();
                            } catch (JSONException e) {
                                Log.e("ComCall", e.getMessage());
                            }
                        }

                        @Override
                        public void onError(ComError error) {
                            Log.e("ComClient", "Make call error" + error.toString());
                            ComCall.this.callListener.onError(ComCall.this, error.getCode(), error.getMessage());
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
                    packet.put("event", "call_accept");
                    JSONObject body = new JSONObject();
                    body.put("call_id", getCallId());
                    body.put("offer_jsep", jsep);
                    packet.put("body", body);
                    client.sendMessage(packet, new RequestCallback() {
                        @Override
                        public void onSuccess(JSONObject data) {
                            Log.i("ComCall", "answerCall success" + data.toString());
//                            try {
//                                JSONObject jsep = data.getJSONObject("jsep");
//                                onRemoteSDP(jsep);
//                            } catch (JSONException e) {
//                                Log.e("ComCall", e.getMessage());
//                            }
                        }

                        @Override
                        public void onError(ComError error) {
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

    public void ringing() {
        JSONObject packet = new JSONObject();
        try {
            packet.put("event", "call_ringing");
            JSONObject body = new JSONObject();
            body.put("call_id", getCallId());
            packet.put("body", body);
            client.sendMessage(packet, new RequestCallback() {
                @Override
                public void onSuccess(JSONObject data) {
                    Log.i("ComCall", "ringing success" + data.toString());

                }

                @Override
                public void onError(ComError error) {
                    Log.e("ComClient", "ringing error" + error.toString());
                }
            });
        } catch (JSONException e) {
            Log.e("ComCall", e.getMessage());
        }
    }

    public void hangup() {
        JSONObject packet = new JSONObject();
        try {
            packet.put("event", "call_stop");
            JSONObject body = new JSONObject();
            body.put("call_id", getCallId());
            packet.put("body", body);
            client.sendMessage(packet, new RequestCallback() {
                @Override
                public void onSuccess(JSONObject data) {
                    Log.i("ComCall", "hangup success" + data.toString());
                    webRTCConnection.close();
                    client.removeCall(ComCall.this);
                }

                @Override
                public void onError(ComError error) {
                    Log.e("ComClient", "hangup error" + error.toString());
                }
            });
        } catch (JSONException e) {
            Log.e("ComCall", e.getMessage());
        }
    }

    public void reject() {
        JSONObject packet = new JSONObject();
        try {
            packet.put("event", "call_reject");
            JSONObject body = new JSONObject();
            body.put("call_id", getCallId());
            packet.put("body", body);
            client.sendMessage(packet, new RequestCallback() {
                @Override
                public void onSuccess(JSONObject data) {
                    Log.i("ComCall", "reject success" + data.toString());
                    webRTCConnection.close();
                    client.removeCall(ComCall.this);
                }

                @Override
                public void onError(ComError error) {
                    Log.e("ComClient", "reject error" + error.toString());
                }
            });
        } catch (JSONException e) {
            Log.e("ComCall", e.getMessage());
        }
    }

    public void handleEvent(String event, JSONObject data){
        Log.d("handleEvent", event + " - " + data.toString());
        switch (event){
            case "call_state": {
                try {
                    SignalingState state = SignalingState.values()[data.getInt("call_state")- 1];
                    Log.d("handleEvent", state.toString());
                    this.callListener.onSignalingStateChange(this, state);
                } catch (JSONException e) {
                    Log.e("ComCall", e.getMessage());
                }
                break;
            }
            case "call_sdp": {
                try {
                    this.onRemoteSDP(data.getJSONObject("jsep"));
                } catch (JSONException e) {
                    Log.e("ComCall", e.getMessage());
                }
                break;
            }
            case "call_stop": {
                this.webRTCConnection.close();
                this.client.removeCall(this);
            }
        }
    }

    // private functions
    private void sendIceCandidate(IceCandidate candidate){
//        JSONObject packet = new JSONObject();
//        try {
//            packet.put("event", "call_candidate");
//            JSONObject body = new JSONObject();
//            body.put("call_id", getCallId());
//            packet.put("body", body);
//            JSONObject cand = new JSONObject();
//            if (candidate == null) {
//                cand.put("completed", true);
//            } else {
//                cand.put("candidate", candidate.sdp);
//                cand.put("sdpMid", candidate.sdpMid);
//                cand.put("sdpMLineIndex", candidate.sdpMLineIndex);
//            }
//            body.put("candidate", cand);
//            Log.d("ComCall", "onIceCandidate" + packet.toString());
//            client.sendMessage(packet, new RequestCallback() {
//                @Override
//                public void onSuccess(JSONObject data) {
//                    Log.i("ComCall", "sendCandidate success" + data.toString());
//                }
//
//                @Override
//                public void onError(ComError error) {
//                    Log.e("ComCall", "sendCandidate error" + error.toString());
//                }
//            });
//
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
    }


    public enum SignalingState {
        CALLING,
        RINGING,
        BUSY,
        ANSWERED,
        ENDED,
    }

    public enum MediaState {
        CONNECTED,
        DISCONNECTED
    }

    public interface ComCallListener {
        void onSignalingStateChange(ComCall call, ComCall.SignalingState state);

        void onMediaStateChange(ComCall call, ComCall.MediaState state);

        void onLocalStream(ComCall call);

        void onRemoteStream(ComCall call);

        void onError(ComCall call, int code, String description);
    }

}
