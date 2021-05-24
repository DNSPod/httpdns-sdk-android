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
        String reqContent = DnsDescription.Family.INET == mFamily ?
                RequestBuilder.buildHttpsInetRequest(hostname, lookupExtra.bizId, lookupExtra.token) :
                RequestBuilder.buildHttpsInet6Request(hostname, lookupExtra.bizId, lookupExtra.token);
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
