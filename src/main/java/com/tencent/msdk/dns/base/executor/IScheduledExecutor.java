package com.tencent.msdk.dns.base.executor;

import java.util.concurrent.Executor;

public interface IScheduledExecutor extends Executor {

    void schedule(/* @Nullable */Runnable task, long delayMills);

    void cancel(/* @Nullable */Runnable task);
}
