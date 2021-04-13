package com.bangtran.comapp;

import android.media.Ringtone;

import com.bangtran.comclient.ComCall;

import java.util.HashMap;
import java.util.Map;

public class Common {
    public static Map<String, ComCall> callsMap = new HashMap<>();
    public static boolean isInCall = false;
    public static Ringtone ringtone;
    public static boolean isAppInBackground = false;
}
