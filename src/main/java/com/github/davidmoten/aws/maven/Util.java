package com.github.davidmoten.aws.maven;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.amazonaws.ClientConfiguration;

final class Util {

    static String formatDateTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    static ClientConfiguration createConfiguration(Proxy proxy) {
        ClientConfiguration cc = new ClientConfiguration();
        if (proxy.host != null) {
            cc.setProxyHost(proxy.host);
            cc.setProxyPort(proxy.port);
            if (proxy.username != null) {
                cc.setProxyUsername(proxy.username);
                cc.setProxyPassword(proxy.password);
            }
        }
        return cc;
    }
}
