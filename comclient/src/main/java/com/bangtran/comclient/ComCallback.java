package com.bangtran.comclient;

import androidx.annotation.Nullable;

public interface  ComCallback<T> {
    void onSuccess(@Nullable T data);
    void onError(ComError error);
}
