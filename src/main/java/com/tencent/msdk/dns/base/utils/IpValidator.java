package com.tencent.msdk.dns.base.utils;

import android.text.TextUtils;

import java.util.regex.Pattern;

public final class IpValidator {

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\." +
                    "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\." +
                    "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\." +
                    "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$");
    // 这里主要是为了和iOS对齐, 其实用Inet6Address.getByName()来判断更好
    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|" +
                    "([0-9a-fA-F]{1,4}:){1,7}:|" +
                    "([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|" +
                    "([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|" +
                    "([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|" +
                    "([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|" +
                    "([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|" +
                    "[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|" +
                    ":((:[0-9a-fA-F]{1,4}){1,7}|:)|" +
                    "fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]+|" +
                    "::ffff(:0{1,4})?:((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])|" +
                    "([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])");

    public static boolean isV4Ip(/* @Nullable */String ip) {
        String realIp = formatIp(ip);
        return !TextUtils.isEmpty(realIp) && IPV4_PATTERN.matcher(realIp).matches();
    }

    public static boolean isV6Ip(/* @Nullable */String ip) {
        String realIp = formatIp(ip);
        return !TextUtils.isEmpty(ip) && IPV6_PATTERN.matcher(ip).matches() || !TextUtils.isEmpty(realIp) && IPV6_PATTERN.matcher(realIp).matches();
    }

    // 带域名的ip转换适配批量查询
    private static String formatIp(String ip){
        // 批量ip例子：baidu.com:39.156.69.79
        if(ip.contains(":")){
            int i=ip.indexOf(":");
            return ip.substring(i+1);
        }
        return ip;
    }
}
