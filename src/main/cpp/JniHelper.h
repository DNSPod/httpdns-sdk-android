//
// JniHelper.h
// utils
//
// Created by jaredhuang on 2019-10-23.
// Copyright © 2019 Tencent. All rights reserved.

#ifndef SELF_DNS_JNIHELPER_H
#define SELF_DNS_JNIHELPER_H

#include <jni.h>

#include <string>
#include <vector>

namespace self_dns {

// char数组，或者其他POD的数组的内存开辟和释放优先使用malloc和free
#define C_SAFE_MALLOC(num, type) \
  reinterpret_cast<type *>(calloc((num), sizeof(type)))
#define C_SAFE_FREE(ptr) \
  do {                   \
    if (ptr != NULL) {   \
      free(ptr);         \
      ptr = NULL;        \
    }                    \
  } while (0)

class JniHelper {
 public:
//  static void InitializeJavaVm(JavaVM *vm, jint version);

//  static jstring String2Jstring(JNIEnv *env, const std::string &s);

//  static jstring Char2Jstring(JNIEnv *env, const char *input);

  static std::string Jstring2String(JNIEnv *env, jstring jstr);

//  static int JbyteArray2byteArray(JNIEnv *env, jbyteArray javaByteArray,
//                                  unsigned char **bufPtr, size_t *bufLen);
//
//  static int JstringArray2stringArray(JNIEnv *env, jobjectArray javaStringArray,
//                                      std::string **stringArrayPtr,
//                                      size_t *stringArrayLen);

//  static jclass FindClass(JNIEnv *env, const char *className);

//  static jmethodID FindStaticMethod(JNIEnv *env, jclass jc,
//                                    const char *methodName,
//                                    const char *methodSign);

//  static jmethodID FindMethod(JNIEnv *env, jclass jc, const char *methodName,
//                              const char *methodSign);

//  static jobject NewGlobalRef(JNIEnv *env, jobject obj);
//
//  static void DeleteLocalRef(JNIEnv *env, jobject localRef);
//
//  static jobject CreateInstance(JNIEnv *env, const char *className,
//                                const char *signature, ...);
//
//  static void CallStaticVoidMethod(JNIEnv *env, jclass clazz,
//                                   jmethodID methodId, ...);
//
//  static int CallStaticIntMethodById(JNIEnv *env, jclass jc, jmethodID methodId,
//                                     int defaultRet, ...);
//
//  static int CallStaticIntMethod(JNIEnv *env, const char *className,
//                                 const char *methodName, const char *methodSign,
//                                 jclass jc, int defaultRet, ...);
//
//  static std::string CallStaticStringMethod(JNIEnv *env, const char *className,
//                                            const char *methodName,
//                                            const char *methodSign, jclass jc,
//                                            const char *defaultRet, ...);

//  static jobject CallStaticObjectMethodById(JNIEnv *env, jclass jc,
//                                            jmethodID methodId, ...);
//
//  static jobject CallObjectMethodById(JNIEnv *env, jobject jo,
//                                      jmethodID methodId, ...);

  static void ClearEnvException(JNIEnv *env);

  static JNIEnv *GetJniEnv();

  static void JniEnvDestructor(void *reserved);

  static void DetachJniEnv();

//  static std::vector<float> JfloatArray2Vec(jfloat *origin, int len);
//
//  static jobject GetApplicationContext(JNIEnv *env);

  static int GetAndroidOsVersion();

//  static int DetachAndroidThread(pthread_t threadId);
};

}  // namespace httpdns_itop

#endif  // ITOP_DNS_JNIHELPER_H
