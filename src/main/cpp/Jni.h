//
// Created by zefengwang on 2018/11/16.
//

#ifndef SELF_DNS_JNI_H
#define SELF_DNS_JNI_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL
Java_com_tencent_msdk_dns_base_jni_Jni_getNetworkStack(JNIEnv *env, jclass type);

JNIEXPORT jint JNICALL Java_com_tencent_msdk_dns_base_jni_Jni_sendToUnity
        (JNIEnv *env, jclass cls, jstring _str);


#ifdef __cplusplus
}
#endif

#endif  // SELF_DNS_JNI_H
