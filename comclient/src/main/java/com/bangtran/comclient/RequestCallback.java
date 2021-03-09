package com.bangtran.comclient;

import org.json.JSONObject;

public interface RequestCallback {
    void onSuccess(JSONObject data);
    void onError(JSONObject error);
}
