package com.tencent.msdk.dns.base.utils;

import android.text.TextUtils;

import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.core.Const;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public final class HttpHelper {

    private static final String CRLF = "\r\n";

    private static final String GET_METHOD = "GET";
    private static final String HTTP_VERSION = "HTTP/1.1";

    private static final String HOST_HEADER = "Host";

    private static final String OK = "OK";

    public static String getRequest(String urlStr) {
        if (TextUtils.isEmpty(urlStr)) {
            throw new IllegalArgumentException("urlStr".concat(Const.EMPTY_TIPS));
        }

        try {
            URL url = new URL(urlStr);
            String host = url.getHost();
            String file = url.getFile();
            return GET_METHOD + ' ' + file + ' ' + HTTP_VERSION + CRLF +
                    "Connection: close" + CRLF +
                    HOST_HEADER + ": " + host + CRLF +
                    CRLF;
        } catch (MalformedURLException e) {
            return "";
        }
    }

    public static int findEndOfString(String sb, int offset) {
        int result;
        for (result = sb.length(); result > offset; result--) {
            if (!Character.isWhitespace(sb.charAt(result - 1))) {
                break;
            }
        }
        return result;
    }

    public static int findNonWhitespace(String sb, int offset) {
        int result;
        for (result = offset; result < sb.length(); result++) {
            if (!Character.isWhitespace(sb.charAt(result))) {
                break;
            }
        }
        return result;
    }

    public static void splitLineAddHeader(String sb, Map<String, String> headers) {
        final int length = sb.length();
        int nameStart;
        int nameEnd;
        int colonEnd;
        int valueStart;
        int valueEnd;

        nameStart = findNonWhitespace(sb, 0);
        for (nameEnd = nameStart; nameEnd < length; nameEnd++) {
            char ch = sb.charAt(nameEnd);
            if (ch == ':' || Character.isWhitespace(ch)) {
                break;
            }
        }

        for (colonEnd = nameEnd; colonEnd < length; colonEnd++) {
            if (sb.charAt(colonEnd) == ':') {
                colonEnd++;
                break;
            }
        }

        valueStart = findNonWhitespace(sb, colonEnd);
        valueEnd = findEndOfString(sb, valueStart);

        String key = sb.substring(nameStart, nameEnd);
        if (valueStart > valueEnd) { // ignore
        } else {
            String value = sb.substring(valueStart, valueEnd);
            key = key.toLowerCase();
            // DnsLog.d("HttpDns header key:" + key + ", value:" + value);
            String v = headers.get(key);
            if (v != null) {
                // 重复的使用逗号分隔
                value = v + "," + value;
            }
            headers.put(key, value);
        }
    }

    public static boolean checkHttpRspFinished(String rawRsp) {
        if (TextUtils.isEmpty(rawRsp)) {
            return false;
        }
        // 如果以\r\n\r\n为结尾则认为body为空字符串
        int index = rawRsp.indexOf(CRLF + CRLF);
        if (index < 0) {
            DnsLog.d("HttpDns not finish header recv");
            return false;
        }
        String body = rawRsp.substring(index + 4);
        Map<String, String> map = new HashMap<>();
        String[] lines = rawRsp.split(CRLF);
        // 跳过第一行
        for (int i = 1; i < lines.length; ++i) {
            splitLineAddHeader(lines[i], map);
        }
        String contentLength = map.get("Content-Length".toLowerCase());
        DnsLog.d("HttpDns Content-Length len:%s, recved body:%d", contentLength, body.length());
        if (contentLength != null) {
            try {
                int cl = Integer.parseInt(contentLength);
                if (body.length() == cl) {
                    return true;
                }
            } catch (Exception ignore) {
            }
        }
        return false;
    }

    public static String responseBody(String rawRsp) {
        if (TextUtils.isEmpty(rawRsp)) {
            throw new IllegalArgumentException("rawRsp".concat(Const.EMPTY_TIPS));
        }

        String[] rsps = rawRsp.split(CRLF + CRLF);
        if (2 != rsps.length) {
            return "";
        }
        if (!rsps[0].contains(OK)) {
            return "";
        }
        return rsps[1];
    }

    public static int responseStatus(String rawRsp) {
        try {
            return Integer.parseInt(rawRsp.substring(9, 12));
        } catch (Exception ignore) {
            return 0;
        }

    }
}
