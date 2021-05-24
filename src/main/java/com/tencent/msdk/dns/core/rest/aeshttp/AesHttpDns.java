package com.tencent.msdk.dns.core.rest.aeshttp;

import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.DnsDescription;

import com.tencent.msdk.dns.core.rest.share.AbsHttpDns;

import com.tencent.msdk.dns.core.rest.share.AbsHttpDnsConfig;
import com.tencent.msdk.dns.core.rest.share.LookupExtra;
import com.tencent.msdk.dns.core.rest.share.RequestBuilder;

import java.net.SocketAddress;


public final class AesHttpDns extends AbsHttpDns {
    private AbsHttpDnsConfig mHttpDnsConfig = null;

    public AesHttpDns(int family) {
        super(family);
        mHttpDnsConfig = new AesHttpDnsConfig();
    }

    @Override
    public String getTag() {
        return Const.AES_HTTP_CHANNEL + "Dns(" + mFamily + ")";
    }

    @Override
    public String getDescriptionChannel() {
        return Const.AES_HTTP_CHANNEL;
    }

    @Override
    public String getTargetUrl(String dnsIp, String hostname, LookupExtra lookupExtra) {
        String encryptHostname = AesCipherSuite.encrypt(hostname, lookupExtra.bizKey);
        String reqContent = DnsDescription.Family.INET == mFamily ?
            RequestBuilder.buildInetRequest(encryptHostname, lookupExtra.bizId) :
            RequestBuilder.buildInet6Request(encryptHostname, lookupExtra.bizId);
        return mHttpDnsConfig.getTargetUrl(dnsIp, reqContent);
    }

    @Override
    public String encrypt(String content, String key) {
        return AesCipherSuite.encrypt(content, key);
    }

    @Override
    public String decrypt(String content, String key) {
        return AesCipherSuite.decrypt(content, key);
    }

    @Override
    public SocketAddress getTargetSocketAddress(String dnsIp, int family) {
        return mHttpDnsConfig.getTargetSocketAddress(dnsIp, family);
    }
}
