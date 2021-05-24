package com.tencent.msdk.dns.core.rest.share;

import android.text.TextUtils;

import com.tencent.msdk.dns.core.Const;

import java.util.Locale;

public final class RequestBuilder {

    private static final String INET_REQUEST_FORMAT = "dn=%s&clientip=1&ttl=1&id=%s";
    private static final String INET6_REQUEST_FORMAT = "dn=%s&clientip=1&ttl=1&id=%s&type=aaaa";
    private static final String HTTPS_INET_REQUEST_FORMAT = "dn=%s&clientip=1&ttl=1&id=%s&token=%s";
    private static final String HTTPS_INET6_REQUEST_FORMAT = "dn=%s&clientip=1&ttl=1&id=%s&token=%s&type=aaaa";

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

    public static String buildHttpsInetRequest(String encryptedHostname, String bizId, String token) {
        return buildHttpsRequest(encryptedHostname, bizId, token, true);
    }

    public static String buildHttpsInet6Request(String encryptedHostname, String bizId, String token) {
        return buildHttpsRequest(encryptedHostname, bizId, token, false);
    }

    private static String buildHttpsRequest(String encryptedHostname, String bizId, String token, boolean inet) {
        if (TextUtils.isEmpty(bizId)) {
            throw new IllegalArgumentException("bizId".concat(Const.EMPTY_TIPS));
        }

        if (TextUtils.isEmpty(token)) {
            throw new IllegalArgumentException("token".concat(Const.EMPTY_TIPS));
        }

        if (TextUtils.isEmpty(encryptedHostname)) {
            return "";
        }
        return String.format(Locale.US,
                inet ? HTTPS_INET_REQUEST_FORMAT : HTTPS_INET6_REQUEST_FORMAT, encryptedHostname, bizId, token);
    }
}
