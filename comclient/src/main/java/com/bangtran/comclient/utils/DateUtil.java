package com.bangtran.comclient.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {
    public static final String DATE_FORMAT_1 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static long convertStringToMilliseconds(String date, String format) throws ParseException {
        return new SimpleDateFormat(format).parse(date).getTime();
    }

    public static long getMillisecondsTime(){
        return new Date().getTime();
    }
}
