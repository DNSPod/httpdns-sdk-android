package com.tencent.msdk.dns.report;

import android.content.Context;
import android.telephony.TelephonyManager;

import com.tencent.msdk.dns.BuildConfig;
import com.tencent.msdk.dns.base.log.DnsLog;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Atta主要用于容灾上报和灯塔（用户个人上报）用途不同
 */
public class AttaHelper {
    private static final String ATTA_URL = "https://h.trace.qq.com/kv";
    private static final String ATTA_ID = BuildConfig.ATTA_ID;
    private static final String ATTA_TOKEN = BuildConfig.ATTA_TOKEN;
    private static final String SDKVERSION = BuildConfig.VERSION_NAME;
    private static final String DEVICEMODEL = getSystemModel();
    private static final String SYSTEMNANE = "Andriod";
    private static final String SYSTEMVERSION = getSystemVersion();
    private static final String SESSIONID = Session.getSessionId();

    public static Runnable report(final String carrier,
                                  final String networkType,
                                  final String dnsId,
                                  final String appId,
                                  final String encryptType,
                                  final String eventName,
                                  final long eventTime,
                                  final String dnsIp,
                                  final long spend,
                                  final long ldns_spend,
                                  final String req_dn,
                                  final String req_type,
                                  final long req_timeout,
                                  final int req_ttl,
                                  final long errorCode,
                                  final int statusCode,
                                  final boolean isCache,
                                  final int count,
                                  final String ldns,
                                  final String hdns) {
        return new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(ATTA_URL
                            + "?attaid=" + ATTA_ID
                            + "&token=" + ATTA_TOKEN
                            + "&carrier=" + carrier
                            + "&networkType=" + networkType
                            + "&dnsId=" + dnsId
                            + "&appId=" + appId
                            + "&encryptType=" + encryptType
                            + "&eventName=" + eventName
                            + "&eventTime=" + eventTime
                            + "&dnsIp=" + dnsIp
                            + "&sdkVersion=" + SDKVERSION
                            + "&deviceName=" + DEVICEMODEL
                            + "&systemName=" + SYSTEMNANE
                            + "&systemVersion=" + SYSTEMVERSION
                            + "&spend=" + spend
                            + "&ldns_spend=" + ldns_spend
                            + "&req_dn=" + req_dn
                            + "&req_type=" + req_type
                            + "&req_timeout=" + req_timeout
                            + "&req_ttl=" + req_ttl
                            + "&errorCode=" + errorCode
                            + "&statusCode=" + statusCode
                            + "&sessionId=" + SESSIONID
                            + "&isCache=" + (isCache ? 1 : 0)
                            + "&count=" + count
                            + "&ldns=" + ldns
                            + "&hdns=" + hdns
                    );
                    DnsLog.d("开始Atta上报：" + url);
                    connection = (HttpURLConnection) url.openConnection();
                    //设置请求方法
                    connection.setRequestMethod("GET");
                    //设置连接超时时间（毫秒）
                    connection.setConnectTimeout(2000);
                    //设置读取超时时间（毫秒）
                    connection.setReadTimeout(2000);
                    connection.connect();
                    int respCode = connection.getResponseCode();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (connection != null) {//关闭连接
                        connection.disconnect();
                        DnsLog.d("Atta上报关闭");
                    }
                }
            }
        };
    }

    // China Mobile: 46000 46002 46007
    // China Unicom: 46001 46006
    // China Telecom: 46003 46005 46011
    // China Spacecom: 46004
    // China Tietong: 46020
    public static String getSimOperator(Context context) {
        String carrierCode = "-1";
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        // 如果手机卡服务缺失，或者未就绪，则返回错误的-1
        if (null == tm || TelephonyManager.SIM_STATE_READY != tm.getSimState()) {
            return carrierCode;
        } else {
            // 通过getSimOperator方法获取运营商设备信息
            carrierCode = tm.getSimOperator();    //mobile data
            //String netOperator = tm.getNetworkOperator();	//Dial Net
            return carrierCode;
        }
    }

    /**
     * 获取当前手机系统版本号
     *
     * @return 系统版本号
     */
    public static String getSystemVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    /**
     * 获取手机型号
     *
     * @return 手机型号
     */
    public static String getSystemModel() {
        return android.os.Build.MODEL;
    }

    /**
     * 获取请求网络栈类型
     *
     * @param curNetStack 当前网络栈
     * @return 请求类型 a/aaaa/dual
     */
    public static String getReqType(int curNetStack) {
        String reqType = null;
        switch (curNetStack) {
            case 1:
                reqType = "a";
                break;
            case 2:
                reqType = "aaaa";
                break;
            case 3:
                reqType = "dual";
                break;
        }
        return reqType;
    }
}