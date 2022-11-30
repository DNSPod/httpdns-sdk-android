package com.tencent.msdk.dns.core.rest.share;

import android.text.TextUtils;

import com.tencent.msdk.dns.core.Const;

import java.util.Locale;

public final class RequestBuilder {

    private static final String INET_REQUEST_FORMAT = "dn=%s&clientip=1&ttl=1&id=%s&alg=%s";
    private static final String INET6_REQUEST_FORMAT = "dn=%s&clientip=1&ttl=1&id=%s&alg=%s&type=aaaa";
    private static final String DOUB_REQUEST_FORMAT = "dn=%s&clientip=1&ttl=1&id=%s&alg=%s&type=addrs";
    private static final String HTTPS_INET_REQUEST_FORMAT = "dn=%s&clientip=1&ttl=1&id=%s&token=%s";
    private static final String HTTPS_INET6_REQUEST_FORMAT = "dn=%s&clientip=1&ttl=1&id=%s&token=%s&type=aaaa";
    private static final String HTTPS_DOUB_REQUEST_FORMAT = "dn=%s&clientip=1&ttl=1&id=%s&token=%s&type=addrs";

    public static String buildInetRequest(String encryptedHostname, String bizId, String alg) {
        return buildRequest(encryptedHostname, bizId, alg, INET_REQUEST_FORMAT);
    }

    public static String buildInet6Request(String encryptedHostname, String bizId, String alg) {
        return buildRequest(encryptedHostname, bizId, alg, INET6_REQUEST_FORMAT);
    }

    public static String buildDoubRequest(String encryptedHostname, String bizId, String alg) {
        return buildRequest(encryptedHostname, bizId, alg, DOUB_REQUEST_FORMAT);
    }

    private static String buildRequest(String encryptedHostname, String bizId, String alg, String format) {
        if (TextUtils.isEmpty(bizId)) {
            throw new IllegalArgumentException("bizId".concat(Const.EMPTY_TIPS));
        }

        if (TextUtils.isEmpty(alg)) {
            throw new IllegalArgumentException("alg".concat(Const.EMPTY_TIPS));
        }

        if (TextUtils.isEmpty(encryptedHostname)) {
            return "";
        }
        return String.format(Locale.US,
                format, encryptedHostname, bizId, alg);
    }

    public static String buildHttpsInetRequest(String encryptedHostname, String bizId, String token) {
        return buildHttpsRequest(encryptedHostname, bizId, token, HTTPS_INET_REQUEST_FORMAT);
    }

    public static String buildHttpsInet6Request(String encryptedHostname, String bizId, String token) {
        return buildHttpsRequest(encryptedHostname, bizId, token, HTTPS_INET6_REQUEST_FORMAT);
    }

    public static String buildHttpsDoubRequest(String encryptedHostname, String bizId, String token) {
        return buildHttpsRequest(encryptedHostname, bizId, token, HTTPS_DOUB_REQUEST_FORMAT);
    }

    private static String buildHttpsRequest(String encryptedHostname, String bizId, String token, String format) {
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
                format, encryptedHostname, bizId, token);
    }
}
