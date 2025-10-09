package com.tencent.msdk.dns.core.rest.https;

import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.DnsDescription;
import com.tencent.msdk.dns.core.rest.share.AbsHttpDns;
import com.tencent.msdk.dns.core.rest.share.AbsHttpDnsConfig;
import com.tencent.msdk.dns.core.rest.share.LookupExtra;
import com.tencent.msdk.dns.core.rest.share.RequestBuilder;

import java.net.SocketAddress;


public final class HttpsDns extends AbsHttpDns {
    private AbsHttpDnsConfig mHttpDnsConfig = null;

    public HttpsDns(int family) {
        super(family);
        mHttpDnsConfig = new HttpsDnsConfig();
    }

    @Override
    public String getTag() {
        return Const.HTTPS_CHANNEL + "Dns(" + mFamily + ")";
    }

    @Override
    public String getDescriptionChannel() {
        return Const.HTTPS_CHANNEL;
    }

    @Override
    public String getTargetUrl(String dnsIp, String hostname, LookupExtra lookupExtra) {
        String reqContent;
        long timestamp = AbsHttpDns.getFutureTimestamp(10);
        // 域名字段中带上失效时间戳，用于底层鉴权
        String authHostname = hostname + ';' + timestamp;
        switch (mFamily) {
            case DnsDescription.Family.INET:
                reqContent = RequestBuilder.buildHttpsInetRequest(authHostname, lookupExtra.bizId, lookupExtra.token);
                break;
            case DnsDescription.Family.INET6:
                reqContent = RequestBuilder.buildHttpsInet6Request(authHostname, lookupExtra.bizId, lookupExtra.token);
                break;
            case DnsDescription.Family.UN_SPECIFIC:
                reqContent = RequestBuilder.buildHttpsDoubRequest(authHostname, lookupExtra.bizId, lookupExtra.token);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + mFamily);
        }
        return mHttpDnsConfig.getTargetUrl(dnsIp, reqContent);
    }

    @Override
    public String encrypt(String content, String key) {
        return content;
    }

    @Override
    public String decrypt(String content, String key) {
        return content;
    }

    @Override
    public SocketAddress getTargetSocketAddress(String dnsIp, int family) {
        return mHttpDnsConfig.getTargetSocketAddress(dnsIp, family);
    }
}
