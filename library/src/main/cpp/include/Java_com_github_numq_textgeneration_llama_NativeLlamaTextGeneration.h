#include <jni.h>
#include <iostream>
#include <shared_mutex>
#include <mutex>
#include <unordered_map>
#include <memory>
#include <vector>
#include "llama.h"
#include "llama-cpp.h"

#ifndef _Included_com_github_numq_textgeneration_llama_NativeLlamaTextGeneration
#define _Included_com_github_numq_textgeneration_llama_NativeLlamaTextGeneration
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_com_github_numq_textgeneration_llama_NativeLlamaTextGeneration_initNative
        (JNIEnv *, jclass, jstring, jint, jint);

JNIEXPORT jstring JNICALL Java_com_github_numq_textgeneration_llama_NativeLlamaTextGeneration_generateNative
        (JNIEnv *, jclass, jlong, jobjectArray, jfloat, jfloat, jfloat, jint, jint);

JNIEXPORT void JNICALL Java_com_github_numq_textgeneration_llama_NativeLlamaTextGeneration_freeNative
        (JNIEnv *, jclass, jlong);

#ifdef __cplusplus
}
#endif
#endif
