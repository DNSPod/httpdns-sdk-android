package com.tencent.msdk.dns.base.lifecycle;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.base.platform.Platform;

final class ApplicationProvider {

    private static Application sApplication = null;

    public static Application get(Context context) {
        if (null == sApplication) {
            sApplication = getApplication(context);
            if (null == sApplication) {
                sApplication = getApplication(Platform.get().getActivity());
            }
        }
        return sApplication;
    }

    private static Application getApplication(Context context) {
        try {
            if (null == context) {
                return null;
            }
            if (context instanceof Application) {
                return (Application) context;
            }
            if (context instanceof Activity) {
                return ((Activity) context).getApplication();
            }
            Context applicationContext = context.getApplicationContext();
            if (applicationContext instanceof Application) {
                return (Application) applicationContext;
            }
            return null;
        } catch (Exception e) {
            DnsLog.d(e, "Get Application failed");
            return null;
        }
    }
}
