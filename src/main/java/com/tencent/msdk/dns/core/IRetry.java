package com.tencent.msdk.dns.core;

public interface IRetry {

    int maxRetryTimes();

    void retryNonBlock(LookupContext lookupContext);

    <LookupExtraT extends IDns.ILookupExtra>
    void retryBlock(LookupContext<LookupExtraT> lookupContext);
}
