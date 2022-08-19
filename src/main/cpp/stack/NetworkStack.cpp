//
// Created by zefengwang on 2018/9/3.
//

#include <errno.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>

#include "NetworkStack.h"

#include "../log/Log.h"

namespace self_dns {
namespace NetworkStack {

bool canConnect(int domain, const struct sockaddr *addr, socklen_t addrLen);

bool isSupportIPv4();

bool isSupportIPv6();

int NetworkStack::get() {
    int curNetworkStackType = NetworkStack::NONE;

    if (isSupportIPv4()) {
//        DEBUG("support IPv4");
        curNetworkStackType |= NetworkStack::IPV4_ONLY;
    }
    if (isSupportIPv6()) {
//        DEBUG("support IPv6");
        curNetworkStackType |= NetworkStack::IPV6_ONLY;
    }

    return curNetworkStackType;
}

bool canConnect(int domain, const struct sockaddr *addr, socklen_t addrLen) {
    // Normally only a single protocol exists to support a particular socket type within a given protocol family,
    // in which case protocol can be specified as 0.
    int socketFd = socket(domain, SOCK_DGRAM, /*protocol*/0);
    if (socketFd < 0) {
        return false;
    }

    int funRet;
    do {
        funRet = connect(socketFd, addr, addrLen);
    } while (funRet < 0 && errno == EINTR);
    bool canConnect = funRet == 0;
    do {
        funRet = close(socketFd);
    } while (funRet < 0 && errno == EINTR);

    return canConnect;
}

union sockaddr_union {
    struct sockaddr generic;
    struct sockaddr_in in;
    struct sockaddr_in6 in6;
};

bool isSupportIPv4() {
    static const struct sockaddr_in v4TestAddr = {
            .sin_family = AF_INET,
            .sin_port = htons(0xFFFF),
            .sin_addr.s_addr = htonl(0x08080808L),  // 8.8.8.8
    };
    sockaddr_union addr = {.in = v4TestAddr};
    return canConnect(PF_INET, &addr.generic, sizeof(addr.in));
}

bool isSupportIPv6() {
    static const struct sockaddr_in6 v6TestAddr = {
            .sin6_family = AF_INET6,
            .sin6_port = htons(0xFFFF),
            .sin6_addr.s6_addr = {0x20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
    };
    sockaddr_union addr = {.in6 = v6TestAddr};
    return canConnect(PF_INET6, &addr.generic, sizeof(addr.in6));
}

}  // namespace NetworkStack
}  // namespace self_dns
