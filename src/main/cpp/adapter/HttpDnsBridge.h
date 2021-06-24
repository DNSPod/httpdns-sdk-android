//
// Created by jaredhuang on 2021/02/23.
//

#ifndef SELF_DNS_HTTP_DNS_BRIDGE_H
#define SELF_DNS_HTTP_DNS_BRIDGE_H

#ifdef __cplusplus
extern "C" {
#endif

typedef int (*HTTPDNSSendToUnity)(const char *jsonStr);

__attribute__ ((visibility ("default"))) HTTPDNSSendToUnity HTTPDNSGetBridge();

__attribute__ ((visibility ("default"))) void HTTPDNSSetBridge(HTTPDNSSendToUnity bridge);

namespace self_dns {

    class HttpDnsBridge {
    public:

        static HTTPDNSSendToUnity HTTPDNSGetBridge();

        static void HTTPDNSSetBridge(HTTPDNSSendToUnity bridge);

    private:
        static HTTPDNSSendToUnity httpDnsSendToUnity;
    };
}


#ifdef __cplusplus
}
#endif
#endif  // SELF_DNS_HTTP_DNS_BRIDGE_H
