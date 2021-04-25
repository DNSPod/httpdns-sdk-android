package com.tencent.msdk.dns.base.utils;

import org.junit.Assert;
import org.junit.Test;

public class IpValidatorTest {

    @Test
    public void testIsV6IP() {
        Assert.assertFalse(IpValidator.isV6Ip("p21.tcdn.qq.com."));
    }
}
