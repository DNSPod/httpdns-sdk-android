package com.tencent.msdk.dns.report;

import com.tencent.msdk.dns.BackupResolver;

public class SpendReportResolver {
    private static SpendReportResolver mSpendReportResolver = null; //   静态对象

    private SpendReportResolver() {
    }

    // 最近一次上报的时间
    private long mLastReportTime = 0;
    // 当前spend上报的次数
    private int mReportCount = 0;
    // 允许的最大上报次数
    private int maxReportCount = 3;
    // 解析耗时上报的间隔时间
    private long mInterval = 5 * 60 * 1000;

    public static SpendReportResolver getInstance() {//静态get方法
        if (mSpendReportResolver == null) {
            synchronized (BackupResolver.class) {
                if (mSpendReportResolver == null) {
                    mSpendReportResolver = new SpendReportResolver();
                }
            }
        }
        return mSpendReportResolver;
    }

    public void init() {
        mReportCount = 0;
    }

    public void setLastReportTime(long time) {
        mLastReportTime = time;
        mReportCount++;
    }

    /**
     * 校验是否满足上报条件-上报次数<=上报最大次数，上报时间间隔>=mInterval;
     *
     * @return
     */
    public boolean getCanReport() {
        return (System.currentTimeMillis() - mLastReportTime) >= mInterval;
//        return mReportCount < maxReportCount && (System.currentTimeMillis() - mLastReportTime) >= mInterval;
    }

}
