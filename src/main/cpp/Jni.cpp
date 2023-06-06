//
// Created by zefengwang on 2018/9/3.
//
#include "Jni.h"

#include <string>
#include <malloc.h>

#include "JniHelper.h"
#include "stack/NetworkStack.h"
#include "adapter/HttpDnsBridge.h"

extern "C" JNIEXPORT int

JNICALL
Java_com_tencent_msdk_dns_base_jni_Jni_getNetworkStack(
        JNIEnv *env,
        jclass /* cls */) {
    return self_dns::NetworkStack::get();
}

JNIEXPORT jint JNICALL Java_com_tencent_msdk_dns_base_jni_Jni_sendToUnity
        (JNIEnv *env, jclass cls, jstring _str) {
    std::string str_msg = self_dns::JniHelper::Jstring2String(env, _str);
    int res = -1;
    HTTPDNSSendToUnity send_to_unity_func = HTTPDNSGetBridge();
    if (send_to_unity_func) {
        res = send_to_unity_func(str_msg.c_str());
    }
    return res;
}