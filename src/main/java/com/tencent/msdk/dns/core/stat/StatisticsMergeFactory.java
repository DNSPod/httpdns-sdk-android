package com.tencent.msdk.dns.core.stat;

import android.content.Context;

import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.IDns;
import com.tencent.msdk.dns.core.IStatisticsMerge;
import com.tencent.msdk.dns.core.rest.share.LookupExtra;

public final class StatisticsMergeFactory implements IStatisticsMerge.IFactory {

    @Override
    public <LookupExtraImplT extends IDns.ILookupExtra>
    IStatisticsMerge<LookupExtraImplT> create(Class<LookupExtraImplT> klass, Context context) {
        if (null == klass) {
            throw new IllegalArgumentException("klass".concat(Const.NULL_POINTER_TIPS));
        }
        if (null == context) {
            throw new IllegalArgumentException("context".concat(Const.NULL_POINTER_TIPS));
        }

        if (LookupExtra.class.equals(klass)) {
            //noinspection unchecked
            return (IStatisticsMerge<LookupExtraImplT>) new StatisticsMerge(context);
        }
        return IStatisticsMerge.IFactory.DEFAULT.create(klass, context);
    }
}
