package com.tencent.msdk.dns.base.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import com.tencent.msdk.dns.base.log.DnsLog;

final class NetworkChangeObservableImpl extends AbsNetworkChangeObservable {

    NetworkChangeObservableImpl(Context context) {
        super(context);

        if (null == context) {
            return;
        }
        try {
            final Context appContext = context.getApplicationContext();
            appContext.registerReceiver(new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    mayChangeNetwork(appContext);
                }
            }, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        } catch (Exception e) {
            DnsLog.w("network register failed " + e);
        }
    }
}
