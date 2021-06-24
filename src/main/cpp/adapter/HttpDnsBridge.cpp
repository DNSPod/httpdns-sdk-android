//
// Created by jaredhuang on 2021/02/23.
//

#include "HttpDnsBridge.h"

#ifdef __cplusplus
extern "C" {
#endif

// 初始化，主要是如果用了命名空间会变成_ZN3mna9MnaBridge12MNASetBridgeEPFiPKcE之类的格式
__attribute__ ((visibility ("default"))) HTTPDNSSendToUnity HTTPDNSGetBridge() {
    return self_dns::HttpDnsBridge::HTTPDNSGetBridge();
}

__attribute__ ((visibility ("default"))) void HTTPDNSSetBridge(HTTPDNSSendToUnity bridge) {
    self_dns::HttpDnsBridge::HTTPDNSSetBridge(bridge);
}

#ifdef __cplusplus
}
#endif

HTTPDNSSendToUnity self_dns::HttpDnsBridge::httpDnsSendToUnity = 0;

void self_dns::HttpDnsBridge::HTTPDNSSetBridge(HTTPDNSSendToUnity bridge) {
    self_dns::HttpDnsBridge::httpDnsSendToUnity = bridge;
}

HTTPDNSSendToUnity self_dns::HttpDnsBridge::HTTPDNSGetBridge() {
    return self_dns::HttpDnsBridge::httpDnsSendToUnity;
}