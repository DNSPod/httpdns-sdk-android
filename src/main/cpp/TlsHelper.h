//
// TlsHelper.h
// utils
//
// Created by jaredhuang on 2019-10-23.
// Copyright © 2019 Tencent. All rights reserved.

#ifndef SELF_DNS_TLSHELPER_H
#define SELF_DNS_TLSHELPER_H

#include <pthread.h>
#include <stdint.h>
#include <sys/syscall.h>
#include <unistd.h>

namespace self_dns {
/**
 * 线程本地变量的Android(linux)实现，全部inline
 */
class TlsHelper {
 public:
  /**
   * Returns the currently executing thread's id
   */
  static inline uint32_t GetCurrentThreadId() {
    // gettid不可移植，但更唯一，返回的是pid
    return static_cast<uint32_t>(gettid());
    // pthread_self是posix标准，但仅在同一进程中唯一
    // return pthread_self();
  }

  /**
   * Allocates a thread local store slot
   */
  static inline uint32_t AllocTlsSlot() {
    // allocate a per-thread mem slot
    pthread_key_t Key = 0;

    if (pthread_key_create(&Key, NULL) != 0) {
      Key = 0xFFFFFFFF;  // matches the Windows TlsAlloc() retval
    }

    return static_cast<uint32_t>(Key);
  }

  /**
   * Sets a value in the specified TLS slot
   *
   * @param slotIndex the TLS index to store it in
   * @param value the value to store in the slot
   */
  static inline void SetTlsValue(uint32_t slotIndex, void *value) {
    pthread_setspecific((pthread_key_t)slotIndex, value);
  }

  /**
   * Reads the value stored at the specified TLS slot
   *
   * @return the value stored in the slot
   */
  static inline void *GetTlsValue(uint32_t slotIndex) {
    return pthread_getspecific((pthread_key_t)slotIndex);
  }

  /**
   * Frees a previously allocated TLS slot
   *
   * @param slotIndex the TLS index to store it in
   */
  static inline void FreeTlsSlot(uint32_t slotIndex) {
    pthread_key_delete((pthread_key_t)slotIndex);
  }
};

}  // namespace self_dns

#endif  // SELF_DNS_TLSHELPER_H
