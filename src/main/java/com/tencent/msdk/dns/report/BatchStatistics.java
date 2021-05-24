package com.tencent.msdk.dns.report;

import com.tencent.msdk.dns.base.utils.CommonUtils;
import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.rest.share.AbsRestDns;
import com.tencent.msdk.dns.core.stat.StatisticsMerge;

final class BatchStatistics {

    public final String netTypeList;
    public final String ssidList;

    public final String hostnameList;
    public final String channelList;
    public final String netStackList;

    public final String restInetNetChangeLookupList;
    public final String restInetStartLookupTimeMillsList;
    public final String restInetLookupErrorCodeList;
    public final String restInetLookupErrorMsgList;
    public final String restInetLookupIpsList;
    public final String restInetLookupTtlList;
    public final String restInetLookupClientIpList;
    public final String restInetLookupCostTimeMillsList;
    public final String restInetLookupRetryTimesList;

    public final String restInet6LookupErrorCodeList;
    public final String restInet6LookupIpsList;
    public final String restInet6LookupErrorMsgList;
    public final String restInet6LookupTtlList;
    public final String restInet6LookupClientIpList;
    public final String restInet6LookupCostTimeMillsList;
    public final String restInet6LookupRetryTimesList;

    private BatchStatistics(
            String netTypeList, String ssidList,
            String hostnameList, String channelList, String netStackList,
            String restInetNetChangeLookupList,
            String restInetStartLookupTimeMillsList,
            String restInetLookupErrorCodeList,
            String restInetLookupErrorMsgList,
            String restInetLookupIpsList,
            String restInetLookupTtlList,
            String restInetLookupClientIpList,
            String restInetLookupCostTimeMillsList,
            String restInetLookupRetryTimesList,
            String restInet6LookupErrorCodeList,
            String restInet6LookupErrorMsgList,
            String restInet6LookupIpsList,
            String restInet6LookupTtlList,
            String restInet6LookupClientIpList,
            String restInet6LookupCostTimeMillsList,
            String restInet6LookupRetryTimesList) {
        this.netTypeList = netTypeList;
        this.ssidList = ssidList;
        this.hostnameList = hostnameList;
        this.channelList = channelList;
        this.netStackList = netStackList;
        this.restInetNetChangeLookupList = restInetNetChangeLookupList;
        this.restInetStartLookupTimeMillsList = restInetStartLookupTimeMillsList;
        this.restInetLookupErrorCodeList = restInetLookupErrorCodeList;
        this.restInetLookupErrorMsgList = restInetLookupErrorMsgList;
        this.restInetLookupIpsList = restInetLookupIpsList;
        this.restInetLookupTtlList = restInetLookupTtlList;
        this.restInetLookupClientIpList = restInetLookupClientIpList;
        this.restInetLookupCostTimeMillsList = restInetLookupCostTimeMillsList;
        this.restInetLookupRetryTimesList = restInetLookupRetryTimesList;
        this.restInet6LookupErrorCodeList = restInet6LookupErrorCodeList;
        this.restInet6LookupErrorMsgList = restInet6LookupErrorMsgList;
        this.restInet6LookupIpsList = restInet6LookupIpsList;
        this.restInet6LookupTtlList = restInet6LookupTtlList;
        this.restInet6LookupClientIpList = restInet6LookupClientIpList;
        this.restInet6LookupCostTimeMillsList = restInet6LookupCostTimeMillsList;
        this.restInet6LookupRetryTimesList = restInet6LookupRetryTimesList;
    }

    public static class Builder {

        private static final char VALUE_SPLITTER = '_';

        // NOTE: 异步解析下, 每次请求(统计)只会进行一路解析, 统一记录到mRestInet为前缀的成员变量中
        private final boolean mAsyncLookup;

        private final StringBuilder mNetTypeListBuilder = new StringBuilder();
        private final StringBuilder mSsidListBuilder = new StringBuilder();

        private final StringBuilder mHostnameListBuilder = new StringBuilder();
        private final StringBuilder mChannelListBuilder = new StringBuilder();
        private final StringBuilder mNetStackListBuilder = new StringBuilder();

        private final StringBuilder mRestInetNetChangeLookupListBuilder = new StringBuilder();
        private final StringBuilder mRestInetStartLookupTimeMillsListBuilder = new StringBuilder();
        private final StringBuilder mRestInetLookupErrorCodeListBuilder = new StringBuilder();
        private final StringBuilder mRestInetLookupErrorMsgListBuilder = new StringBuilder();
        private final StringBuilder mRestInetLookupIpsListBuilder = new StringBuilder();
        private final StringBuilder mRestInetLookupTtlListBuilder = new StringBuilder();
        private final StringBuilder mRestInetLookupClientIpListBuilder = new StringBuilder();
        private final StringBuilder mRestInetLookupCostTimeMillsListBuilder = new StringBuilder();
        private final StringBuilder mRestInetLookupRetryTimesListBuilder = new StringBuilder();

        private final StringBuilder mRestInet6LookupErrorCodeListBuilder = new StringBuilder();
        private final StringBuilder mRestInet6LookupErrorMsgListBuilder = new StringBuilder();
        private final StringBuilder mRestInet6LookupIpsListBuilder = new StringBuilder();
        private final StringBuilder mRestInet6LookupTtlListBuilder = new StringBuilder();
        private final StringBuilder mRestInet6LookupClientIpListBuilder = new StringBuilder();
        private final StringBuilder mRestInet6LookupCostTimeMillsListBuilder = new StringBuilder();
        private final StringBuilder mRestInet6LookupRetryTimesListBuilder = new StringBuilder();

        public Builder(boolean asyncLookup) {
            mAsyncLookup = asyncLookup;
        }

        public Builder append(StatisticsMerge statMerge) {
            if (null == statMerge) {
                throw new IllegalArgumentException("statMerge".concat(Const.NULL_POINTER_TIPS));
            }

            mNetTypeListBuilder.append(statMerge.netType).append(VALUE_SPLITTER);
            mSsidListBuilder.append(statMerge.ssid).append(VALUE_SPLITTER);
            mHostnameListBuilder.append(statMerge.hostname).append(VALUE_SPLITTER);
            mNetStackListBuilder.append(statMerge.curNetStack).append(VALUE_SPLITTER);
            if (mAsyncLookup) {
                mChannelListBuilder.append(statMerge.channel).append(VALUE_SPLITTER);

                AbsRestDns.Statistics restDnsStat =
                        AbsRestDns.Statistics.NOT_LOOKUP != statMerge.restInetDnsStat ?
                                statMerge.restInetDnsStat : statMerge.restInet6DnsStat;
                mRestInetNetChangeLookupListBuilder
                        .append(restDnsStat.netChangeLookup).append(VALUE_SPLITTER);
                mRestInetStartLookupTimeMillsListBuilder
                        .append(restDnsStat.startLookupTimeMills).append(VALUE_SPLITTER);
                mRestInetLookupErrorCodeListBuilder
                        .append(restDnsStat.errorCode).append(VALUE_SPLITTER);
                mRestInetLookupErrorMsgListBuilder
                        .append(restDnsStat.errorMsg).append(VALUE_SPLITTER);
                mRestInetLookupIpsListBuilder
                        .append(CommonUtils.toStringList(restDnsStat.ips, ReportConst.IP_SPLITTER))
                        .append(VALUE_SPLITTER);
                mRestInetLookupTtlListBuilder.append(restDnsStat.ttl).append(VALUE_SPLITTER);
                mRestInetLookupClientIpListBuilder
                        .append(restDnsStat.clientIp).append(VALUE_SPLITTER);
                mRestInetLookupCostTimeMillsListBuilder
                        .append(restDnsStat.costTimeMills).append(VALUE_SPLITTER);
                mRestInetLookupRetryTimesListBuilder
                        .append(restDnsStat.retryTimes).append(VALUE_SPLITTER);
            } else {
                mRestInetLookupErrorCodeListBuilder
                        .append(statMerge.restInetDnsStat.errorCode).append(VALUE_SPLITTER);
                mRestInetLookupErrorMsgListBuilder
                        .append(statMerge.restInetDnsStat.errorMsg).append(VALUE_SPLITTER);
                mRestInetLookupIpsListBuilder
                        .append(CommonUtils.toStringList(
                                statMerge.restInetDnsStat.ips, ReportConst.IP_SPLITTER))
                        .append(VALUE_SPLITTER);
                mRestInetLookupTtlListBuilder
                        .append(statMerge.restInetDnsStat.ttl).append(VALUE_SPLITTER);
                mRestInetLookupClientIpListBuilder
                        .append(statMerge.restInetDnsStat.clientIp).append(VALUE_SPLITTER);
                mRestInetLookupCostTimeMillsListBuilder
                        .append(statMerge.restInetDnsStat.costTimeMills).append(VALUE_SPLITTER);
                mRestInetLookupRetryTimesListBuilder
                        .append(statMerge.restInetDnsStat.retryTimes).append(VALUE_SPLITTER);

                mRestInet6LookupErrorCodeListBuilder
                        .append(statMerge.restInet6DnsStat.errorCode).append(VALUE_SPLITTER);
                mRestInet6LookupErrorMsgListBuilder
                        .append(statMerge.restInet6DnsStat.errorMsg).append(VALUE_SPLITTER);
                mRestInet6LookupIpsListBuilder
                        .append(CommonUtils.toStringList(
                                statMerge.restInet6DnsStat.ips, ReportConst.IP_SPLITTER))
                        .append(VALUE_SPLITTER);
                mRestInet6LookupTtlListBuilder
                        .append(statMerge.restInet6DnsStat.ttl).append(VALUE_SPLITTER);
                mRestInet6LookupClientIpListBuilder
                        .append(statMerge.restInet6DnsStat.clientIp).append(VALUE_SPLITTER);
                mRestInet6LookupCostTimeMillsListBuilder
                        .append(statMerge.restInet6DnsStat.costTimeMills).append(VALUE_SPLITTER);
                mRestInet6LookupRetryTimesListBuilder
                        .append(statMerge.restInet6DnsStat.retryTimes).append(VALUE_SPLITTER);
            }
            return this;
        }

        public BatchStatistics build() {
            if (0 != mNetTypeListBuilder.length()) {
                mNetTypeListBuilder.setLength(mNetTypeListBuilder.length() - 1);
                mSsidListBuilder.setLength(mSsidListBuilder.length() - 1);
                mHostnameListBuilder.setLength(mHostnameListBuilder.length() - 1);
                mNetStackListBuilder.setLength(mNetStackListBuilder.length() - 1);

                mRestInetLookupErrorCodeListBuilder
                        .setLength(mRestInetLookupErrorCodeListBuilder.length() - 1);
                mRestInetLookupErrorMsgListBuilder
                        .setLength(mRestInetLookupErrorMsgListBuilder.length() - 1);
                mRestInetLookupIpsListBuilder.setLength(mRestInetLookupIpsListBuilder.length() - 1);
                mRestInetLookupTtlListBuilder.setLength(mRestInetLookupTtlListBuilder.length() - 1);
                mRestInetLookupClientIpListBuilder
                        .setLength(mRestInetLookupClientIpListBuilder.length() - 1);
                mRestInetLookupCostTimeMillsListBuilder
                        .setLength(mRestInetLookupCostTimeMillsListBuilder.length() - 1);
                mRestInetLookupRetryTimesListBuilder
                        .setLength(mRestInetLookupRetryTimesListBuilder.length() - 1);
                if (mAsyncLookup) {
                    mChannelListBuilder.setLength(mChannelListBuilder.length() - 1);
                    mRestInetNetChangeLookupListBuilder
                            .setLength(mRestInetNetChangeLookupListBuilder.length() - 1);
                    mRestInetStartLookupTimeMillsListBuilder
                            .setLength(mRestInetStartLookupTimeMillsListBuilder.length() - 1);
                } else {
                    mRestInet6LookupErrorCodeListBuilder
                            .setLength(mRestInet6LookupErrorCodeListBuilder.length() - 1);
                    mRestInet6LookupErrorMsgListBuilder
                            .setLength(mRestInet6LookupErrorMsgListBuilder.length() - 1);
                    mRestInet6LookupIpsListBuilder
                            .setLength(mRestInet6LookupIpsListBuilder.length() - 1);
                    mRestInet6LookupTtlListBuilder
                            .setLength(mRestInet6LookupTtlListBuilder.length() - 1);
                    mRestInet6LookupClientIpListBuilder
                            .setLength(mRestInet6LookupClientIpListBuilder.length() - 1);
                    mRestInet6LookupCostTimeMillsListBuilder
                            .setLength(mRestInet6LookupCostTimeMillsListBuilder.length() - 1);
                    mRestInet6LookupRetryTimesListBuilder
                            .setLength(mRestInet6LookupRetryTimesListBuilder.length() - 1);
                }
            }

            return new BatchStatistics(
                    mNetTypeListBuilder.toString(), mSsidListBuilder.toString(),
                    mHostnameListBuilder.toString(), mChannelListBuilder.toString(),
                    mNetStackListBuilder.toString(),
                    mRestInetNetChangeLookupListBuilder.toString(),
                    mRestInetStartLookupTimeMillsListBuilder.toString(),
                    mRestInetLookupErrorCodeListBuilder.toString(),
                    mRestInetLookupErrorMsgListBuilder.toString(),
                    mRestInetLookupIpsListBuilder.toString(),
                    mRestInetLookupTtlListBuilder.toString(),
                    mRestInetLookupClientIpListBuilder.toString(),
                    mRestInetLookupCostTimeMillsListBuilder.toString(),
                    mRestInetLookupRetryTimesListBuilder.toString(),
                    mRestInet6LookupErrorCodeListBuilder.toString(),
                    mRestInet6LookupErrorMsgListBuilder.toString(),
                    mRestInet6LookupIpsListBuilder.toString(),
                    mRestInet6LookupTtlListBuilder.toString(),
                    mRestInet6LookupClientIpListBuilder.toString(),
                    mRestInet6LookupCostTimeMillsListBuilder.toString(),
                    mRestInet6LookupRetryTimesListBuilder.toString());
        }
    }
}
