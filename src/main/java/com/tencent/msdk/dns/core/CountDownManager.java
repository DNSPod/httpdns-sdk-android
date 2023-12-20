package com.tencent.msdk.dns.core;

import com.tencent.msdk.dns.base.executor.DnsExecutors;
import com.tencent.msdk.dns.base.log.DnsLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public final class CountDownManager {

    static Transaction beginTransaction() {
        return new Transaction();
    }

    private static CountDownLatch startCountDown(Transaction transaction) {
        CountDownLatch countDownLatch = transaction.mCountDownLatch;
        if (null == countDownLatch) {
            int count = 0;
            for (IgnorableOrNotTask task : transaction.mPendingTasks) {
                if (!task.mIgnorable) {
                    count++;
                }
            }
            countDownLatch = new CountDownLatch(count);
        }
        for (IgnorableOrNotTask task : transaction.mPendingTasks) {
            DnsExecutors.WORK.execute(new CountDownTask(task, countDownLatch));
        }
        transaction.mPendingTasks.clear();
        return countDownLatch;
    }

    public static class Transaction {

        private List<IgnorableOrNotTask> mPendingTasks = Collections.emptyList();
        private CountDownLatch mCountDownLatch = null;

        @SuppressWarnings("UnusedReturnValue")
        public Transaction addTask(Runnable task) {
            return addTask(task, false);
        }

        @SuppressWarnings("UnusedReturnValue")
        public synchronized Transaction addTask(Runnable task, boolean ignorable) {
            if (null == task) {
                throw new IllegalArgumentException("task".concat(Const.NULL_POINTER_TIPS));
            }

            if (Collections.<IgnorableOrNotTask>emptyList() == mPendingTasks) {
                mPendingTasks = new ArrayList<>();
            }
            mPendingTasks.add(new IgnorableOrNotTask(task, ignorable));
            return this;
        }

        public CountDownLatch commit() {
            mCountDownLatch = startCountDown(this);
            return mCountDownLatch;
        }
    }

    private static class IgnorableOrNotTask implements Runnable {

        private final Runnable mRealTask;
        private final boolean mIgnorable;

        public IgnorableOrNotTask(Runnable realTask, boolean ignorable) {
            mRealTask = realTask;
            mIgnorable = ignorable;
        }

        @Override
        public void run() {
            mRealTask.run();
        }
    }

    private static class CountDownTask implements Runnable {

        private final IgnorableOrNotTask mRealTask;
        private final CountDownLatch mCountDownLatch;

        CountDownTask(IgnorableOrNotTask realTask, CountDownLatch countDownLatch) {
            mRealTask = realTask;
            mCountDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            try {
                mRealTask.run();
            } catch (Exception ignored) {
                DnsLog.e("exception: %s", ignored);
            }
//            if (!mRealTask.mIgnorable) {
//                mCountDownLatch.countDown();
//            }
        }
    }
}
