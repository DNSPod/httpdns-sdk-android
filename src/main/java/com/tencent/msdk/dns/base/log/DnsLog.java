package com.tencent.msdk.dns.base.log;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static android.util.Log.VERBOSE;
import static android.util.Log.WARN;
import static android.util.Log.isLoggable;

import java.util.Locale;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class DnsLog {

    private static final String TAG = "HTTPDNS";

    private static int sLogLevel;

    static {
        // 初始情况只考虑系统设置
        int logLevel = ERROR + 1;
        for (int level = ERROR; level >= VERBOSE; level--) {
            if (!isLoggable(TAG, level)) {
                break;
            }
            logLevel = level;
        }
        sLogLevel = logLevel;
    }

    public static void setLogLevel(int logLevel) {
        // 以最宽松的日志层级为准
        sLogLevel = Math.min(logLevel, sLogLevel);
    }

    public static void addLogNode(/* @Nullable */ILogNode logNode) {
        Log.addLogNode(logNode);
    }

    public static boolean canLog(int logLevel) {
        return logLevel >= sLogLevel;
    }

    public static void v(String msg, Object... args) {
        v(null, msg, args);
    }

    public static void v(/* @Nullable */Throwable tr, String msg, Object... args) {
        tryLog(VERBOSE, tr, msg, args);
    }

    public static void d(String msg, Object... args) {
        d(null, msg, args);
    }

    public static void d(/* @Nullable */Throwable tr, String msg, Object... args) {
        tryLog(DEBUG, tr, msg, args);
    }

    public static void i(String msg, Object... args) {
        i(null, msg, args);
    }

    public static void i(/* @Nullable */Throwable tr, String msg, Object... args) {
        tryLog(INFO, tr, msg, args);
    }

    public static void w(String msg, Object... args) {
        w(null, msg, args);
    }

    public static void w(/* @Nullable */Throwable tr, String msg, Object... args) {
        tryLog(WARN, tr, msg, args);
    }

    public static void e(String msg, Object... args) {
        e(null, msg, args);
    }

    public static void e(/* @Nullable */Throwable tr, String msg, Object... args) {
        tryLog(ERROR, tr, msg, args);
    }

    private static void tryLog(int priority, Throwable tr, String msg, Object... args) {
        try {
            if (priority >= sLogLevel) {
                Log.println(priority, TAG, String.format(Locale.US, msg, args),
                        tr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
