package com.tencent.msdk.dns.core.rest.aeshttp;

import android.text.TextUtils;

import com.tencent.msdk.dns.core.rest.share.AbsHttpDnsConfig;
import java.util.Locale;

final class AesHttpDnsConfig extends AbsHttpDnsConfig {
    private static final String TARGET_URL_FORMAT = "http://%s/d?%s&alg=aes";
    private static final int TARGET_PORT = 80;

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
