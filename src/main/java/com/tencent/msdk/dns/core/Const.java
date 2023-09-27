package com.tencent.msdk.dns.core;

public interface Const {

    String LOCAL_CHANNEL = "Local";
    String DES_HTTP_CHANNEL = "DesHttp";
    String AES_HTTP_CHANNEL = "AesHttp";
    String HTTPS_CHANNEL = "Https";

    String INVALID_HOSTNAME = "";

    String INVALID_CHANNEL = "";

    int INVALID_TIMEOUT_MILLS = -1;

    int INVALID_NETWORK_STACK = -1;

    String INVALID_IP = "0";

    String[] EMPTY_IPS = new String[0];

    int DEFAULT_TIME_INTERVAL = 0;

    int MAX_DEFAULT_TTL = 6000;

    // 字节码优化, 详见https://jakewharton.com/the-economics-of-generated-code/

    String NULL_POINTER_TIPS = " can not be null";
    String EMPTY_TIPS = " can not be empty";
    String NOT_INIT_TIPS = " is not initialized yet";
    String INVALID_TIPS = " is invalid";
    String LESS_THAN_0_TIPS = " can not less than 0";
}
