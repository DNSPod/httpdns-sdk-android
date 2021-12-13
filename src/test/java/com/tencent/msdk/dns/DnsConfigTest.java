package com.tencent.msdk.dns;

import org.junit.Test;

import org.junit.Assert;

public final class DnsConfigTest {
    @Test
    public void testContains() {
        DnsConfig dnsConfig = new DnsConfig.Builder()
                .protectedDomains("www.qq.com", "weixin.qq.com", "sports.qq.com", "video.qq.com", "bugly.qq.com", "beacon.qq.com")
                .preLookupDomains("www.qq.com", "weixin.qq.com", "sports.qq.com", "video.qq.com", "www.taobao.com")
                .asyncLookupDomains("weixin.qq.com", "sports.qq.com", "video.qq.com", "bugly.qq.com", "www.baidu.com")
                .dnsId("3031")
                .dnsKey("DAyJYOlU")
                .dnsIp("119.29.29.98")
                .build();
        checkContains(dnsConfig);

        dnsConfig = new DnsConfig.Builder()
                .preLookupDomains("www.qq.com", "weixin.qq.com", "sports.qq.com", "video.qq.com", "www.taobao.com")
                .protectedDomains("www.qq.com", "weixin.qq.com", "sports.qq.com", "video.qq.com", "bugly.qq.com", "beacon.qq.com")
                .asyncLookupDomains("weixin.qq.com", "sports.qq.com", "video.qq.com", "bugly.qq.com", "www.baidu.com")
                .dnsId("3031")
                .dnsKey("DAyJYOlU")
                .dnsIp("119.29.29.98")
                .build();
        checkContains(dnsConfig);

        dnsConfig = new DnsConfig.Builder()
                .protectedDomains("www.qq.com", "weixin.qq.com", "sports.qq.com", "video.qq.com", "bugly.qq.com", "beacon.qq.com")
                .asyncLookupDomains("weixin.qq.com", "sports.qq.com", "video.qq.com", "bugly.qq.com", "www.baidu.com")
                .preLookupDomains("www.qq.com", "weixin.qq.com", "sports.qq.com", "video.qq.com", "www.taobao.com")
                .dnsId("3031")
                .dnsKey("DAyJYOlU")
                .dnsIp("119.29.29.98")
                .build();
        checkContains(dnsConfig);

        dnsConfig = new DnsConfig.Builder()
                .asyncLookupDomains("weixin.qq.com", "sports.qq.com", "video.qq.com", "bugly.qq.com", "www.baidu.com")
                .preLookupDomains("www.qq.com", "weixin.qq.com", "sports.qq.com", "video.qq.com", "www.taobao.com")
                .protectedDomains("www.qq.com", "weixin.qq.com", "sports.qq.com", "video.qq.com", "bugly.qq.com", "beacon.qq.com")
                .dnsId("3031")
                .dnsKey("DAyJYOlU")
                .dnsIp("119.29.29.98")
                .build();
        checkContains(dnsConfig);
    }

    private void checkContains(DnsConfig dnsConfig) {
        Assert.assertEquals(6, dnsConfig.protectedDomains.size());
        Assert.assertEquals(4, dnsConfig.preLookupDomains.size());
        Assert.assertEquals(3, dnsConfig.asyncLookupDomains.size());
        for (String domain : dnsConfig.preLookupDomains) {
            boolean contains = false;
            for (DnsConfig.WildcardDomain wildcardDomain : dnsConfig.protectedDomains) {
                if (wildcardDomain.contains(domain)) {
                    contains = true;
                    break;
                }
            }
            Assert.assertTrue(contains);
        }
        for (String domain : dnsConfig.asyncLookupDomains) {
            Assert.assertTrue(dnsConfig.preLookupDomains.contains(domain));
        }
    }

    @Test
    public void testMaxNumOfPreLookupDomains() {
        DnsConfig dnsConfig = new DnsConfig.Builder()
                .maxNumOfPreLookupDomains(3)
                .preLookupDomains("www.qq.com", "weixin.qq.com", "sports.qq.com", "video.qq.com", "www.taobao.com")
                .dnsId("3031")
                .dnsKey("DAyJYOlU")
                .dnsIp("119.29.29.98")
                .build();
        Assert.assertEquals(3, dnsConfig.preLookupDomains.size());
        Assert.assertTrue(dnsConfig.preLookupDomains.contains("www.qq.com"));
        Assert.assertTrue(dnsConfig.preLookupDomains.contains("weixin.qq.com"));
        Assert.assertTrue(dnsConfig.preLookupDomains.contains("sports.qq.com"));
        Assert.assertFalse(dnsConfig.preLookupDomains.contains("video.qq.com"));
        Assert.assertFalse(dnsConfig.preLookupDomains.contains("www.taobao.com"));

        dnsConfig = new DnsConfig.Builder()
                .maxNumOfPreLookupDomains(3)
                .asyncLookupDomains("weixin.qq.com", "sports.qq.com", "video.qq.com", "bugly.qq.com", "www.baidu.com")
                .dnsId("3031")
                .dnsKey("DAyJYOlU")
                .dnsIp("119.29.29.98")
                .build();
        Assert.assertEquals(3, dnsConfig.asyncLookupDomains.size());
        Assert.assertTrue(dnsConfig.asyncLookupDomains.contains("weixin.qq.com"));
        Assert.assertTrue(dnsConfig.asyncLookupDomains.contains("sports.qq.com"));
        Assert.assertTrue(dnsConfig.asyncLookupDomains.contains("video.qq.com"));
        Assert.assertFalse(dnsConfig.asyncLookupDomains.contains("bugly.qq.com"));
        Assert.assertFalse(dnsConfig.asyncLookupDomains.contains("www.baidu.com"));
    }

    @Test
    public void testWildcardDomains() {
        DnsConfig dnsConfig = new DnsConfig.Builder()
                .protectedDomains("*.qq.com")
                .preLookupDomains("www.qq.com", "weixin.qq.com", "sports.qq.com", "video.qq.com", "www.taobao.com")
                .dnsId("3031")
                .dnsKey("DAyJYOlU")
                .dnsIp("119.29.29.98")
                .build();
        Assert.assertEquals(4, dnsConfig.preLookupDomains.size());
        Assert.assertTrue(dnsConfig.preLookupDomains.contains("www.qq.com"));
        Assert.assertTrue(dnsConfig.preLookupDomains.contains("weixin.qq.com"));
        Assert.assertTrue(dnsConfig.preLookupDomains.contains("sports.qq.com"));
        Assert.assertTrue(dnsConfig.preLookupDomains.contains("video.qq.com"));
        Assert.assertFalse(dnsConfig.preLookupDomains.contains("www.taobao.com"));
    }
}
