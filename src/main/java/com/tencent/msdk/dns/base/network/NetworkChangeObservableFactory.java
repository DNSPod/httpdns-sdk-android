package com.tencent.msdk.dns.base.network;

import android.content.Context;
import android.os.Build;

final class NetworkChangeObservableFactory implements INetworkChangeObservable.IFactory {

    @Override
    public INetworkChangeObservable create(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return new NetworkChangeObservableImpl(context);
        } else {
            return new NetworkChangeObservableV21Impl(context);
        }
    }
}
