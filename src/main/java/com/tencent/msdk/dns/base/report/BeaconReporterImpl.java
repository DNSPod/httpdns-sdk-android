package com.tencent.msdk.dns.base.report;

import android.content.Context;

import com.tencent.msdk.dns.base.log.DnsLog;

import java.lang.reflect.Method;
import java.util.Map;

@Deprecated
final class BeaconReporterImpl extends AbsReporter<BeaconReporterInitParameters> {

    private static final String BEACON_REPORTER_NAME = BeaconReporterImpl.class.getSimpleName();

    private static final String BEACON_CLASS_NAME = "com.tencent.beacon.event.UserAction";


    @Override
    public String getName() {
        return BEACON_REPORTER_NAME;
    }

    @Override
    public boolean canReport() {
        try {
            Class userAction = Class.forName(BEACON_CLASS_NAME);
            DnsLog.d("find userAction class for %s", userAction);
            return true;
        } catch (Throwable tr) {
            DnsLog.d("Can not find userAction class for %s", tr);
            return false;
        }
    }

    @Override
    public boolean init(BeaconReporterInitParameters initParameter) {
        try {
            Class userAction = Class.forName(BEACON_CLASS_NAME);
            Method initUserAction = userAction.getMethod("initUserAction", Context.class);
            Method setAppKey = userAction.getMethod("setAppKey", String.class);

            initUserAction.invoke(null, initParameter.appContext);
            setAppKey.invoke(null, initParameter.appId);
            return true;
        } catch (Throwable tr) {
            DnsLog.d("userAction init failed %s", tr);
            return false;
        }
    }

    @Override
    public boolean setDebug(boolean enabled) {
        try {
            Class userAction = Class.forName(BEACON_CLASS_NAME);
            Method setLogAble = userAction.getMethod("setLogAble", boolean.class, boolean.class);

            setLogAble.invoke(null, enabled, enabled);
            return true;
        } catch (Throwable tr) {
            DnsLog.d("userAction setDebug failed %s", tr);
            return false;
        }
    }

    @Override
    boolean reportInternal(int env, String eventName, Map<String, String> eventInfo) {
        // 这里进行实时上报, 对齐其他上报通道逻辑
        try {
            Class userAction = Class.forName(BEACON_CLASS_NAME);
            Method onUserAction = userAction.getMethod("onUserAction", String.class, boolean.class, long.class,
                    long.class, Map.class, boolean.class);

            boolean isReport = (Boolean) onUserAction.invoke(null, eventName, true, 0, -1, eventInfo, true);

            DnsLog.d("userAction reportInternal success %s", isReport);

            return isReport;

        } catch (Throwable tr) {
            DnsLog.d("userAction reportInternal failed %s", tr);

            return false;
        }
    }
}
