package com.tencent.msdk.dns.base.report;

import android.content.Context;

@Deprecated
public final class BeaconReporterInitParameters implements IReporter.IInitParameters {

    public final Context appContext;
    public final String appId;

    public BeaconReporterInitParameters(Context appContext, String appId) {
        this.appContext = appContext;
        this.appId = appId;
    }
}
