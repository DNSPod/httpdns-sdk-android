package com.tencent.msdk.dns.base.report;

import android.text.TextUtils;
import android.util.Log;

import com.tencent.msdk.dns.BuildConfig;
import com.tencent.msdk.dns.base.log.DnsLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ReportManager {

    public interface Channel {
        int BEACON = 1;
        int GCC = 2;
    }

    public interface Environment {
        int TEST = 1;
        int RELEASE = 2;
    }

    private ReportManager() {
    }

    private static List<IReporter> sBuiltInReporters = Collections.emptyList();
    private static List<IReporter> sCustomReporters = Collections.emptyList();

    public static void init(int channel) {
        sBuiltInReporters = ReporterFactory.getReporters(channel);
    }

    public static synchronized void addReporter(/* @Nullable */IReporter reporter) {
        if (null == reporter || !reporter.canReport()) {
            return;
        }
        if (sCustomReporters.isEmpty()) {
            // NOTE: sCustomReporters为空时为Collections.emptyList(), emptyList不允许add操作
            sCustomReporters = new ArrayList<>();
        }
        sCustomReporters.add(reporter);
    }

    public static boolean canReport() {
        return !sBuiltInReporters.isEmpty() || !sCustomReporters.isEmpty();
    }

    public static <InitParameters extends IReporter.IInitParameters>
    void initBuiltInReporter(int channel, InitParameters initParameters) {
        @SuppressWarnings("unchecked")
        IReporter<InitParameters> reporter = ReporterFactory.getReporter(channel);
        if (null == reporter) {
            DnsLog.d("Get builtIn reporter from channel: %d failed", channel);
            return;
        }
        if (!reporter.init(initParameters)) {
            DnsLog.d("%s init failed", reporter.getName());
        }
    }

    public static void setDebug(int channel, boolean enabled) {
        IReporter reporter = ReporterFactory.getReporter(channel);
        if (null == reporter) {
            DnsLog.d("Get builtIn reporter from channel: %d failed", channel);
            return;
        }
        if (!reporter.setDebug(enabled)) {
            DnsLog.d("%s setDebug(%b) failed", reporter.getName(), enabled);
        }
    }

    public static void setDebug(boolean enabled) {
        for (IReporter reporter : sBuiltInReporters) {
            if (!reporter.setDebug(enabled)) {
                DnsLog.d("%s setDebug(%b) failed", reporter.getName(), enabled);
            }
        }
        for (IReporter reporter : sCustomReporters) {
            if (!reporter.setDebug(enabled)) {
                DnsLog.d("%s setDebug(%b) failed", reporter.getName(), enabled);
            }
        }
    }

    public static void report(int env,
            /* @Nullable */String eventName, /* @Nullable */Map<String, String> eventInfo) {
        if (TextUtils.isEmpty(eventName) || null == eventInfo) {
            return;
        }
        DnsLog.d("HTTPDNS_SDK_VER:" + BuildConfig.VERSION_NAME  + ", Try to report %s", eventName);

        // NOTE: 上报打印太耗时, 仅在命令行将日志打印层级设为允许VERBOSE级别打印时才打印
        if (DnsLog.canLog(Log.VERBOSE)) {
            for (Map.Entry<String, String> infoItem : eventInfo.entrySet()) {
                DnsLog.d("%s: %s", infoItem.getKey(), infoItem.getValue());
            }
        }
        if (!ReporterFactory.canReport) {
            return;
        }
        for (IReporter reporter : sBuiltInReporters) {
            if (!reporter.report(env, eventName, eventInfo)) {
                DnsLog.d("%s report failed", reporter.getName());
            }
        }
        for (IReporter reporter : sCustomReporters) {
            if (!reporter.report(env, eventName, eventInfo)) {
                DnsLog.d("%s report failed", reporter.getName());
            }
        }
    }
}
