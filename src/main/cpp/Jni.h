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

JNIEXPORT jbyteArray JNICALL
Java_com_tencent_msdk_dns_base_jni_Jni_desCrypt(JNIEnv *env,
                                                jclass cls,
                                                jbyteArray _src,
                                                jstring _key,
                                                jint mode);

JNIEXPORT jbyteArray JNICALL
Java_com_tencent_msdk_dns_base_jni_Jni_aesCrypt(JNIEnv *env,
                                                jclass cls,
                                                jbyteArray _src,
                                                jstring _key,
                                                jint mode,
                                                jbyteArray _aes_iv);

JNIEXPORT jint JNICALL Java_com_tencent_msdk_dns_base_jni_Jni_sendToUnity
        (JNIEnv *env, jclass cls, jstring _str);


#ifdef __cplusplus
}
#endif

#endif  // SELF_DNS_JNI_H
