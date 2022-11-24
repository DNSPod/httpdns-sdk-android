package com.tencent.msdk.dns.core;
import com.tencent.msdk.dns.DnsService;
import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.core.rest.deshttp.DesCipherSuite;
import com.tencent.msdk.dns.core.rest.share.LookupExtra;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public final class ConfigFromServer {
    private static boolean enableDomainServer;
    private static boolean enableReport;

    public static void init(LookupExtra lookupExtra) {
        String urlStr = "http://182.254.60.40/conf?id="+lookupExtra.bizId+"&alg=des";
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        String rawRspContent = "";
        String lineTxt = "";
        try {
            //  发起请求
            connection = (HttpURLConnection) new URL(urlStr).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.connect();
            //  读取网络请求结果
            reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), "UTF-8"));
            while ((lineTxt = reader.readLine()) != null) {
                lineTxt += '\n';
                rawRspContent += lineTxt;
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connection.disconnect();
        }
        if (!rawRspContent.isEmpty()) {
            //  解密
            String rspContent = DesCipherSuite.decrypt(rawRspContent, lookupExtra.bizKey);
            DnsLog.d("lookup byUrl: %s, rsp: %s, raw: %s", urlStr, rspContent, rawRspContent);

            String[] configs = rspContent.split("\\|");
            for (String str : configs) {
                String[] item = str.split(":");
                if (item[0].contains("log")) {
                    enableReport = Boolean.parseBoolean(item[1]);
                } else if (item[0].contains("domain")) {
                    enableDomainServer = Boolean.parseBoolean(item[1]);
                }
            }
            DnsService.getDnsConfig().setDnsConfigFromServer(enableReport, enableDomainServer);
        }
    }


}
