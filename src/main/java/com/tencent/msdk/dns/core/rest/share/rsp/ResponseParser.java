package com.tencent.msdk.dns.core.rest.share.rsp;

import android.text.TextUtils;

import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.core.Const;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ResponseParser {

    private static final int RSP_GROUP_COUNT = 3;
    private static final Pattern RSP_PATTERN = Pattern.compile("(.*),(.*)\\|(.*)");

    private static final String IP_SPLITTER = ";";

    public static Response parseResponse(int family, String rawRsp) {
        if (TextUtils.isEmpty(rawRsp)) {
            return Response.EMPTY;
        }
        // format: ip;ip;ip;...,ttl|client ip
        // like: 123.59.226.2;123.59.226.3,303|59.37.125.43
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
