package com.tencent.msdk.dns.base.log;

import android.os.Build;
import android.util.Log;

public final class AndroidLogNode implements ILogNode {

    private static final int MAX_TAG_LENGTH = 23;
    private static final int MAX_LOG_LENGTH = 4000;

    @Override
    public void println(int priority,
            /* @Nullable */String tag, /* @Nullable */String msg, /* @Nullable */Throwable tr) {
        switch (priority) {
            case Log.VERBOSE:
            case Log.DEBUG:
            case Log.INFO:
            case Log.WARN:
            case Log.ERROR:
                log(priority, tag, msg, tr);
                break;
            default:
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void log(int priority, String tag, String msg, Throwable tr) {
        tag = getTag(tag);
        if (null == msg) {
            msg = "";
        }
        if (null != tr) {
            msg += "\n" + Log.getStackTraceString(tr);
        }

        if (MAX_LOG_LENGTH >= msg.length()) {
            Log.println(priority, tag, msg);
            return;
        }
        // Split by line, then ensure each line can fit into Log's maximum length.
        for (int i = 0, len = msg.length(); i < len; i++) {
            int newline = msg.indexOf('\n', i);
            newline = -1 != newline ? newline : len;
            do {
                int end = Math.min(newline, i + MAX_LOG_LENGTH);
                String part = msg.substring(i, end);
                Log.println(priority, tag, part);
                i = end;
            } while (i < newline);
        }
    }

    private String getTag(String tag) {
        if (null == tag) {
            tag = "";
        }
        if (MAX_TAG_LENGTH >= tag.length() || Build.VERSION_CODES.N <= Build.VERSION.SDK_INT) {
            return tag;
        }
        return tag.substring(0, MAX_TAG_LENGTH);
    }
}
