package com.tencent.msdk.dns.core;

import androidx.annotation.NonNull;

import com.tencent.msdk.dns.BackupResolver;
import com.tencent.msdk.dns.BuildConfig;
import com.tencent.msdk.dns.DnsService;
import com.tencent.msdk.dns.base.executor.DnsExecutors;
import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.core.rest.aeshttp.AesCipherSuite;
import com.tencent.msdk.dns.core.rest.deshttp.DesCipherSuite;
import com.tencent.msdk.dns.core.rest.share.LookupExtra;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public final class ConfigFromServer {
    private static final int TIMEOUT = 2000;    // 请求超时时间 2000s
    private static final int MAX_RETRIES = 1;   // 重试次数
    private static final int FAIL_RETRY_INTERVAL = 5; // 失败延时任务启动 5min
    private static final int DEFAULT_EXPIRATION_TIME = 60; // 默认成功延时任务启动 60 min
    private static final int MIN_EXPIRATION_TIME = 1; // 成功延时任务启动最小 1 min
    private static final int MAX_EXPIRATION_TIME = 1440; // 成功延时任务启动最大 1440 min
    private static LookupExtra mLookupExtra;
    private static String mChannel;
    private static int mIndex = 0;

    /**
     * 域名接入服务初始化
     *
     * @param lookupExtra 加密数据
     * @param channel     http,https
     */
    public static void init(LookupExtra lookupExtra, String channel) {
        mLookupExtra = lookupExtra;
        mChannel = channel;
        doRequestWithRetry();
    }

    /**
     * 获取配置服务ip
     *
     * @param initServer 启动IP列表（国内，国际，http, https）
     * @return 初始化服务IP
     */
    private static String getInitIp(String[] initServer) {
        if (mIndex > initServer.length - 1) {
            mIndex = 0;
        }
        return initServer[mIndex];
    }

    /**
     * 组合配置请求URL
     *
     * @return URL（des 、 aes加密及https加密条件下url）
     */
    private static String getUrlStr() {
        if (mChannel.equals(Const.HTTPS_CHANNEL)) {
            if (BuildConfig.FLAVOR.equals("intl")) {
                DnsLog.d("httpdns-sdk-intl version still doesn't support https");
                return null;
            }
            return "https://" + getInitIp(BuildConfig.HTTPS_INIT_SERVER) + "/conf?token=" + mLookupExtra.token;
        } else {
            String alg = mChannel.equals(Const.AES_HTTP_CHANNEL) ? "aes" : "des";
            return "http://" + getInitIp(BuildConfig.HTTP_INIT_SERVER) + "/conf?id=" + mLookupExtra.bizId
                    + "&alg=" + alg;
        }
    }

    /**
     * 解析结果解密处理。日志上报配置，域名服务配置下发，动态配置IP处理。
     *
     * @param rawRspContent 返回的未解密的response
     */
    private static void handleResponse(@NonNull String rawRspContent) {
        boolean enableReport = false;
        boolean enableDomainServer = false;
        String ips = "";
        int expiredTime = DEFAULT_EXPIRATION_TIME;
        if (rawRspContent.length() > 0) {
            //  解密
            String rspContent;
            if (mChannel.equals(Const.HTTPS_CHANNEL)) {
                rspContent = rawRspContent;
            } else if (mChannel.equals(Const.AES_HTTP_CHANNEL)) {
                rspContent = AesCipherSuite.decrypt(rawRspContent, mLookupExtra.bizKey);
            } else {
                rspContent = DesCipherSuite.decrypt(rawRspContent, mLookupExtra.bizKey);
            }

            DnsLog.d("lookup config rsp: %s, raw: %s", rspContent, rawRspContent);
            // eg: "log:1|domain:0|ip:1.1.1.1;2.2.2.2|ttl:60"
            String[] configs = rspContent.split("\\|");
            for (String str : configs) {
                String[] item = str.split(":");
                if (item[0].contains("log")) {
                    enableReport = "1".equals(item[1]);
                }
                if (item[0].contains("domain")) {
                    enableDomainServer = "1".equals(item[1]);
                }
                if (item[0].contains("ip")) {
                    ips = item[1];
                }
                if (item[0].contains("ttl") && !item[1].isEmpty()) {
                    int ttl = Integer.parseInt(item[1]);
                    if (ttl >= MIN_EXPIRATION_TIME && ttl <= MAX_EXPIRATION_TIME) {
                        expiredTime = ttl;
                    }
                }
            }
            DnsService.setDnsConfigFromServer(enableReport, enableDomainServer);
            if (!ips.isEmpty()) {
                BackupResolver.getInstance().handleDynamicDNSIps(ips, expiredTime);
                // 请求成功后，按返回的时间延时调度
                scheduleRetryRequest(expiredTime);
            } else {
                // 域名服务: 动态IP服务优先级高于三网域名服务。
                BackupResolver.getInstance().getServerIps();
            }
        }
    }

    /**
     * 延时任务调度
     *
     * @param interval 单位：分钟min
     */
    public static void scheduleRetryRequest(int interval) {
        DnsLog.d("The delayed scheduling task will be executed after %s minutes.", interval);
        DnsExecutors.MAIN.schedule(
                new Runnable() {
                    @Override
                    public void run() {
                        doRequestWithRetry();
                    }
                }, (long) interval * 60 * 1000);
    }

    /**
     * 服务处理，超时后重试及延时更新任务下发
     * 失败后立即重试（默认）1次
     * 重试失败后，切换初始化IP，并按失败间隔下发延时任务
     */
    private static void doRequestWithRetry() {
        String urlString = getUrlStr();
        if (urlString == null || urlString.isEmpty()) return;
        int attempt = 0;
        while (attempt <= MAX_RETRIES) {
            try {
                String response = doRequest(urlString);
                handleResponse(response);
                return;
            } catch (SocketTimeoutException e) {
                DnsLog.d("Timeout occurred, %s retrying... (" + (attempt + 1) + "/" + (MAX_RETRIES + 1) + ")",
                        urlString);
                attempt++;
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        // 重试失败后，切换初始化IP，并按失败间隔（默认5min）下发延时任务重新请求动态解析IP。
        mIndex++;
        scheduleRetryRequest(FAIL_RETRY_INTERVAL);
    }

    /**
     * 发起配置请求，获取配置结果
     *
     * @param urlString 请求url
     * @return 返回response
     * @throws Exception
     */
    private static String doRequest(@NonNull String urlString) throws Exception {
        if (urlString.isEmpty()) return "";
        HttpURLConnection connection = null;
        BufferedReader reader;
        StringBuilder rawRspContent = new StringBuilder();
        String lineTxt;
        try {
            //  发起请求
            URL url = new URL(urlString);
            if (url.getProtocol().equalsIgnoreCase("https")) {
                connection = (HttpsURLConnection) url.openConnection();
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.connect();
            //  读取网络请求结果
            reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), "UTF-8"));
            while ((lineTxt = reader.readLine()) != null) {
                rawRspContent.append(lineTxt);
            }
            reader.close();
            return rawRspContent.toString();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
