package com.github.davidmoten.aws.maven;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

final class Util {

    static String formatDateTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }
}
