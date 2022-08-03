package com.tencent.msdk.dns.core.ipRank;

public interface IpRankCallback {
    void onResult(String hostname, String[] sortedIps);
}
