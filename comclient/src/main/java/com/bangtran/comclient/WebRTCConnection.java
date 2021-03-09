package com.bangtran.comclient;

import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;

public class WebRTCConnection {
    private PeerConnection pc;
    private PeerConnectionFactory pcFactory;
    private SessionDescription mySDP;
    private WebRTCListener webRTCListener;
    private MediaStream localStream;
    private MediaStream remoteStream;
    private EglBase.Context eglContext;

    public WebRTCConnection() {
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

    public void initConnection(HandleWebRTCCallback callback){

    }

    public void handleRemoteSDP(HandleWebRTCCallback callback){

    }
}
