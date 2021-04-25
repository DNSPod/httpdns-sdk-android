package com.tencent.msdk.dns.base.executor;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

import com.tencent.msdk.dns.base.log.DnsLog;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

public final class DnsExecutors {

    /**
     * 提供SDK内部使用线程池的接口
     */
    public interface ExecutorSupplier {

        /**
         * 获取线程池实例
         *
         * @return 线程池实例
         */
        Executor get();
    }

    private static final int INVALID_PRIORITY = Integer.MIN_VALUE;
    private static final String DNS_TASK_NAME_PREFIX = "dns-work-";
    private static final AtomicInteger DNS_TASK_ID_GENERATOR = new AtomicInteger(0);

    /**
     * @hide
     */
    public static final IScheduledExecutor MAIN = new MainExecutor();
    /**
     * @hide
     */
    public static final Executor WORK = new WorkExecutor();

    /**
     * @hide
     */
    public static ExecutorSupplier sExecutorSupplier = null;

    private static Runnable wrapTask(final Runnable task) {
        // 外部类持有的都是静态对象，不存在泄漏风险
        return new Runnable() {

            @Override
            public void run() {
                String origName = setThreadName(
                        DNS_TASK_NAME_PREFIX + DNS_TASK_ID_GENERATOR.getAndIncrement());
                int origPriority = setThreadPriority2Background();
                try {
                    if (null != task) {
                        task.run();
                    }
                } catch (Exception e) {
                    DnsLog.w(e, "Run task in executor failed");
                }
                restoreThreadPriority(origPriority);
                restoreThreadName(origName);
            }
        };
    }

    private static int setThreadPriority2Background() {
        int oriPriority = INVALID_PRIORITY;
        try {
            oriPriority = Process.getThreadPriority(Process.myTid());
            if (Process.THREAD_PRIORITY_BACKGROUND != oriPriority) {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            }
        } catch (Exception ignored) {
        }
        return oriPriority;
    }

    private static void restoreThreadPriority(int origPriority) {
        if (INVALID_PRIORITY == origPriority) {
            return;
        }
        try {
            int curPriority = Process.getThreadPriority(Process.myTid());
            if (origPriority != curPriority) {
                Process.setThreadPriority(origPriority);
            }
        } catch (Exception ignored) {
        }
    }

    private static String setThreadName(String newName) {
        String origName = Thread.currentThread().getName();
        Thread.currentThread().setName(newName);
        return origName;
    }

    private static void restoreThreadName(String origName) {
        Thread.currentThread().setName(origName);
    }

    private static class MainExecutor implements IScheduledExecutor {

        private final HandlerThread mMainThread;
        private final Handler mMainHandler;

        private final Map<Runnable, Runnable> mTaskWrapperMap = new ConcurrentHashMap<>();

        private MainExecutor() {
            mMainThread = new HandlerThread("dns-main");
            mMainThread.start();
            mMainHandler = new Handler(mMainThread.getLooper());
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public void execute(/* @NonNull */Runnable task) {
            if (null != task) {
                mMainHandler.post(wrapTask(task));
            }
        }

        @Override
        public void schedule(Runnable task, long delayMills) {
            if (null != task) {
                Runnable taskWrapper = wrapTask(task);
                if (0 < delayMills) {
                    mTaskWrapperMap.put(task, taskWrapper);
                    mMainHandler.postDelayed(taskWrapper, delayMills);
                } else {
                    execute(taskWrapper);
                }
            }
        }

        @Override
        public void cancel(Runnable task) {
            if (null != task) {
                Runnable taskWrapper = mTaskWrapperMap.get(task);
                if (null != taskWrapper) {
                    mMainHandler.removeCallbacks(taskWrapper);
                }
            }
        }
    }

    private static class WorkExecutor implements Executor {

        private final Executor mRealExecutor;

        private WorkExecutor() {
            Executor realExecutor = null;
            if (null != sExecutorSupplier) {
                realExecutor = sExecutorSupplier.get();
            }
            if (null == realExecutor) {
                realExecutor = AsyncTask.THREAD_POOL_EXECUTOR;
            }
            mRealExecutor = realExecutor;
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public void execute(/* @NonNull */Runnable task) {
            if (null != task) {
                mRealExecutor.execute(wrapTask(task));
            }
        }
    }
}
