package com.tencent.msdk.dns.base.report;

//import com.tencent.gcloud.httpdns.report.GCloudCoreReporter;
import com.tencent.msdk.dns.base.log.DnsLog;

import java.util.Map;

final class GCCReporterImpl extends AbsReporter<IReporter.IInitParameters> {

    private static final String GCC_REPORTER_NAME = GCCReporterImpl.class.getSimpleName();

    @Override
    public String getName() {
        return GCC_REPORTER_NAME;
    }

    @Override
    public boolean canReport() {
        try {
//            GCloudCoreReporter.class.getClass();
            return true;
        } catch (Throwable tr) {
            DnsLog.d("Can not find GCloudCoreReporter class for %s", tr);
            return false;
        }
    }

    @Override
    boolean reportInternal(int env, String eventName, Map<String, String> eventInfo) {
        try {
//            return GCloudCoreReporter.reportEvent(env, eventName, eventInfo);
            return true;
        } catch (Throwable tr) {
            return false;
        }
    }
}
