package com.tencent.msdk.dns.base.log;

/**
 * 接受日志输出的接口
 */
@SuppressWarnings("WeakerAccess")
public interface ILogNode {

    /**
     * 日志输出时回调
     *
     * @param priority 日志等级, 使用<a href="https://developer.android.google.cn/reference/android/util/Log">Log</a>类定义的常量, 可选值为
     *                 <a href="https://developer.android.google.cn/reference/android/util/Log.html#VERBOSE">VERBOSE</a>,
     *                 <a href="https://developer.android.google.cn/reference/android/util/Log.html#DEBUG">DEBUG</a>,
     *                 <a href="https://developer.android.google.cn/reference/android/util/Log.html#INFO">INFO</a>,
     *                 <a href="https://developer.android.google.cn/reference/android/util/Log.html#WARN">WARN</a>,
     *                 <a href="https://developer.android.google.cn/reference/android/util/Log.html#ERROR">ERROR</a>
     * @param tag      日志tag
     * @param msg      日志信息
     * @param tr       日志记录的异常, 可能为null
     */
    void println(int priority,
            /* @Nullable */String tag, /* @Nullable */String msg, /* @Nullable */Throwable tr);
}
