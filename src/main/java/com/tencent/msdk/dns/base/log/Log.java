package com.tencent.msdk.dns.base.log;

import java.util.ArrayList;
import java.util.List;

final class Log {

    private static final List<ILogNode> LOG_NODE_LIST = new ArrayList<>();

    static {
        LOG_NODE_LIST.add(new AndroidLogNode());
    }

    static synchronized void addLogNode(/* @Nullable */ILogNode logNode) {
        if (null != logNode) {
            LOG_NODE_LIST.add(logNode);
        }
    }

    static void println(int priority,
            /* @Nullable */String tag, /* @Nullable */String msg, /* @Nullable */Throwable tr) {
        for (ILogNode logNode : LOG_NODE_LIST) {
            logNode.println(priority, tag, msg, tr);
        }
    }
}
