package com.tencent.msdk.dns.base.network;

import android.content.Context;

interface INetworkChangeObservable {

    interface IFactory {

        INetworkChangeObservable create(Context context);
    }

    void addNetworkChangeListener(IOnNetworkChangeListener onNetworkChangeListener);
}
