package com.tencent.msdk.dns.report;

interface ReportConst {

    String PRE_LOOKUP_EVENT_NAME = "HDNSPreLookup"; // 预解析
    String ASYNC_LOOKUP_EVENT_NAME = "HDNSLookupAsync"; // 缓存自动刷新（异步）
    String LOOKUP_METHOD_CALLED_EVENT_NAME = "HDNSGetHostByName";   // 普通解析
    String LOOKUP_FROM_CACHED_EVENT_NAME = "HDNSLookupCached";  // 命中缓存
    String EXPIRED_ASYNC_LOOKUP_EVENT_NAME = "HDNSLookupExpiredAsync"; // useExpiredIpEnable为true时的异步更新缓存事件,区别于HDNSLookupAsync

    String IP_SPLITTER = ",";

    String SDK_VERSION_KEY = "sdk_Version";

    String APP_ID_KEY = "appID";
    String BIZ_ID_KEY = "id";

    String USER_ID_KEY = "userID";

    String NETWORK_TYPE_KEY = "netType";

    String CHANNEL_KEY = "channel";
    String HOSTNAME_KEY = "domain";
    String NETWORK_STACK_KEY = "net_stack";

    String LOCAL_LOOKUP_IPS_KEY = "ldns_ip";
    String LOCAL_LOOKUP_COST_TIME_MILLS_KEY = "ldns_time";

    String REST_LOOKUP_ERROR_CODE_KEY = "hdns_err_code";
    String REST_LOOKUP_ERROR_MESSAGE_KEY = "hdns_err_msg";
    String REST_LOOKUP_IPS_KEY = "hdns_ip";
    String REST_LOOKUP_TTL_KEY = "ttl";
    String REST_LOOKUP_CLIENT_IP_KEY = "clientIP";
    String REST_LOOKUP_COST_TIME_MILLS_KEY = "hdns_time";
    String REST_LOOKUP_RETRY_TIMES_KEY = "hdns_retry";
    String REST_LOOKUP_CACHE_HIT_KEY = "isCache";

    String INET_LOOKUP_IPS_KEY = "dns_ips";
    String INET6_LOOKUP_IPS_KEY = "dns_4a_ips";

    String LOOKUP_COUNT_KEY = "lookup_count";

    String BATCH_NETWORK_TYPE_KEY = "net_types";

    String BATCH_HOSTNAME_KEY = "domains";
    String BATCH_NETWORK_STACK_KEY = "net_stacks";

    String BATCH_NETWORK_CHANGE_KEY = "net_changes";
    String BATCH_LOOKUP_TIME_MILLS_KEY = "lookup_time_mses";

    String BATCH_REST_LOOKUP_ERROR_CODE_KEY = "hdns_err_codes";
    String BATCH_REST_LOOKUP_ERROR_MESSAGE_KEY = "hdns_err_msgs";
    String BATCH_REST_LOOKUP_IPS_KEY = "hdns_ipses";
    String BATCH_REST_LOOKUP_TTL_KEY = "hdns_ttls";
    String BATCH_REST_LOOKUP_CLIENT_IP_KEY = "hdns_client_ips";
    String BATCH_REST_LOOKUP_COST_TIME_MILLS_KEY = "hdns_time_mses";
    String BATCH_REST_LOOKUP_RETRY_TIMES_KEY = "hdns_retrys";
    String LOOKUP_RESPONSE_STATUS_CODE = "status_code";
}
