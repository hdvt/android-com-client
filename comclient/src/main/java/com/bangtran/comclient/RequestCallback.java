package com.bangtran.comclient;

import androidx.annotation.Nullable;

import org.json.JSONObject;

public interface RequestCallback {
    void onSuccess(@Nullable JSONObject data);
    void onError(ComError error);
}
