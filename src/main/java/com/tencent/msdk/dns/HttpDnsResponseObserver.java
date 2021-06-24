package com.tencent.msdk.dns;

import com.tencent.msdk.dns.core.IpSet;

public interface HttpDnsResponseObserver {
    void onHttpDnsResponse(String tag, String domain, Object ipResultSemicolonSep);
}