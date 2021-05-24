//
// Created by zefengwang on 2018/9/3.
//

#ifndef SELF_DNS_STACK_NETWORK_STACK_H
#define SELF_DNS_STACK_NETWORK_STACK_H

namespace self_dns {
namespace NetworkStack {
const int NONE/* UNKNOWN */ = 0;
const int IPV4_ONLY = 1;
const int IPV6_ONLY = 2;
const int DUAL_STACK = 3;
int get();
}  // namespace NetworkStack
}  // namespace self_dns

#endif  // SELF_DNS_STACK_NETWORK_STACK_H
