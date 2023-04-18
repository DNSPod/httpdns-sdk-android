package com.tencent.msdk.dns.core;

import com.tencent.msdk.dns.BackupResolver;
import com.tencent.msdk.dns.BuildConfig;
import com.tencent.msdk.dns.DnsService;
import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.core.rest.aeshttp.AesCipherSuite;
import com.tencent.msdk.dns.core.rest.deshttp.DesCipherSuite;
import com.tencent.msdk.dns.core.rest.share.LookupExtra;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public final class ConfigFromServer {
    private static boolean enableDomainServer;
    private static boolean enableReport;

    public static void init(LookupExtra lookupExtra, String channel) {
        String urlStr = "";
        if (channel.equals(Const.HTTPS_CHANNEL)) {
            if (BuildConfig.FLAVOR.equals("intl")) {
                DnsLog.d("httpdns-sdk-intl version still doesn't support https");
                return;
            }
            urlStr = "https://" + BuildConfig.HTTPS_INIT_SERVER + "/conf?token=" + lookupExtra.token;
        } else {
            String alg = channel.equals(Const.AES_HTTP_CHANNEL) ? "aes" : "des";
            urlStr = "http://" + BuildConfig.HTTP_INIT_SERVER + "/conf?id=" + lookupExtra.bizId + "&alg=" + alg;
        }

        HttpURLConnection connection = null;
        BufferedReader reader = null;
        String rawRspContent = "";
        String lineTxt = "";
        try {
            //  发起请求
            connection = (HttpURLConnection) new URL(urlStr).openConnection();
            if (connection instanceof HttpsURLConnection) {
                final HttpsURLConnection httpsURLConnection = (HttpsURLConnection) connection;
                httpsURLConnection.setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });
            }
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
            String rspContent = "";
            if (channel.equals(Const.HTTPS_CHANNEL)) {
                rspContent = rawRspContent;
            } else if (channel.equals(Const.AES_HTTP_CHANNEL)) {
                rspContent = AesCipherSuite.decrypt(rawRspContent, lookupExtra.bizKey);
            } else {
                rspContent = DesCipherSuite.decrypt(rawRspContent, lookupExtra.bizKey);
            }

            DnsLog.d("lookup byUrl: %s, rsp: %s, raw: %s", urlStr, rspContent, rawRspContent);

            String[] configs = rspContent.split("\\|");
            for (String str : configs) {
                String[] item = str.split(":");
                if (item[0].contains("log")) {
                    enableReport = "1".equals(item[1]);
                } else if (item[0].contains("domain")) {
                    enableDomainServer = "1".equals(item[1]);
                }
            }
            DnsService.setDnsConfigFromServer(enableReport, enableDomainServer);
        }
        BackupResolver.getInstance().getServerIps();
    }


}
