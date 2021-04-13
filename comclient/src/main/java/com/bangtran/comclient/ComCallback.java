package com.bangtran.comclient;

public interface  ComCallback {
    void onSuccess();
    void onError(ComError error);
}
