package com.tencent.msdk.dns.base.report;

import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ReporterFactory {
    private static final SparseArray<IReporter> CHANNEL_REPORTER_MAP =
            new SparseArray<>(2);

    public static boolean canReport = false;

    private ReporterFactory() {
    }

    static List<IReporter> getReporters(int channel) {
        List<IReporter> reporters = null;
        for (int i = 0; i < CHANNEL_REPORTER_MAP.size(); i++) {
            int channelItem = CHANNEL_REPORTER_MAP.keyAt(i);
            if (0 != (channelItem & channel)) {
                if (null == reporters) {
                    reporters = new ArrayList<>();
                }
                reporters.add(CHANNEL_REPORTER_MAP.get(channelItem));
            }
        }
        return null != reporters ? reporters : Collections.<IReporter>emptyList();
    }

    static IReporter getReporter(int channel) {
        for (int i = 0; i < CHANNEL_REPORTER_MAP.size(); i++) {
            int channelItem = CHANNEL_REPORTER_MAP.keyAt(i);
            if (channelItem == channel) {
                return CHANNEL_REPORTER_MAP.get(channelItem);
            }
        }
        return null;
    }
}
