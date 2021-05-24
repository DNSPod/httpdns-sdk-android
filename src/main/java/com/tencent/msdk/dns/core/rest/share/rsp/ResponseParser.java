package com.tencent.msdk.dns.core.rest.share.rsp;

import android.text.TextUtils;

import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.core.Const;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ResponseParser {

    private static final int RSP_GROUP_COUNT = 3;
    private static final int RSP_GROUP_COUNT_BATCH = 4;
    private static final Pattern RSP_PATTERN = Pattern.compile("(.*),(.*)\\|(.*)");
    private static final Pattern RSP_PATTERN_BATCH = Pattern.compile("(.*).:(.*),(.*)\\|(.*)");

    private static final String IP_SPLITTER = ";";

    public static Response parseResponse(int family, String rawRsp) {

        if (TextUtils.isEmpty(rawRsp)) {
            return Response.EMPTY;
        }
        // 判断是否为批量查询，通过\n来判断
        String[] rspList = rawRsp.split("\n");

        // format: ip;ip;ip;...,ttl|client ip
        // like: 123.59.226.2;123.59.226.3,303|59.37.125.43
        //  批量查询情况
        if (rspList.length > 1) {
            ArrayList<String> ipsList = new ArrayList();
            String clientIp = "";
            int ttl = 0;
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
                    for (int n = 0; n < tmpIps.length; n++) {
                        ipsList.add(host + ":" + tmpIps[n]);
                    }
                    ttl = Integer.parseInt(rspMatcher.group(3));
                } catch (Exception e) {
                    DnsLog.w(e, "Parse external response failed");
                    return Response.EMPTY;
                }
            }
            if(ipsList.size()==0){
                return Response.EMPTY;
            }
            String ips[]=ipsList.toArray(new String[ipsList.size()]);
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
                int ttl = Integer.parseInt(rspMatcher.group(2));
                return new Response(family, clientIp, ips, ttl);
            } catch (Exception e) {
                DnsLog.w(e, "Parse external response failed");
                return Response.EMPTY;
            }
        }
    }
}
