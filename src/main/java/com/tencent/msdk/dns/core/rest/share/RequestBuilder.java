package com.tencent.msdk.dns.core.rest.share;

import android.text.TextUtils;

import com.tencent.msdk.dns.core.Const;

import java.util.Locale;

public final class RequestBuilder {

    private static final String INET_REQUEST_FORMAT = "dn=%s&clientip=1&ttl=1&id=%s";
    private static final String INET6_REQUEST_FORMAT = "dn=%s&clientip=1&ttl=1&id=%s&type=aaaa";

    public static String buildInetRequest(String encryptedHostname, String bizId) {
        return buildRequest(encryptedHostname, bizId, true);
    }

    public static String buildInet6Request(String encryptedHostname, String bizId) {
        return buildRequest(encryptedHostname, bizId, false);
    }

    private static String buildRequest(String encryptedHostname, String bizId, boolean inet) {
        if (TextUtils.isEmpty(bizId)) {
            throw new IllegalArgumentException("bizId".concat(Const.EMPTY_TIPS));
        }

        if (TextUtils.isEmpty(encryptedHostname)) {
            return "";
        }
        return String.format(Locale.US,
                inet ? INET_REQUEST_FORMAT : INET6_REQUEST_FORMAT, encryptedHostname, bizId);
    }
}
