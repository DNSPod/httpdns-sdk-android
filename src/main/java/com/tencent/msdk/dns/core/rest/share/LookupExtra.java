package com.tencent.msdk.dns.core.rest.share;

import android.text.TextUtils;

import com.tencent.msdk.dns.core.Const;
import com.tencent.msdk.dns.core.IDns;

public final class LookupExtra implements IDns.ILookupExtra {

    public final String bizId;
    public final String bizKey;
    public final String token;

    public LookupExtra(String bizId, String bizKey, String token) {
        if (TextUtils.isEmpty(bizId)) {
            throw new IllegalArgumentException("bizId".concat(Const.EMPTY_TIPS));
        }
        if (TextUtils.isEmpty(bizKey)) {
            throw new IllegalArgumentException("bizKey".concat(Const.EMPTY_TIPS));
        }

        this.bizId = bizId;
        this.bizKey = bizKey;
        this.token = token;
    }

    @Override
    public String toString() {
        return "LookupExtra{" +
                "bizId='" + bizId + '\'' +
                ", bizKey='" + bizKey + '\'' +
                ", token='" + token + '\'' +
                '}';
    }
}
