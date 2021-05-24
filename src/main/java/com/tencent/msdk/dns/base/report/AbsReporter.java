package com.tencent.msdk.dns.base.report;

import android.text.TextUtils;

import java.util.Map;

public abstract class AbsReporter<InitParameters extends IReporter.IInitParameters>
        implements IReporter<InitParameters> {

    @Override
    public boolean init(InitParameters initParameter) {
        return true;
    }

    @Override
    public boolean setDebug(boolean enabled) {
        return true;
    }

    @Override
    public boolean report(int env, String eventName, Map<String, String> eventInfo) {
        if (TextUtils.isEmpty(eventName) || null == eventInfo) {
            return false;
        }
        return reportInternal(env, eventName, eventInfo);
    }

    abstract boolean reportInternal(int env, String eventName, Map<String, String> eventInfo);
}
