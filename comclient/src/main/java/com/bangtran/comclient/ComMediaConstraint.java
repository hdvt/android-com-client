package com.bangtran.comclient;

public class ComMediaConstraint {
    public static final String VIDEO_TRACK_ID = "ARDAMSv0";
    public static final String AUDIO_TRACK_ID = "ARDAMSa0";
    public static final String LOCAL_MEDIA_ID = "ARDAMS";
    public static final String VIDEO_TRACK_TYPE = "video";
    public static final String TAG = "PCRTCClient";
    public static final String VIDEO_CODEC_VP8 = "VP8";
    public static final String VIDEO_CODEC_VP9 = "VP9";
    public static final String VIDEO_CODEC_H264 = "H264";
    public static final String VIDEO_CODEC_H264_BASELINE = "H264 Baseline";
    public static final String VIDEO_CODEC_H264_HIGH = "H264 High";
    public static final String AUDIO_CODEC_OPUS = "opus";
    public static final String AUDIO_CODEC_ISAC = "ISAC";
    public static final String VIDEO_CODEC_PARAM_START_BITRATE = "x-google-start-bitrate";
    public static final String VIDEO_FLEXFEC_FIELDTRIAL =
            "WebRTC-FlexFEC-03-Advertised/Enabled/WebRTC-FlexFEC-03/Enabled/";
    public static final String VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL = "WebRTC-IntelVP8/Enabled/";
    public static final String DISABLE_WEBRTC_AGC_FIELDTRIAL =
            "WebRTC-Audio-MinimizeResamplingOnMobile/Enabled/";
    public static final String AUDIO_CODEC_PARAM_BITRATE = "maxaveragebitrate";
    public static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation";
    public static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl";
    public static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter";
    public static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression";
    public static final String DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT = "DtlsSrtpKeyAgreement";
    public static final int HD_VIDEO_WIDTH = 1280;
    public static final int HD_VIDEO_HEIGHT = 720;
    public static final int BPS_IN_KBPS = 1000;


    private boolean videoEnabled;
    private boolean audioEnabled;
    private int videoWidth;
    private int videoHeigh;
    private int videoFps;

    public ComMediaConstraint(boolean videoEnabled, boolean audioEnabled, int videoWidth, int videoHeigh, int videoFps) {
        this.videoEnabled = videoEnabled;
        this.audioEnabled = audioEnabled;
        this.videoWidth = videoWidth != 0 ? videoWidth : HD_VIDEO_WIDTH;
        this.videoHeigh = videoHeigh != 0 ? videoHeigh : HD_VIDEO_HEIGHT;
        this.videoFps = videoFps != 0 ? videoFps : 30;
    }

    public boolean isVideoEnabled() {
        return videoEnabled;
    }

    public void setVideoEnabled(boolean videoEnabled) {
        this.videoEnabled = videoEnabled;
    }

    public boolean isAudioEnabled() {
        return audioEnabled;
    }

    public void setAudioEnabled(boolean audioEnabled) {
        this.audioEnabled = audioEnabled;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public void setVideoWidth(int videoWidth) {
        this.videoWidth = videoWidth;
    }

    public int getVideoHeigh() {
        return videoHeigh;
    }

    public void setVideoHeigh(int videoHeigh) {
        this.videoHeigh = videoHeigh;
    }

    public int getVideoFps() {
        return videoFps;
    }

    public void setVideoFps(int videoFps) {
        this.videoFps = videoFps;
    }
}
