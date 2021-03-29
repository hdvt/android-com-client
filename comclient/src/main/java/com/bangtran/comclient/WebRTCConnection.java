package com.bangtran.comclient;

import android.graphics.Camera;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.CameraVideoCapturer;
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
    private static PeerConnectionFactory pcFactory;
    private static EglBase.Context eglContext;

    private final PeerConnectionObserver pcObserver = new PeerConnectionObserver();
    private WebRTCListener webRTCListener;
    private PeerConnection pc;
    private VideoCapturer videoCapturer;

    private AudioSource audioSource;
    private VideoSource videoSource;
    private MediaStream localStream;
    private MediaStream remoteStream;
    private AudioTrack audioTrack = null;
    private VideoTrack videoTrack = null;

    private SessionDescription localSdp;

    private ComMediaConstraint comMediaConstraint;

    public WebRTCConnection(WebRTCListener listener) {
        webRTCListener = listener;
    }

    public void initConnection(HandleWebRTCCallback callback) {
        ComClient.executor.execute(() -> {
            if (eglContext == null) {
                eglContext = EglBase.create().getEglBaseContext();
            }
            if (pcFactory == null) {
                PeerConnectionFactory.initialize(
                        PeerConnectionFactory.InitializationOptions.builder(callback.getAppContext())
                                .setFieldTrials(ComMediaConstraint.VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL)
                                .setEnableInternalTracer(true)
                                .createInitializationOptions());
                final VideoEncoderFactory encoderFactory;
                final VideoDecoderFactory decoderFactory;
                encoderFactory = new DefaultVideoEncoderFactory(eglContext, true /* enableIntelVp8Encoder */, false);
                decoderFactory = new DefaultVideoDecoderFactory(eglContext);

                pcFactory = PeerConnectionFactory.builder()
                        .setOptions(new PeerConnectionFactory.Options())
                        .setVideoEncoderFactory(encoderFactory)
                        .setVideoDecoderFactory(decoderFactory)
                        .createPeerConnectionFactory();
            }
            // set constraint
            comMediaConstraint = callback.getMediaConstraint();

            audioSource = pcFactory.createAudioSource(new MediaConstraints());
            audioTrack = pcFactory.createAudioTrack(ComMediaConstraint.AUDIO_TRACK_ID, audioSource);
            audioTrack.setEnabled(true);

            if (comMediaConstraint.isVideoEnabled()) {
                Camera1Enumerator enumerator = new Camera1Enumerator(false);
                final String[] deviceNames = enumerator.getDeviceNames();

                for (String deviceName : deviceNames) {
                    if (enumerator.isFrontFacing(deviceName)) {
                        videoCapturer = enumerator.createCapturer(deviceName, null);
                    }
                }
                videoSource = pcFactory.createVideoSource(videoCapturer.isScreencast());
                SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglContext);
                videoCapturer.initialize(surfaceTextureHelper, callback.getAppContext(), videoSource.getCapturerObserver());
                videoCapturer.startCapture(comMediaConstraint.getVideoWidth(), comMediaConstraint.getVideoHeigh(), comMediaConstraint.getVideoFps());
                videoTrack = pcFactory.createVideoTrack(ComMediaConstraint.VIDEO_TRACK_ID, videoSource);
                videoTrack.setEnabled(true);
            }

            if (audioTrack != null || videoTrack != null) {
                localStream = pcFactory.createLocalMediaStream(ComMediaConstraint.LOCAL_MEDIA_ID);
                if (audioTrack != null) {
                    localStream.addTrack(audioTrack);
                }
                if (videoTrack != null)
                    localStream.addTrack(videoTrack);
            }
            PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(callback.getIceServers());
            pc = pcFactory.createPeerConnection(rtcConfig, pcObserver);
            if (localStream != null) {
                Log.d("WebRTCConnection", "Has a stream...");
                webRTCListener.onLocalStream(localStream);
                pc.addStream(localStream);
            }

            Log.d("WebRTCConnection", "Create offer..");
            MediaConstraints sdpMediaConstraints = new MediaConstraints();
            sdpMediaConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
            if (comMediaConstraint.isAudioEnabled())
                sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
            if (comMediaConstraint.isVideoEnabled())
                sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
            pc.createOffer(new SDPObserver(callback), sdpMediaConstraints);
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

    public void switchCamera() {
        if (videoCapturer instanceof CameraVideoCapturer) {
            if (!comMediaConstraint.isVideoEnabled()) {
                Log.e("WebRTCConnection", "Failed to switch camera.");
                return;
            }
            Log.d("WebRTCConnection", "Switch camera");
            CameraVideoCapturer cameraVideoCapturer = (CameraVideoCapturer) videoCapturer;
            cameraVideoCapturer.switchCamera(null);
        } else {
            Log.d("WebRTCConnection", "Will not switch camera, video caputurer is not a camera");
        }
    }

    public void setVideoEnabled(boolean enabled){
        if (videoTrack != null)
            videoTrack.setEnabled(enabled);
    }

    public void close() {
        if (remoteStream != null) {
            remoteStream.dispose();
            remoteStream = null;
        }
        if (localStream != null) {
            localStream.dispose();
            localStream = null;
        }
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
                videoCapturer = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (pc != null && pc.signalingState() != PeerConnection.SignalingState.CLOSED)
            pc.close();
        pc = null;
        localSdp = null;
    }

    public EglBase.Context getEglContext() {
        return eglContext;
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
                    webRTCListener.onMediaState(ComCall.MediaState.CONNECTED);
                } else if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
                    webRTCListener.onMediaState(ComCall.MediaState.DISCONNECTED);
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
                    if (localSdp == null) {
                        localSdp = sdp;
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
                        jsep.put("sdp", localSdp.description);
                        jsep.put("type", localSdp.type.canonicalForm());
                        callback.onSuccess(jsep);
                    } catch (JSONException e) {
                        Log.e("SDPObserver", "onSetSuccess: " + e.getMessage());
                    }
                } else {
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
