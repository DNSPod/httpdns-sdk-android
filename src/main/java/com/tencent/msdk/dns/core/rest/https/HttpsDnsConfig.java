package com.tencent.msdk.dns.core.rest.https;

import android.text.TextUtils;

import com.tencent.msdk.dns.core.rest.share.AbsHttpDnsConfig;

import java.util.Locale;

public class HttpsDnsConfig extends AbsHttpDnsConfig {
    private static final String TARGET_URL_FORMAT = "https://%s/d?%s";
    private static final int TARGET_PORT = 443;

    @Override
    public String getTargetUrl(String host, String content) {
        if (TextUtils.isEmpty(content)) {
            return "";
        }
        return String.format(Locale.US, TARGET_URL_FORMAT, host, content);
    }

    @Override
    public int getBizTargetPort() {
        return TARGET_PORT;
    }
}
