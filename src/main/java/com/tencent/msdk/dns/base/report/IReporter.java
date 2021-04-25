package com.tencent.msdk.dns.base.report;

import java.util.Map;

public interface IReporter<InitParameters extends IReporter.IInitParameters> {

    interface IInitParameters {

        IInitParameters DEFAULT = new IInitParameters() {};
    }

    String getName();

    boolean canReport();

    boolean init(InitParameters initParameter);

    boolean setDebug(boolean enabled);

    boolean report(
            int env, /* @Nullable */String eventName, /* @Nullable */Map<String, String> eventInfo);
}
