package com.isw2data.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;

public class Utils {
    private Utils(){
        throw new IllegalStateException("Utility class");
    }

    //converte unix time in localDateTime
    public static LocalDateTime convertTime(long unixSeconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(unixSeconds),
                TimeZone.getDefault().toZoneId());
    }

}