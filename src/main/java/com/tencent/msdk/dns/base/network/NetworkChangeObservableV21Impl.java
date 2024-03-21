package com.tencent.msdk.dns.base.network;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import com.tencent.msdk.dns.base.log.DnsLog;

final class NetworkChangeObservableV21Impl extends AbsNetworkChangeObservable {

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    NetworkChangeObservableV21Impl(Context context) {
        super(context);

        if (null == context) {
            return;
        }
        final Context appContext = context.getApplicationContext();
        final ConnectivityManager connectivityManager =
                (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (null == connectivityManager) {
            return;
        }
        try {
            NetworkRequest allNetworkRequest =
                    new NetworkRequest.Builder()
                            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                            .addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
                            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                            .build();
            connectivityManager.registerNetworkCallback(allNetworkRequest,
                    new ConnectivityManager.NetworkCallback() {

                        // NOTE：存在一次切网多次调用的情况
                        // NOTE: 当前不好确认只在回调onAvailable和onLost时驱动切网逻辑是否能覆盖所有切网情况, 日志记录所有回调时机方便后续定位问题

                        @Override
                        public void onAvailable(Network network) {
                            super.onAvailable(network);
                            DnsLog.d("Network onAvailable(%s)", network);

                            mayChangeNetwork(appContext);
                        }

                        @Override
                        public void onLosing(Network network, int maxMsToLive) {
                            super.onLosing(network, maxMsToLive);
                            DnsLog.d("Network onLosing(%s)", network);
                        }

                        @Override
                        public void onLost(Network network) {
                            super.onLost(network);
                            DnsLog.d("Network onLost(%s)", network);

                            mayChangeNetwork(appContext);
                        }

                        @Override
                        public void onUnavailable() {
                            super.onUnavailable();
                            DnsLog.d("Network onUnavailable");
                        }

                        @Override
                        public void onCapabilitiesChanged(Network network,
                                                          NetworkCapabilities networkCapabilities) {
                            super.onCapabilitiesChanged(network, networkCapabilities);
                            DnsLog.d("Network onCapabilitiesChanged(%s)", network);
                        }

                        @Override
                        public void onLinkPropertiesChanged(Network network,
                                                            LinkProperties linkProperties) {
                            super.onLinkPropertiesChanged(network, linkProperties);
                            DnsLog.d("Network onLinkPropertiesChanged(%s)", network);
                        }
                    });
        } catch (Exception e) {
            DnsLog.w("network register failed " + e);
        }
    }
}
