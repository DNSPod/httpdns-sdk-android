package com.tencent.msdk.dns.base.bugly;

import android.content.Context;
import android.content.SharedPreferences;

import com.tencent.msdk.dns.BuildConfig;
import com.tencent.msdk.dns.DnsService;
import com.tencent.msdk.dns.base.log.DnsLog;

public class SharedBugly {
    private static String appId = BuildConfig.BUGLY_ID;
    private static String appVersion = BuildConfig.VERSION_NAME;

    public static void init(Context context) {
        try {
            if (DnsService.getDnsConfig().experimentalBuglyEnable) {
                SharedPreferences settings = context.getSharedPreferences("BuglySdkInfos", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(appId, appVersion);//必填信息
                editor.commit();
                DnsLog.d("shared bugly inited success");
            } else {

            }
        } catch (Exception e) {
            DnsLog.d("shared bugly inited error " + e);
        }
    }
}
