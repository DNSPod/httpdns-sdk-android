//
// JniHelper.cpp
// utils
//
// Created by jaredhuang on 2019-10-23.
// Copyright © 2019 Tencent. All rights reserved.

#include "JniHelper.h"
#include "log/Log.h"
#include "TlsHelper.h"

#include <sys/system_properties.h>
#include <stdlib.h>
#include <cstring>

namespace self_dns {

JavaVM *currentJavaVM_ = nullptr;
jint currentJavaVersion_ = 0;
int androidOsVersion_ = 0;

void JniHelper::InitializeJavaVm(JavaVM *vm, jint version) {
  if (currentJavaVM_ == nullptr) {
    currentJavaVM_ = vm;
    currentJavaVersion_ = version;
  }
  androidOsVersion_ = GetAndroidOsVersion();
  INFO("InitializeJavaVm: javaVersion:%d, osVersion:%d", currentJavaVersion_,
          androidOsVersion_);
}

void JniHelper::ClearEnvException(JNIEnv *env) {
  if (!env) {
    return;
  }

  jthrowable exception = env->ExceptionOccurred();

  if (exception) {
    env->ExceptionDescribe();
    env->ExceptionClear();
  }
}

jclass JniHelper::FindClass(JNIEnv *env, const char *className) {
  if (!env) {
    return nullptr;
  }

  ClearEnvException(env);
  jclass ret = env->FindClass(className);

  if (ret == nullptr) {
    ClearEnvException(env);
  }

  return ret;
}

/**
 * GetStaticMethodID
 */
jmethodID JniHelper::FindStaticMethod(JNIEnv *env, jclass jc,
                                      const char *methodName,
                                      const char *methodSign) {
  if (!env) {
    return nullptr;
  }

  jmethodID ret = nullptr;

  if (jc != nullptr) {
    ClearEnvException(env);
    ret = env->GetStaticMethodID(jc, methodName, methodSign);

    if (ret == nullptr) {
      ClearEnvException(env);
    }
  }

  return ret;
}

/**
 * GetMethodID
 */
jmethodID JniHelper::FindMethod(JNIEnv *env, jclass jc, const char *methodName,
                                const char *methodSign) {
  if (!env) {
    return nullptr;
  }

  jmethodID ret = nullptr;

  if (jc != nullptr) {
    ClearEnvException(env);
    ret = env->GetMethodID(jc, methodName, methodSign);

    if (ret == nullptr) {
      ClearEnvException(env);
    }
  }

  return ret;
}

/**
 * 将string转为jstring
 */
jstring JniHelper::String2Jstring(JNIEnv *env, const std::string &s) {
  if (!env) {
    return nullptr;
  }

  ClearEnvException(env);
  return env->NewStringUTF(s.c_str());
}

/**
 * 将char转为jstring
 */
jstring JniHelper::Char2Jstring(JNIEnv *env, const char *input) {
  if (!env) {
    return nullptr;
  }

  ClearEnvException(env);
  return env->NewStringUTF(input);
}

/**
 * 将jstring转为string
 */
std::string JniHelper::Jstring2String(JNIEnv *env, jstring jstr) {
  if (jstr == nullptr) {
    return "";
  }

  if (!env) {
    return "";
  }

  ClearEnvException(env);
  const char *chars = env->GetStringUTFChars(jstr, nullptr);
  std::string ret(chars);
  env->ReleaseStringUTFChars(jstr, chars);

  return ret;
}

/**
 * 将jbyteArray转为byteArray
 * 注意*bufPtr在使用后需要free
 */
int JniHelper::JbyteArray2byteArray(JNIEnv *env, jbyteArray javaByteArray,
                                    unsigned char **bufPtr, size_t *bufLen) {
  *bufLen = 0;

  if (!javaByteArray) {
    return -1;
  }

  if (!env) {
    return -1;
  }

  ClearEnvException(env);
  *bufLen = (size_t)env->GetArrayLength(javaByteArray);
  *bufPtr = C_SAFE_MALLOC(*bufLen, unsigned char);

  if (*bufPtr == nullptr) {
    *bufLen = 0;
    return -2;
  }

  env->GetByteArrayRegion(javaByteArray, 0, *bufLen,
                          reinterpret_cast<jbyte *>(*bufPtr));
  return 0;
}

/**
 * 将jstringArray转为stringArray
 * 注意*stringArrayPtr在使用后需要delete[]
 */
int JniHelper::JstringArray2stringArray(JNIEnv *env,
                                        jobjectArray javaStringArray,
                                        std::string **stringArrayPtr,
                                        size_t *stringArrayLen) {
  *stringArrayLen = 0;

  if (!stringArrayPtr) {
    return -1;
  }

  if (!env) {
    return -1;
  }

  ClearEnvException(env);
  *stringArrayLen = (size_t)env->GetArrayLength(javaStringArray);
  *stringArrayPtr = new std::string[*stringArrayLen];

  if (*stringArrayPtr == nullptr) {
    *stringArrayPtr = nullptr;
    return -2;
  }

  int len = *stringArrayLen;

  for (int i = 0; i < len; ++i) {
    auto jstr = (jstring)env->GetObjectArrayElement(javaStringArray, i);
    // 注意括号不能省，否则会先右结合
    (*stringArrayPtr)[i] = JniHelper::Jstring2String(env, jstr);
  }

  return 0;
}

void JniHelper::DetachJniEnv() {
  if (currentJavaVM_ != nullptr) {
    JniHelper::ClearEnvException(JniHelper::GetJniEnv());
    currentJavaVM_->DetachCurrentThread();
  }
}

void JniHelper::JniEnvDestructor(void *reserved) {
  INFO("*** JniEnvDestructor: %s", "");
  JniHelper::DetachJniEnv();
}

JNIEnv *JniHelper::GetJniEnv() {
  if (currentJavaVM_ == nullptr) {
    WARN("jvm null in GetJniEnv%s", "");
    return nullptr;
  }

  // 注册一个jni的detach
  static uint32_t tlsSlot = 0;

  if (tlsSlot == 0) {
    pthread_key_create(reinterpret_cast<pthread_key_t *>(&tlsSlot),
                       &JniEnvDestructor);
  }

  JNIEnv *env = nullptr;
  jint getEnvResult = currentJavaVM_->GetEnv(reinterpret_cast<void **>(&env),
                                             currentJavaVersion_);

  if (getEnvResult == JNI_EDETACHED) {
    // attach to this thread
    jint attachResult = currentJavaVM_->AttachCurrentThread(&env, nullptr);

    if (attachResult == JNI_ERR) {
      WARN("Attach failed in GetJniEnv%s", "");
      return nullptr;
    }

    TlsHelper::SetTlsValue(tlsSlot, static_cast<void *>(env));
  } else if (getEnvResult != JNI_OK) {
    WARN("Failed to GetJniEnv environment! Result = %d", getEnvResult);
    return nullptr;
  }

  return env;
}

jobject JniHelper::NewGlobalRef(JNIEnv *env, jobject obj) {
  jobject result = nullptr;

  if (env && obj != nullptr) {
    ClearEnvException(env);
    result = env->NewGlobalRef(obj);
  }

  return result;
}

void JniHelper::DeleteLocalRef(JNIEnv *env, jobject localRef) {
  if (env && localRef != nullptr) {
    ClearEnvException(env);
    env->DeleteLocalRef(localRef);
  }
}

jobject JniHelper::CreateInstance(JNIEnv *env, const char *className,
                                  const char *signature, ...) {
  if (!env) {
    return nullptr;
  }

  jobject instance = nullptr;
  jclass theClass = JniHelper::FindClass(env, className);

  if (theClass != nullptr) {
    ClearEnvException(env);
    jmethodID constructor = FindMethod(env, theClass, "<init>", signature);

    if (constructor != nullptr) {
      va_list args;
      va_start(args, signature);
      ClearEnvException(env);
      instance = env->NewObjectV(theClass, constructor, args);
      va_end(args);

      if (instance == nullptr) {
        WARN(
            "CreateInstance failed, invoking constructor from %s with "
            "signature %s",
            className, signature);
        ClearEnvException(env);
      }
    } else {
      WARN(
          "CreateInstance failed, getting constructor from %s with "
          "signature %s",
          className, signature);
    }

    env->DeleteLocalRef(theClass);
  } else {
    WARN("CreateInstance failed, getting class %s", className);
  }

  return instance;
}

void JniHelper::CallStaticVoidMethod(JNIEnv *env, jclass clazz,
                                     jmethodID methodId, ...) {
  if (env) {
    va_list args;
    va_start(args, methodId);
    ClearEnvException(env);
    env->CallStaticVoidMethodV(clazz, methodId, args);
    va_end(args);
  }
}

int JniHelper::CallStaticIntMethodById(JNIEnv *env, jclass jc,
                                       jmethodID methodId, int defaultRet,
                                       ...) {
  int ret = defaultRet;

  if (!env) {
    return ret;
  }

  if (jc != nullptr) {
    if (methodId != nullptr) {
      va_list args;
      va_start(args, defaultRet);
      ClearEnvException(env);
      ret = env->CallStaticIntMethodV(jc, methodId, args);
      va_end(args);
    }
  }

  return ret;
}

int JniHelper::CallStaticIntMethod(JNIEnv *env, const char *className,
                                   const char *methodName,
                                   const char *methodSign, jclass jc,
                                   int defaultRet, ...) {
  int ret = defaultRet;

  if (!env) {
    return ret;
  }

  jclass apiClass;

  if (jc != nullptr) {
    apiClass = jc;
  } else {
    apiClass = JniHelper::FindClass(env, className);
  }

  if (apiClass != nullptr) {
    jmethodID method =
        JniHelper::FindStaticMethod(env, apiClass, methodName, methodSign);

    if (method != nullptr) {
      DEBUG(
          "CallStatic className: %s, methodName: %s, clazz: %p, method: %p",
          className, methodName, apiClass, method);
      ClearEnvException(env);
      va_list args;
      va_start(args, defaultRet);
      ret = env->CallStaticIntMethodV(apiClass, method, args);
      va_end(args);
    }

    if (jc == nullptr) {
      env->DeleteLocalRef(apiClass);
    }
  }

  return ret;
}

std::string JniHelper::CallStaticStringMethod(JNIEnv *env,
                                              const char *className,
                                              const char *methodName,
                                              const char *methodSign, jclass jc,
                                              const char *defaultRet, ...) {
  std::string ret = defaultRet;

  if (!env) {
    return ret;
  }

  jclass apiClass;

  if (jc != nullptr) {
    apiClass = jc;
  } else {
    apiClass = JniHelper::FindClass(env, className);
  }

  if (apiClass != nullptr) {
    jmethodID method =
        JniHelper::FindStaticMethod(env, apiClass, methodName, methodSign);

    if (method != nullptr) {
      WARN("binding className: %s, methodName: %s, clazz: %p, method: %p",
              className, methodName, apiClass, method);
      ClearEnvException(env);
      va_list args;
      va_start(args, defaultRet);
      auto retJs =
          (jstring)env->CallStaticObjectMethodV(apiClass, method, args);
      va_end(args);

      if (retJs == nullptr) {
        ClearEnvException(env);
      }

      const char *retCh = env->GetStringUTFChars(retJs, nullptr);
      ret = std::string(retCh);
      env->ReleaseStringUTFChars(retJs, retCh);
    }

    if (jc == nullptr) {
      env->DeleteLocalRef(apiClass);
    }
  }

  return ret;
}

jobject JniHelper::CallStaticObjectMethodById(JNIEnv *env, jclass jc,
                                              jmethodID methodId, ...) {
  jobject ret = nullptr;
  if (!env) {
    return ret;
  }

  if (jc != nullptr) {
    if (methodId != nullptr) {
      va_list args;
      va_start(args, methodId);
      ClearEnvException(env);
      ret = env->CallStaticObjectMethodV(jc, methodId, args);
      va_end(args);

      if (ret == nullptr) {
        ClearEnvException(env);
      }
    }
  }

  return ret;
}

jobject JniHelper::CallObjectMethodById(JNIEnv *env, jobject jo,
                                        jmethodID methodId, ...) {
  jobject ret = nullptr;
  if (!env) {
    return ret;
  }

  if (jo != nullptr) {
    if (methodId != nullptr) {
      va_list args;
      va_start(args, methodId);
      ClearEnvException(env);
      ret = env->CallObjectMethodV(jo, methodId, args);
      va_end(args);

      if (ret == nullptr) {
        ClearEnvException(env);
      }
    }
  }

  return ret;
}

std::vector<float> JniHelper::JfloatArray2Vec(jfloat *origin, int len) {
  std::vector<float> result;

  if (origin == nullptr) {
    return result;
  }

  for (int i = 0; i < len; i++) {
    result.push_back(*(origin + i));
  }

  return result;
}

jobject JniHelper::GetApplicationContext(JNIEnv *env) {
  if (!env) {
    return nullptr;
  }
  ClearEnvException(env);
  // ActivityThread
  jclass activityThread = FindClass(env, "android/app/ActivityThread");
  jmethodID currentActivityThread =
      FindStaticMethod(env, activityThread, "currentActivityThread",
                       "()Landroid/app/ActivityThread;");
  jobject at =
      CallStaticObjectMethodById(env, activityThread, currentActivityThread);
  // getApplication Context
  jmethodID getApplication = FindMethod(env, activityThread, "getApplication",
                                        "()Landroid/app/Application;");
  jobject context = CallObjectMethodById(env, at, getApplication);
  return context;
}

int JniHelper::GetAndroidOsVersion() {
  char osVersion[PROP_VALUE_MAX + 1];
  memset(osVersion, 0, sizeof(osVersion));
  int osVersionLen = __system_property_get("ro.build.version.sdk", osVersion);
  if (osVersionLen > 0) {
    return atoi(osVersion);
  }
  return 0;
}

int JniHelper::DetachAndroidThread(pthread_t threadId) {
  if (androidOsVersion_ == 0 || androidOsVersion_ >= 26) {
    // 此时不detach
    DEBUG("DetachAndroidThread return for api version:%d",
                androidOsVersion_);
    return 0;
  }
  return pthread_detach(threadId);
}

}  // namespace vpncomm

