package com.tencent.msdk.dns.core.rank;

public class IpRankItem {
    private final String hostName;

    private final int port;

    public IpRankItem(String hostName, int port) {
        this.hostName = hostName;
        this.port = port;
    }

    public IpRankItem(String hostName) {
        this.hostName = hostName;
        this.port = 8080;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }
}
