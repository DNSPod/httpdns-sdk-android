package com.tencent.msdk.dns.base.network;

import android.content.Context;

import com.tencent.msdk.dns.base.log.DnsLog;
import com.tencent.msdk.dns.base.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract class AbsNetworkChangeObservable implements INetworkChangeObservable {

    private volatile int mCurNetworkType = -1;

    private final List<IOnNetworkChangeListener> mOnNetworkChangeListeners = Collections
            .synchronizedList(new ArrayList<IOnNetworkChangeListener>());

    @SuppressWarnings("unused")
    AbsNetworkChangeObservable(Context context) {
        // 约束子类构造方法传入context
    }

    @Override
    public synchronized void addNetworkChangeListener(IOnNetworkChangeListener onNetworkChangeListener) {
        if (null != onNetworkChangeListener) {
            mOnNetworkChangeListeners.add(onNetworkChangeListener);
        }
    }

    void mayChangeNetwork(Context context) {
        try {
            if (isNetworkChanged(context)) {
                changeNetwork();
            }
        } catch (Exception e) {
            DnsLog.d(e, "mayChangeNetwork exception occur");
        }
    }

    private void changeNetwork() {
        DnsLog.d("changeNetwork call");
        synchronized (mOnNetworkChangeListeners) {
            for (IOnNetworkChangeListener mOnNetworkChangeListener : mOnNetworkChangeListeners) {
                mOnNetworkChangeListener.onNetworkChange();
            }
        }
    }

    private boolean isNetworkChanged(Context context) {
        // 异常情况返回true
        if (null == context) {
            return true;
        }
        int curNetType = NetworkUtils.getNetworkState(context);
        if (mCurNetworkType == -1) {
            mCurNetworkType = curNetType;
        } else if (mCurNetworkType != curNetType) {
            mCurNetworkType = curNetType;
            // NetType不相同才notify
            return true;
        }
        return false;
    }
}
