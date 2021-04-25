//
// Created by zefengwang on 2018/10/26.
//

#ifndef SELF_DNS_LOG_LOG_H
#define SELF_DNS_LOG_LOG_H

#include <android/log.h>

#define PRIORITY ANDROID_LOG_DEBUG
#define TAG "HTTPDNS"

#define VERBOSE(x...)                                         \
    do {                                                      \
        if (ANDROID_LOG_VERBOSE >= PRIORITY)                  \
            __android_log_print(ANDROID_LOG_VERBOSE, TAG, x); \
    } while(0)

#define DEBUG(x...)                                         \
    do {                                                    \
        if (ANDROID_LOG_DEBUG >= PRIORITY)                  \
            __android_log_print(ANDROID_LOG_DEBUG, TAG, x); \
    } while(0)

#define INFO(x...)                                         \
    do {                                                   \
        if (ANDROID_LOG_INFO >= PRIORITY)                  \
            __android_log_print(ANDROID_LOG_INFO, TAG, x); \
    } while(0)

#define WARN(x...)                                         \
    do {                                                   \
        if (ANDROID_LOG_WARN >= PRIORITY)                  \
            __android_log_print(ANDROID_LOG_WARN, TAG, x); \
    } while(0)

#define ERROR(x...)                                         \
    do {                                                    \
        if (ANDROID_LOG_ERROR >= PRIORITY)                  \
            __android_log_print(ANDROID_LOG_ERROR, TAG, x); \
    } while(0)

#endif  // SELF_DNS_LOG_LOG_H
