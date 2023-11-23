package com.tencent.msdk.dns.core.rest.share.rsp;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.core.DnsDescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ResponseParser {

    private static final int RSP_GROUP_COUNT = 3;
    private static final int RSP_GROUP_COUNT_BATCH = 4;
    private static final int RSP_GROUP_UNSPECIFIC_COUNT = 5;
    private static final int RSP_GROUP_UNSPECIFIC_COUNT_BATCH = 6;
    private static final Pattern RSP_PATTERN = Pattern.compile("(.*),(.*)\\|(.*)");
    private static final Pattern RSP_PATTERN_BATCH = Pattern.compile("(.*)\\.:(.*),(.*)\\|(.*)");
    private static final Pattern RSP_UNSPECIFIC = Pattern.compile("(.*),(.*)-(.*),(.*)\\|(.*)");
    private static final Pattern RSP_UNSPECIFIC_BATCH = Pattern.compile("(.*)\\.:(.*),(.*)-(.*),(.*)\\|(.*)");

    private static final String IP_SPLITTER = ";";

    public static Response parseResponse(int family, String rawRsp) {
        if (TextUtils.isEmpty(rawRsp)) {
            return Response.EMPTY;
        }
        // 判断是否为批量查询，通过\n来判断
        String[] rspList = rawRsp.split("\n");

        if (family == DnsDescription.Family.UN_SPECIFIC) {
            return parseDoubResponse(rspList);
        }

        // format: ip;ip;ip;...,ttl|client ip
        // like: 123.59.226.2;123.59.226.3,303|59.37.125.43
        //  批量查询情况
        Map<String, Integer> ttl = new HashMap<>();
        if (rspList.length > 1) {
            ArrayList<String> ipsList = new ArrayList();
            String clientIp = "";
            // 遍历
            for (String rsp : rspList) {
                //  批量情况会携带域名信息
                Matcher rspMatcher = RSP_PATTERN_BATCH.matcher(rsp);
                if (!rspMatcher.matches() || RSP_GROUP_COUNT_BATCH != rspMatcher.groupCount()) {
//                    return Response.EMPTY;
                    continue;
                }
                try {
                    //  批量情况会携带域名信息
                    String host = rspMatcher.group(1);
                    clientIp = rspMatcher.group(4) + ",";
                    String[] tmpIps = rspMatcher.group(2).split(IP_SPLITTER);
                    //  将域名和ip组装为 host:ip的形式存入clientIp
                    for (String tmpIp : tmpIps) {
                        ipsList.add(host + ":" + tmpIp);
                    }
                    ttl.put(host, Integer.parseInt(rspMatcher.group(3)));
                } catch (Exception e) {
                    DnsLog.w(e, "Parse external response failed");
                    return Response.EMPTY;
                }
            }
            if (ipsList.size() == 0) {
                return Response.EMPTY;
            }
            String[] ips = ipsList.toArray(new String[ipsList.size()]);
            return new Response(family, clientIp, ips, ttl);
        } else {
            //  普通查询
            Matcher rspMatcher = RSP_PATTERN.matcher(rawRsp);
            if (!rspMatcher.matches() || RSP_GROUP_COUNT != rspMatcher.groupCount()) {
                return Response.EMPTY;
            }
            try {
                String clientIp = rspMatcher.group(3);
                String[] ips = rspMatcher.group(1).split(IP_SPLITTER);
                // 单个域名未传递，暂用onehost代替
                ttl.put("onehost", Integer.parseInt(rspMatcher.group(2)));
                return new Response(family, clientIp, ips, ttl);
            } catch (Exception e) {
                DnsLog.w(e, "Parse external response failed");
                return Response.EMPTY;
            }
        }
    }

    public static Response parseDoubResponse(@NonNull String[] rspList) {
        // format: ip;ip;ip;...,ttl|client ip
        // like: www.qq.com.:121.14.77.221;121.14.77.201,120-2402:4e00:1020:1404:0:9227:71a3:83d2;
        // 2402:4e00:1020:1404:0:9227:71ab:2b74,120|113.108.77.69
        //  批量查询情况
        Map<String, Integer> ttl = new HashMap<>();
        if (rspList.length > 1) {
            ArrayList<String> inet4IpsList = new ArrayList();
            ArrayList<String> inet6IpsList = new ArrayList();
            String clientIp = "";
            // 遍历
            for (String rsp : rspList) {
                //  批量情况会携带域名信息
                Matcher rspMatcher = RSP_UNSPECIFIC_BATCH.matcher(rsp);
                if (!rspMatcher.matches() || RSP_GROUP_UNSPECIFIC_COUNT_BATCH != rspMatcher.groupCount()) {
                    continue;
                }
                try {
                    //  批量情况会携带域名信息
                    String host = rspMatcher.group(1);
                    clientIp = rspMatcher.group(6) + ",";

                    String[] inet4Ips = rspMatcher.group(2).split(IP_SPLITTER);
                    String[] inet6Ips = rspMatcher.group(4).split(IP_SPLITTER);
                    // ttl先按ipv4, ipv6的最小值的获取
                    ttl.put(host, Math.min(Integer.parseInt(rspMatcher.group(3)),
                            Integer.parseInt(rspMatcher.group(5))));

                    //  将域名和ip组装为 host:ip的形式存入clientIp
                    for (String inet4Ip : inet4Ips) {
                        inet4IpsList.add(host + ":" + inet4Ip);
                    }

                    for (String inet6Ip : inet6Ips) {
                        inet6IpsList.add(host + ":" + inet6Ip);
                    }
                } catch (Exception e) {
                    DnsLog.w(e, "Parse external response failed");
                    return Response.EMPTY;
                }
            }
            if (inet4IpsList.size() == 0 && inet6IpsList.size() == 0) {
                return Response.EMPTY;
            }
            String[] inet4IpsTemp = inet4IpsList.toArray(new String[inet4IpsList.size()]);
            String[] inet6IpsTemp = inet6IpsList.toArray(new String[inet6IpsList.size()]);
            return new Response(clientIp, inet4IpsTemp, inet6IpsTemp, ttl);
        } else {
            try {
                // format: ip;ip;...;ip,ttl-ip;ip;...;ip,ttl|client ip
                Matcher rspMatcher = RSP_UNSPECIFIC.matcher(rspList[0]);
                if (!rspMatcher.matches() || RSP_GROUP_UNSPECIFIC_COUNT != rspMatcher.groupCount()) {
                    return Response.EMPTY;
                }

                String clientIp = rspMatcher.group(5);
                String[] inet4Ips = rspMatcher.group(1).split(IP_SPLITTER);
                String[] inet6Ips = rspMatcher.group(3).split(IP_SPLITTER);
                // ttl先按ipv4, ipv6的最小值的获取
                int value = Math.min(Integer.parseInt(rspMatcher.group(2)), Integer.parseInt(rspMatcher.group(4)));
                ttl.put("onehost", value);

                return new Response(clientIp, inet4Ips, inet6Ips, ttl);
            } catch (Exception e) {
                return Response.EMPTY;
            }

        }
    }
}
