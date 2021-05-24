//
// Created by zefengwang on 2018/9/3.
//
#include "Jni.h"

#include <string>
#include <malloc.h>

#include "JniHelper.h"
#include "stack/NetworkStack.h"
#include "des/des.h"
#include "aes/aes.h"

extern "C" JNIEXPORT int

JNICALL
Java_com_tencent_msdk_dns_base_jni_Jni_getNetworkStack(
        JNIEnv *env,
        jclass /* cls */) {
    return self_dns::NetworkStack::get();
}

JNIEXPORT jbyteArray JNICALL
Java_com_tencent_msdk_dns_base_jni_Jni_desCrypt(JNIEnv *env,
                                                jclass cls,
                                                jbyteArray _src,
                                                jstring _key,
                                                jint mode) {
    (void)cls;

    unsigned char *src = nullptr;
    std::size_t srcLen = 0;
    unsigned char *output = nullptr;
    unsigned int outLen = 0;
    self_dns::JniHelper::JbyteArray2byteArray(env, _src, &src, &srcLen);
    std::string key = self_dns::JniHelper::Jstring2String(env, _key);

    self_dns::des_crypt(src, srcLen, (const unsigned char *) key.c_str(), mode, &output, &outLen);
    jbyteArray jarray = nullptr;
    if (output != nullptr && outLen > 0) {
        self_dns::JniHelper::ClearEnvException(env);
        jarray = env->NewByteArray(outLen);
        jbyte *bytes = (jbyte *)output;
        env->SetByteArrayRegion(jarray, 0, outLen, bytes);
    }
    // 注意des_crypt使用了malloc
    C_SAFE_FREE(output);
    // 注意JbyteArray2byteArray使用malloc
    C_SAFE_FREE(src);
    return jarray;
}

JNIEXPORT jbyteArray JNICALL
Java_com_tencent_msdk_dns_base_jni_Jni_aesCrypt(JNIEnv *env,
                                                jclass cls,
                                                jbyteArray _src,
                                                jstring _key,
                                                jint mode,
                                                jbyteArray _aes_iv) {
    (void)cls;

    unsigned char *src = nullptr;
    size_t src_len = 0;
    unsigned char *aes_iv = nullptr;
    size_t aes_iv_len = 0;
    unsigned char *output = nullptr;
    unsigned int outLen = 0;
    self_dns::JniHelper::JbyteArray2byteArray(env, _src, &src, &src_len);
    std::string key = self_dns::JniHelper::Jstring2String(env, _key);
    jbyteArray jarray = nullptr;
    outLen = self_dns::AesGetOutLen(src_len, mode);

    self_dns::JniHelper::JbyteArray2byteArray(env, _aes_iv, &aes_iv, &aes_iv_len);

    if (outLen > 0) {
        output = new unsigned char[outLen]();
        outLen = self_dns::AesCryptWithKey(src, src_len,
                                           output, mode,
                                           (const unsigned char *) key.c_str(),
                                           (const unsigned char *) aes_iv);
        if (output != nullptr && outLen > 0) {
            self_dns::JniHelper::ClearEnvException(env);
            jarray = env->NewByteArray(outLen);
            jbyte *bytes = (jbyte *) output;
            env->SetByteArrayRegion(jarray, 0, outLen, bytes);
        }
    }
    C_SAFE_FREE(output);
    C_SAFE_FREE(src);
    C_SAFE_FREE(aes_iv);
    return jarray;
}