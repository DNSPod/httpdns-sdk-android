package com.tencent.msdk.dns.base.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

public final class NetworkUtils {

    private static final int NETWORK_CLASS_NO_NET = 0;
    private static final int NETWORK_CLASS_UNKNOWN = 0;
    private static final int NETWORK_CLASS_2_G = 1;
    private static final int NETWORK_CLASS_3_G = 2;
    private static final int NETWORK_CLASS_4_G = 3;
    private static final int NETWORK_CLASS_WIFI = 4;
    private static final int NETWORK_CLASS_ETHERNET = 5;

    public static String getNetworkName(/* @Nullable */Context context) {
        return getNetworkName(getNetworkState(context));
    }

    @SuppressWarnings("WeakerAccess")
    public static String getNetworkName(int netState) {
        switch (netState) {
            case NETWORK_CLASS_2_G:
                return "2G";
            case NETWORK_CLASS_3_G:
                return "3G";
            case NETWORK_CLASS_4_G:
                return "4G";
            case NETWORK_CLASS_WIFI:
                return "wifi";
            case NETWORK_CLASS_ETHERNET:
                return "ethernet";
            default:
                return "unknown";
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static int getNetworkState(/* @Nullable */Context context) {
        if (null == context) {
            return NETWORK_CLASS_UNKNOWN;
        }
        try {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (null == connectivityManager) {
                return NETWORK_CLASS_UNKNOWN;
            }
            NetworkInfo info = connectivityManager.getActiveNetworkInfo();
            if (null == info) {
                return NETWORK_CLASS_NO_NET;
            }
            switch (info.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                    return NETWORK_CLASS_WIFI;
                case ConnectivityManager.TYPE_MOBILE:
                    return getMobileNetworkClass(info);
                case ConnectivityManager.TYPE_ETHERNET:
                    return NETWORK_CLASS_ETHERNET;
                default:
                    return NETWORK_CLASS_UNKNOWN;
            }
        } catch (Exception e) {
            return NETWORK_CLASS_UNKNOWN;
        }
    }

    private static int getMobileNetworkClass(NetworkInfo info) {
        if (null == info) {
            return NETWORK_CLASS_NO_NET;
        }
        switch (info.getSubtype()) {
            case TelephonyManager.NETWORK_TYPE_UNKNOWN: // 0
                return NETWORK_CLASS_UNKNOWN;
            case TelephonyManager.NETWORK_TYPE_GPRS: // 1
            case TelephonyManager.NETWORK_TYPE_EDGE: // 2
            case TelephonyManager.NETWORK_TYPE_CDMA: // 4
            case TelephonyManager.NETWORK_TYPE_1xRTT: // 7
            case TelephonyManager.NETWORK_TYPE_IDEN: // 11
            case 16: // case TelephonyManager.NETWORK_TYPE_GSM [api25 16]
                return NETWORK_CLASS_2_G;
            case TelephonyManager.NETWORK_TYPE_UMTS: // 3
            case TelephonyManager.NETWORK_TYPE_EVDO_0: //5
            case TelephonyManager.NETWORK_TYPE_EVDO_A://6
            case TelephonyManager.NETWORK_TYPE_HSDPA: //8
            case TelephonyManager.NETWORK_TYPE_HSUPA://9
            case TelephonyManager.NETWORK_TYPE_HSPA://10
            case TelephonyManager.NETWORK_TYPE_EVDO_B: // [api9] 12
            case TelephonyManager.NETWORK_TYPE_EHRPD: // [api11]  14
            case TelephonyManager.NETWORK_TYPE_HSPAP: // [api13]  15
            case 17:  // [api25] NETWORK_TYPE_TD_SCDMA
                return NETWORK_CLASS_3_G;
            // 圈复杂度优化，NETWORK_CLASS_4_G返回与default一致。
//            case TelephonyManager.NETWORK_TYPE_LTE: // api11  13
//            case 18: // TelephonyManager.NETWORK_TYPE_IWLAN [api25 18]
//                return NETWORK_CLASS_4_G;
            default:
                return NETWORK_CLASS_4_G;
        }
    }
}
