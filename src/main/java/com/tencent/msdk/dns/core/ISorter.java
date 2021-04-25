package com.tencent.msdk.dns.core;

public interface ISorter {

    interface IFactory {

        ISorter create(int curNetStack);
    }

    // NOTE: ips可能为空数组
    void put(IDns dns, String[] ips);

    IpSet sort();
}
