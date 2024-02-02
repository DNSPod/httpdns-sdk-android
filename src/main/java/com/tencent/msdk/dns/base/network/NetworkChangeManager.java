package com.tencent.msdk.dns.base.network;

import android.content.Context;

import com.tencent.msdk.dns.base.log.DnsLog;

public final class NetworkChangeManager {

    private volatile static INetworkChangeObservable sNetworkChangeObservable;

    public static void install(Context context) {
        if (null == context) {
            DnsLog.w("Install network change manager failed: context can not be null");
            return;
        }
        if (null == sNetworkChangeObservable) {
            synchronized (NetworkChangeManager.class) {
                if (null == sNetworkChangeObservable) {
                    sNetworkChangeObservable = new NetworkChangeObservableFactory().create(context);

                }
            }
        }
    }

    public static void addNetworkChangeListener(IOnNetworkChangeListener onNetworkChangeListener) {
        synchronized (NetworkChangeManager.class) {
            if (null != sNetworkChangeObservable) {
                sNetworkChangeObservable.addNetworkChangeListener(onNetworkChangeListener);
            }
        }
    }
}
