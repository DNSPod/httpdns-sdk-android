package com.tencent.msdk.dns;

public interface HttpDnsResponseObserver {
    void onHttpDnsResponse(String tag, String domain, Object ipResultSemicolonSep);
}