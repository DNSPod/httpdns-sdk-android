package com.tencent.msdk.dns.core;

public interface IRetry {

    int maxRetryTimes();

    void retryNonBlock(LookupContext lookupContext);

    <LookupExtra extends IDns.ILookupExtra>
    void retryBlock(LookupContext<LookupExtra> lookupContext);
}
