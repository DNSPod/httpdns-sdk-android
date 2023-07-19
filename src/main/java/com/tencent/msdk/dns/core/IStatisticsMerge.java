package com.tencent.msdk.dns.core;

import android.content.Context;

public interface IStatisticsMerge<LookupExtra extends IDns.ILookupExtra> extends IDns.IStatistics {

    interface IFactory {

        IFactory DEFAULT = new IFactory() {

            @Override
            public <LookupExtra extends IDns.ILookupExtra>
            IStatisticsMerge<LookupExtra> create(Class<LookupExtra> klass, Context context) {
                return new IStatisticsMerge<LookupExtra>() {

                    @Override
                    public void merge(IDns dns, IDns.IStatistics stat) {
                    }

                    @Override
                    public void statContext(LookupContext lookupContext) {
                    }

                    @Override
                    public void statResult(IpSet ipSet) {
                    }

                    @Override
                    public boolean lookupSuccess() {
                        return false;
                    }

                    @Override
                    public boolean lookupNeedRetry() {
                        return false;
                    }

                    @Override
                    public boolean lookupFailed() {
                        return false;
                    }

                   @Override
                   public boolean lookupPartCached() {
                       return false;
                   }

                    @Override
                    public String toJsonResult() {
                        return "{\"v4_ips\":\"\",\"v4_ttl\":\"\",\"v4_client_ip\":\"\",\"v6_ips\":\"\",\"v6_ttl\":\"\",\"v6_client_ip\":\"\"}";
                    }
                };
            }
        };

        <LookupExtra extends IDns.ILookupExtra>
        IStatisticsMerge<LookupExtra> create(Class<LookupExtra> klass, Context context);
    }

    <Statistics extends IDns.IStatistics> void merge(IDns dns, Statistics stat);

    void statContext(LookupContext<LookupExtra> lookupContext);

    void statResult(IpSet ipSet);

    String toJsonResult();
}
