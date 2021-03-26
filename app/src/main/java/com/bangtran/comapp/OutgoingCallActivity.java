package com.bangtran.comapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bangtran.comclient.ComCall;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OutgoingCallActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int REQUEST_PERMISSION_CALL = 1;
    public static final int REQUEST_PERMISSION_CAMERA = 2;
    public static final int REQUEST_PERMISSION_CAMERA_WHEN_ANSWER = 3;
    private FrameLayout mLocalViewContainer;
    private FrameLayout mRemoteViewContainer;
    private TextView tvTo;
    private TextView tvState;
    private ImageButton btnMute;
    private ImageButton btnSpeaker;
    private ImageButton btnVideo;
    private ImageButton btnSwitch;
    private ComCall mComCall;
    private String from;
    private String to;
    private boolean isVideoCall;
    private boolean isMute = false;
    private boolean isSpeaker = false;
    private boolean isVideo = false;
    private ComCall.MediaState mMediaState;
    private ComCall.SignalingState mSignalingState;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outgoing_call);

        from = getIntent().getStringExtra("from");
        to = getIntent().getStringExtra("to");
        isVideoCall = getIntent().getBooleanExtra("is_video_call", false);


        mLocalViewContainer = (FrameLayout) findViewById(R.id.v_local);
        mRemoteViewContainer = (FrameLayout) findViewById(R.id.v_remote);

        tvTo = (TextView) findViewById(R.id.tv_to);
        tvTo.setText(to);

        tvState = (TextView) findViewById(R.id.tv_state);

        btnMute = (ImageButton) findViewById(R.id.btn_mute);
        btnMute.setOnClickListener(this);
        btnSpeaker = (ImageButton) findViewById(R.id.btn_speaker);
        btnSpeaker.setOnClickListener(this);
        btnVideo = (ImageButton) findViewById(R.id.btn_video);
        btnVideo.setOnClickListener(this);
        btnSwitch = (ImageButton) findViewById(R.id.btn_switch);
        btnSwitch.setOnClickListener(this);

        isSpeaker = isVideoCall;
        if (isSpeaker) {
            btnSpeaker.setImageResource(R.drawable.ic_speaker_on);
        } else {
            btnSpeaker.setImageResource(R.drawable.ic_speaker_off);
        }

        isVideo = isVideoCall;
        if (isVideo) {
            btnVideo.setImageResource(R.drawable.ic_video);
        } else {
            btnVideo.setImageResource(R.drawable.ic_video_off);
        }

        ImageButton btnEnd = (ImageButton) findViewById(R.id.btn_end);
        btnEnd.setOnClickListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> lstPermissions = new ArrayList<>();

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                lstPermissions.add(Manifest.permission.RECORD_AUDIO);
            }

            if (isVideoCall) {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    lstPermissions.add(Manifest.permission.CAMERA);
                }
            }

            if (lstPermissions.size() > 0) {
                String[] permissions = new String[lstPermissions.size()];
                for (int i = 0; i < lstPermissions.size(); i++) {
                    permissions[i] = lstPermissions.get(i);
                }
                ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_CALL);
                return;
            }
        }

        makeCall();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        boolean isGranted = false;
        if (grantResults.length > 0) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    isGranted = false;
                    break;
                } else {
                    isGranted = true;
                }
            }
        }
        switch (requestCode) {
            case REQUEST_PERMISSION_CALL:
                if (!isGranted) {
                    finish();
                } else {
                    makeCall();
                }
                break;
            case REQUEST_PERMISSION_CAMERA:
                if (isGranted) {
                    enableOrDisableVideo();
                }
                break;
            case REQUEST_PERMISSION_CAMERA_WHEN_ANSWER:
                if (isGranted) {
                    acceptCameraRequest();
                }
                break;
        }
    }

    private void makeCall() {
        mComCall = new ComCall(this, MainActivity.client, from, to);
        mComCall.setCustomData("{\"call_service_id\":1,\"call_type\":\"AUDIO\"}");
        mComCall.setVideoCall(isVideoCall);

        mComCall.setCallListener(new ComCall.ComCallListener() {
            @Override
            public void onSignalingStateChange(ComCall call, ComCall.SignalingState signalingState) {
//                Log.e("Stringee", "======== Custom data: " + stringeeCall.getCustomDataFromYourServer());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSignalingState = signalingState;
                        if (signalingState == ComCall.SignalingState.CALLING) {
                            tvState.setText("Outgoing call");
                        } else if (signalingState == ComCall.SignalingState.RINGING) {
                            tvState.setText("Ringing");
                        } else if (signalingState == ComCall.SignalingState.ANSWERED) {
                            tvState.setText("Starting");
                            if (mMediaState == ComCall.MediaState.CONNECTED) {
                                tvState.setText("Started");
                            }
                        } else if (signalingState == ComCall.SignalingState.BUSY) {
                            tvState.setText("Busy");
                            if (mComCall != null)
                                mComCall.hangup();
                            finish();
                        } else if (signalingState == ComCall.SignalingState.ENDED) {
                            tvState.setText("Ended");
                            if (mComCall != null)
                                mComCall.hangup();
                            finish();
                        }
                    }
                });
            }

            @Override
            public void onMediaStateChange(ComCall call, ComCall.MediaState state) {

            }

            @Override
            public void onLocalStream(ComCall call) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mComCall.isVideoCall()) {
                            Log.d("OutgoingCallActivity", "onLocalStream");
                            mLocalViewContainer.addView(mComCall.getLocalView());
                            mComCall.renderLocalView(true);
                        }
                    }
                });
            }

            @Override
            public void onRemoteStream(ComCall call) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (call.isVideoCall()) {
                            Log.d("OutgoingCallActivity", "onRemoteStream");
                            mRemoteViewContainer.addView(call.getRemoteView());
                            call.renderRemoteView(false);
                        }
                    }
                });
            }

            @Override
            public void onError(ComCall call, int code, String description) {

            }
        });
        mComCall.makeCall();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_mute:
                isMute = !isMute;
                if (isMute) {
                    btnMute.setImageResource(R.drawable.ic_mute);
                } else {
                    btnMute.setImageResource(R.drawable.ic_mic);
                }
                if (mComCall != null) {
//                    mComCall.mute(isMute);
                }
                break;
            case R.id.btn_speaker:
                isSpeaker = !isSpeaker;
                if (isSpeaker) {
                    btnSpeaker.setImageResource(R.drawable.ic_speaker_on);
                } else {
                    btnSpeaker.setImageResource(R.drawable.ic_speaker_off);
                }
                if (mComCall != null) {
//                    mComCall.setSpeakerphoneOn(isSpeaker);
                }
                break;
            case R.id.btn_end:
                if (mComCall != null)
                    mComCall.hangup();
                finish();
                break;
            case R.id.btn_video:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {
                        String[] permissions = {Manifest.permission.CAMERA};
                        ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_CAMERA);
                        return;
                    }
                }
                enableOrDisableVideo();
                break;
            case R.id.btn_switch:
                if (mComCall != null) {
//                    mComCall.switchCamera(null);
                }
                break;
        }
    }

    private void enableOrDisableVideo() {
        isVideo = !isVideo;
        if (isVideo) {
            btnVideo.setImageResource(R.drawable.ic_video);
        } else {
            btnVideo.setImageResource(R.drawable.ic_video_off);
        }
        if (mComCall != null) {
            if (!mComCall.isVideoCall()) { // Send camera request
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("type", "cameraRequest");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
//                mComCall.sendCallInfo(jsonObject);
            }
//            mComCall.enableVideo(isVideo);
        }
    }

    private void acceptCameraRequest() {
        isVideo = true;
        btnVideo.setImageResource(R.drawable.ic_video);
//        mComCall.enableVideo(true);
    }
}
