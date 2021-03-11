package com.bangtran.comclient;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

public class WebRTCConnection {
    private final String VIDEO_TRACK_ID = "1929283";
    private final String AUDIO_TRACK_ID = "1928882";
    private final String LOCAL_MEDIA_ID = "1198181";
    private final PeerConnectionObserver pcObserver = new PeerConnectionObserver();
    private PeerConnection pc;
    private PeerConnectionFactory pcFactory;
    private SessionDescription mySdp;
    private WebRTCListener webRTCListener;
    private MediaStream localStream;
    private MediaStream remoteStream;
    private EglBase.Context eglContext;
    private AudioTrack audioTrack = null;
    private VideoTrack videoTrack = null;

    public WebRTCConnection(WebRTCListener listener) {
        webRTCListener = listener;
        eglContext = EglBase.create().getEglBaseContext();

        final VideoEncoderFactory encoderFactory;
        final VideoDecoderFactory decoderFactory;

        encoderFactory = new DefaultVideoEncoderFactory(
                eglContext, true /* enableIntelVp8Encoder */, false);
        decoderFactory = new DefaultVideoDecoderFactory(eglContext);
        pcFactory = PeerConnectionFactory.builder()
                .setOptions(new PeerConnectionFactory.Options())
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();
    }

    public EglBase.Context getEglContext() {
        return eglContext;
    }

    public void initConnection(HandleWebRTCCallback callback) {
        ComClient.executor.execute(() -> {
            MediaStream stream = null;
            AudioSource source = pcFactory.createAudioSource(new MediaConstraints());
            audioTrack = pcFactory.createAudioTrack(AUDIO_TRACK_ID, source);
            VideoCapturer videoCapturer = null;
            Camera1Enumerator enumerator = new Camera1Enumerator(false);
            final String[] deviceNames = enumerator.getDeviceNames();

            // First, try to find front facing camera
            for (String deviceName : deviceNames) {
                if (enumerator.isFrontFacing(deviceName)) {
                    videoCapturer = enumerator.createCapturer(deviceName, null);
                }
            }
            MediaConstraints constraints = new MediaConstraints();
            VideoSource vsource = pcFactory.createVideoSource(videoCapturer.isScreencast());
            SurfaceTextureHelper surfaceTextureHelper =
                    SurfaceTextureHelper.create("CaptureThread", eglContext);
            videoCapturer.initialize(surfaceTextureHelper, callback.getAppContext(), vsource.getCapturerObserver());
            videoCapturer.startCapture(1280, 720, 30);
            videoTrack = pcFactory.createVideoTrack(VIDEO_TRACK_ID, vsource);

            if (audioTrack != null || videoTrack != null) {
                stream = pcFactory.createLocalMediaStream(LOCAL_MEDIA_ID);
                if (audioTrack != null)
                    stream.addTrack(audioTrack);
                if (videoTrack != null)
                    stream.addTrack(videoTrack);
            }
            localStream = stream;
            if (localStream != null) {
                Log.d("WebRTCConnection", "Has a stream...");
                webRTCListener.onLocalStream(stream);
            }

            MediaConstraints pc_cons = new MediaConstraints();
            pc_cons.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
            if (true)
                pc_cons.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
            if (true)
                pc_cons.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

            PeerConnection.RTCConfiguration rtcConfig =
                    new PeerConnection.RTCConfiguration(callback.getIceServers());
            pc = pcFactory.createPeerConnection(rtcConfig, pcObserver);
            if (localStream != null)
                pc.addStream(localStream);

            Log.d("WebRTCConnection", "Create offer..");
            pc_cons.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
            pc_cons.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
            pc_cons.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
            pc.createOffer(new SDPObserver(callback), pc_cons);
        });
    }

    public void handleRemoteSDP(HandleWebRTCCallback callback) {
        ComClient.executor.execute(() -> {
            JSONObject jsep = callback.getRemoteSDP();
            JSONObject error = new JSONObject();
            if (jsep != null) {
                if (pc == null) {
                    Log.d("JANUSCLIENT", "could not set remote offer");

                    try {
                        error.put("message", "No peerconnection created, if this is an answer please use createAnswer)");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    callback.onError(error);
                    return;
                }
                try {
                    String sdpString = jsep.getString("sdp");
                    Log.d("WebRTCConnection", "setRemoteDescription");
                    Log.d("WebRTCConnection", sdpString);
                    SessionDescription.Type type = SessionDescription.Type.fromCanonicalForm(jsep.getString("type"));
                    SessionDescription sdp = new SessionDescription(type, sdpString);
                    pc.setRemoteDescription(new SDPObserver(callback), sdp);
                } catch (JSONException ex) {
                    try {
                        error.put("message", ex.getMessage());
                        callback.onError(error);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }


    private class PeerConnectionObserver implements PeerConnection.Observer {
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {

        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.d("PeerConnectionObserver", "IceConnectionState: " + iceConnectionState);
            ComClient.executor.execute(() -> {
                if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
                    webRTCListener.onMediaState(MediaState.CONNECTED);
                } else if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
                    webRTCListener.onMediaState(MediaState.DISCONNECTED);
                }
            });
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            Log.d("PeerConnectionObserver", "onIceCandidate");
            ComClient.executor.execute(() -> {
                webRTCListener.onIceCandidate(iceCandidate);
            });
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
            Log.d("PeerConnectionObserver", "onIceCandidatesRemoved");
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.d("PeerConnectionObserver", "onAddStream");
            ComClient.executor.execute(() -> {
                webRTCListener.onRemoteStream(mediaStream);
            });
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.d("PeerConnectionObserver", "onRemoveStream");

        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {
            Log.d("PeerConnectionObserver", "onDataChannel");

        }

        @Override
        public void onRenegotiationNeeded() {

        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {

        }
    }

    private class SDPObserver implements SdpObserver {
        private HandleWebRTCCallback callback;

        public SDPObserver(HandleWebRTCCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onCreateSuccess(final SessionDescription sdp) {
            Log.d("SDPObserver", "onCreateSuccess");
            ComClient.executor.execute(() -> {
                if (pc != null) {
                    if (mySdp == null) {
                        mySdp = sdp;
                        pc.setLocalDescription(new SDPObserver(callback), sdp);
                    }
                }
            });
        }

        @Override
        public void onSetSuccess() {
            Log.d("SDPObserver", "onSetSuccess");
            ComClient.executor.execute(() -> {
                if (pc.getRemoteDescription() == null) {
                    JSONObject jsep = new JSONObject();
                    try {
                        jsep.put("sdp", mySdp.description);
                        jsep.put("type", mySdp.type.canonicalForm());
                        callback.onSuccess(jsep);
                    } catch (JSONException e) {
                        Log.e("SDPObserver", "onSetSuccess: " + e.getMessage());
                    }
                }
                else {
                    callback.onSuccess(null);
                }
            });
        }

        @Override
        public void onCreateFailure(final String error) {
        }

        @Override
        public void onSetFailure(final String error) {
        }
    }
}
