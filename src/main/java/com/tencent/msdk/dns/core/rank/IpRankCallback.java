package com.tencent.msdk.dns.core.rank;

public interface IpRankCallback {
    void onResult(String hostname, String[] sortedIps);
}
