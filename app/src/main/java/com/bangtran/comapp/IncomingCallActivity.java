package com.bangtran.comapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bangtran.comclient.ComCall;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class IncomingCallActivity extends AppCompatActivity implements View.OnClickListener {

    private FrameLayout mLocalViewContainer;
    private FrameLayout mRemoteViewContainer;
    private TextView tvFrom;
    private TextView tvState;
    private ImageButton btnAnswer;
    private ImageButton btnEnd;
    private ImageButton btnMute;
    private ImageButton btnSpeaker;
    private ImageButton btnVideo;
    private ImageButton btnSwitch;
    private View vControl;

    private ComCall mCall;
    private boolean isMute = false;
    private boolean isSpeaker = false;
    private boolean isVideo = false;

    private ComCall.MediaState mMediaState;
    private ComCall.SignalingState mSignalingState;

    public static final int REQUEST_PERMISSION_CALL = 1;
    public static final int REQUEST_PERMISSION_CAMERA = 2;
    public static final int REQUEST_PERMISSION_CAMERA_WHEN_ANSWER = 3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);

        int callId = getIntent().getIntExtra("call_id", 0);
        mCall = MainActivity.callsMap.get(callId);

        mLocalViewContainer = (FrameLayout) findViewById(R.id.v_local);
        mRemoteViewContainer = (FrameLayout) findViewById(R.id.v_remote);

        tvFrom = (TextView) findViewById(R.id.tv_from);
        tvFrom.setText(mCall.getCalleeID());

        tvState = (TextView) findViewById(R.id.tv_state);

        btnAnswer = (ImageButton) findViewById(R.id.btn_answer);
        btnAnswer.setOnClickListener(this);

        btnEnd = (ImageButton) findViewById(R.id.btn_end);
        btnEnd.setOnClickListener(this);

        btnMute = (ImageButton) findViewById(R.id.btn_mute);
        btnMute.setOnClickListener(this);
        btnSpeaker = (ImageButton) findViewById(R.id.btn_speaker);
        btnSpeaker.setOnClickListener(this);
        btnVideo = (ImageButton) findViewById(R.id.btn_video);
        btnVideo.setOnClickListener(this);
        btnSwitch = (ImageButton) findViewById(R.id.btn_switch);
        btnSwitch.setOnClickListener(this);

        isSpeaker = mCall.isVideoCall();
        if (isSpeaker) {
            btnSpeaker.setImageResource(R.drawable.ic_speaker_on);
        } else {
            btnSpeaker.setImageResource(R.drawable.ic_speaker_off);
        }

        vControl = findViewById(R.id.v_control);
        isVideo = mCall.isVideoCall();
        if (isVideo) {
            btnVideo.setVisibility(View.VISIBLE);
            btnVideo.setImageResource(R.drawable.ic_video);
        } else {
            btnVideo.setVisibility(View.INVISIBLE);
            btnVideo.setImageResource(R.drawable.ic_video_off);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> lstPermissions = new ArrayList<>();

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                lstPermissions.add(Manifest.permission.RECORD_AUDIO);
            }

            if (mCall.isVideoCall()) {
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

        initAnswer();
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
                    initAnswer();
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

    private void initAnswer() {
        mCall.setCallListener(new ComCall.ComCallListener() {
            @Override
            public void onSignalingStateChange(ComCall comCall, final ComCall.SignalingState signalingState) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSignalingState = signalingState;
                        if (signalingState == ComCall.SignalingState.ANSWERED) {
                            tvState.setText("Starting");
                            if (mMediaState == ComCall.MediaState.CONNECTED) {
                                tvState.setText("Started");
                            }
                        } else if (signalingState == ComCall.SignalingState.ENDED) {
                            tvState.setText("Ended");
                            if (mCall != null) {
                                mCall.hangup();
                            }
                            finish();
                        }
                    }
                });
            }



           
            @Override
            public void onMediaStateChange(ComCall call, final ComCall.MediaState mediaState) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mMediaState = mediaState;
                        if (mediaState == ComCall.MediaState.CONNECTED) {
                            if (mSignalingState == ComCall.SignalingState.ANSWERED) {
                                tvState.setText("Started");
                            }
                        }
                    }
                });
            }

            @Override
            public void onLocalStream(final ComCall call) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (call.isVideoCall()) {
                            mLocalViewContainer.addView(call.getLocalView());
                            call.renderLocalView(true);
                        }
                    }
                });
            }

            @Override
            public void onRemoteStream(final ComCall call) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (call.isVideoCall()) {
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

//        mCall.answer(this);
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
                if (mCall != null) {
//                    mCall.mute(isMute);
                }
                break;
            case R.id.btn_speaker:
                isSpeaker = !isSpeaker;
                if (isSpeaker) {
                    btnSpeaker.setImageResource(R.drawable.ic_speaker_on);
                } else {
                    btnSpeaker.setImageResource(R.drawable.ic_speaker_off);
                }
                if (mCall != null) {
//                    mCall.setSpeakerphoneOn(isSpeaker);
                }
                break;
            case R.id.btn_answer:
                vControl.setVisibility(View.VISIBLE);
                if (mCall != null) {
                    btnAnswer.setVisibility(View.GONE);
                    if (!mCall.isVideoCall()) {
                        btnVideo.setVisibility(View.VISIBLE);
                    }
                    mCall.answer(this);
                }
                break;
            case R.id.btn_end:
                if (mCall != null) {
                    mCall.hangup();
                }
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
                if (mCall != null) {
//                    mCall.switchCamera(null);
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
        if (mCall != null) {
            if (!mCall.isVideoCall()) { // Send camera request
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("type", "cameraRequest");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
//                mCall.sendCallInfo(jsonObject);
            }
//            mCall.enableVideo(isVideo);
        }
    }

    private void acceptCameraRequest() {
        isVideo = true;
        btnVideo.setImageResource(R.drawable.ic_video);
//        mCall.enableVideo(true);
    }
}
