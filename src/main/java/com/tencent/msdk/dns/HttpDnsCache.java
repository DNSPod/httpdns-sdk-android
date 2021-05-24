package com.tencent.msdk.dns;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.tencent.msdk.dns.base.log.DnsLog;

public final class HttpDnsCache {

    public static class ConnectivityChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            DnsLog.d("Network change.");
        }
    }
}
