package com.tencent.msdk.dns.core;

import androidx.annotation.NonNull;
import android.text.TextUtils;

public final class DnsDescription {

    public interface Family {
        int INET = 1;
        int INET6 = 2;
        int UN_SPECIFIC = 3;
    }

    public final String channel;
    public final int family;

    public DnsDescription(String channel, int family) {
        if (TextUtils.isEmpty(channel)) {
            throw new IllegalArgumentException("channel".concat(Const.EMPTY_TIPS));
        }
        if (isFamilyInvalid(family)) {
            throw new IllegalArgumentException("family".concat(Const.INVALID_TIPS));
        }

        this.channel = channel;
        this.family = family;
    }

    public static boolean isFamilyInvalid(int family) {
        return Family.INET != family && Family.INET6 != family && Family.UN_SPECIFIC != family;
    }

    @NonNull
    @Override
    public String toString() {
        return this.channel + "Dns(" + this.family + ")";
    }
}
