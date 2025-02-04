#include "Java_com_github_numq_ttt_llama_NativeLlamaTextToText.h"

static jclass exceptionClass;
static std::shared_mutex mutex;
static std::unordered_map<jlong, llama_context_ptr> pointers;
static llama_model_ptr model;

void handleException(JNIEnv *env, const std::string &errorMessage) {
    env->ThrowNew(exceptionClass, errorMessage.c_str());
}

llama_context *getPointer(jlong handle) {
    auto it = pointers.find(handle);
    if (it == pointers.end()) {
        throw std::runtime_error("Invalid handle");
    }
    return it->second.get();
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;

    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_8) != JNI_OK) {
        throw std::runtime_error("Failed to get JNI environment");
    }

    exceptionClass = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/RuntimeException")));
    if (exceptionClass == nullptr) {
        throw std::runtime_error("Failed to find java/lang/RuntimeException class");
    }

    llama_backend_init();

    return JNI_VERSION_1_8;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    JNIEnv *env;

    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_8) != JNI_OK) return;

    if (exceptionClass) env->DeleteGlobalRef(exceptionClass);

    pointers.clear();

    model = nullptr;

    llama_backend_free();
}

JNIEXPORT jlong JNICALL
Java_com_github_numq_ttt_llama_NativeLlamaTextToText_initNative(JNIEnv *env, jclass thisClass, jstring modelPath,
                                                                jint contextSize, jint batchSize) {
    std::unique_lock<std::shared_mutex> lock(mutex);

    try {
        const char *modelPathChars = env->GetStringUTFChars(modelPath, nullptr);
        if (!modelPathChars) {
            throw std::runtime_error("Failed to get model path string");
        }

        std::string modelPathStr(modelPathChars);
        env->ReleaseStringUTFChars(modelPath, modelPathChars);

        if (modelPathStr.empty()) {
            throw std::runtime_error("Model path should not be empty");
        }

        auto modelParams = llama_model_default_params();

        auto loadedModel = llama_model_load_from_file(modelPathStr.c_str(), modelParams);

        if (!llama_model_load_from_file(modelPathStr.c_str(), modelParams)) {
            throw std::runtime_error("Failed to load model");
        }

        model = llama_model_ptr(loadedModel);

        auto contextParams = llama_context_default_params();
        contextParams.n_ctx = contextSize;
        contextParams.n_batch = batchSize;

        auto context = llama_init_from_model(model.get(), contextParams);
        if (!context) {
            throw std::runtime_error("Failed to create context");
        }

        llama_context_ptr ptr(context);

        auto handle = reinterpret_cast<jlong>(ptr.get());

        pointers[handle] = std::move(ptr);

        return handle;
    } catch (const std::exception &e) {
        handleException(env, e.what());
        return -1;
    }
}

JNIEXPORT jint JNICALL
Java_com_github_numq_ttt_llama_NativeLlamaTextToText_calculateTokensNative(JNIEnv *env, jclass thisClass, jlong handle,
                                                                           jstring prompt) {
    std::shared_lock<std::shared_mutex> lock(mutex);

    try {
        auto it = pointers.find(handle);
        if (it == pointers.end()) {
            throw std::runtime_error("Invalid handle");
        }

        const char *promptChars = env->GetStringUTFChars(prompt, nullptr);
        if (!promptChars) {
            throw std::runtime_error("Failed to get prompt string");
        }

        std::string promptStr(promptChars);
        env->ReleaseStringUTFChars(prompt, promptChars);

        auto vocab = llama_model_get_vocab(model.get());

        if (!vocab) {
            throw std::runtime_error("Failed to get model vocab");
        }

        auto isFirst = llama_get_kv_cache_used_cells(it->second.get()) == 0;

        auto nPrompt = -llama_tokenize(
                vocab,
                promptStr.c_str(),
                static_cast<int32_t>(promptStr.size()),
                nullptr,
                0,
                isFirst,
                true
        );

        if (nPrompt <= 0) {
            throw std::runtime_error("Failed to calculate number of tokens");
        }

        return nPrompt;
    } catch (const std::exception &e) {
        handleException(env, e.what());

        return 0;
    }
}


JNIEXPORT jstring JNICALL
Java_com_github_numq_ttt_llama_NativeLlamaTextToText_applyTemplateNative(JNIEnv *env, jclass thisClass, jlong handle,
                                                                         jobjectArray messages) {
    std::shared_lock<std::shared_mutex> lock(mutex);

    try {
        auto it = pointers.find(handle);
        if (it == pointers.end()) {
            throw std::runtime_error("Invalid handle");
        }

        jsize messageCount = env->GetArrayLength(messages);
        std::vector<llama_chat_message> chatMessages(messageCount);

        for (jsize i = 0; i < messageCount; ++i) {
            jobject messageObj = env->GetObjectArrayElement(messages, i);
            jclass messageClass = env->GetObjectClass(messageObj);

            jfieldID roleFieldID = env->GetFieldID(messageClass, "role", "Ljava/lang/String;");
            auto role = reinterpret_cast<jstring>(env->GetObjectField(messageObj, roleFieldID));
            const char *roleChars = env->GetStringUTFChars(role, nullptr);
            chatMessages[i].role = roleChars;
            env->ReleaseStringUTFChars(role, roleChars);

            jfieldID contentFieldID = env->GetFieldID(messageClass, "content", "Ljava/lang/String;");
            auto content = reinterpret_cast<jstring>(env->GetObjectField(messageObj, contentFieldID));
            const char *contentChars = env->GetStringUTFChars(content, nullptr);
            chatMessages[i].content = contentChars;
            env->ReleaseStringUTFChars(content, contentChars);
        }

        auto vocab = llama_model_get_vocab(model.get());
        if (!vocab) {
            throw std::runtime_error("Failed to get model vocab");
        }

        auto tmpl = llama_model_chat_template(model.get(), nullptr);

        std::vector<char> formatted(llama_n_ctx(it->second.get()));

        int new_len = llama_chat_apply_template(tmpl, chatMessages.data(), chatMessages.size(), true,
                                                formatted.data(), static_cast<int32_t>(formatted.size()));

        if (new_len > static_cast<int>(formatted.size())) {
            formatted.resize(new_len);
            new_len = llama_chat_apply_template(tmpl, chatMessages.data(), chatMessages.size(), true, formatted.data(),
                                                static_cast<int32_t>(formatted.size()));
        }

        std::string formattedPrompt(formatted.begin(), formatted.begin() + new_len);

        return env->NewStringUTF(formattedPrompt.c_str());
    } catch (const std::exception &e) {
        handleException(env, e.what());

        return nullptr;
    }
}

JNIEXPORT jstring JNICALL
Java_com_github_numq_ttt_llama_NativeLlamaTextToText_generateNative(JNIEnv *env, jclass thisClass, jlong handle,
                                                                    jstring prompt, jfloat temperature, jfloat topP,
                                                                    jfloat minP, jfloat penaltyRepeat,
                                                                    jfloat penaltyFreq, jfloat penaltyPresent) {
    std::shared_lock<std::shared_mutex> lock(mutex);

    try {
        auto it = pointers.find(handle);
        if (it == pointers.end()) {
            throw std::runtime_error("Invalid handle");
        }

        const char *promptChars = env->GetStringUTFChars(prompt, nullptr);
        if (!promptChars) {
            throw std::runtime_error("Failed to get prompt string");
        }

        std::string promptStr(promptChars);
        env->ReleaseStringUTFChars(prompt, promptChars);

        auto vocab = llama_model_get_vocab(model.get());
        if (!vocab) {
            throw std::runtime_error("Failed to get model vocab");
        }

        auto isFirst = llama_get_kv_cache_used_cells(it->second.get()) == 0;

        auto nPrompt = -llama_tokenize(
                vocab,
                promptStr.c_str(),
                static_cast<int32_t>(promptStr.size()),
                nullptr,
                0,
                isFirst,
                true
        );

        if (nPrompt <= 0) {
            throw std::runtime_error("Failed to calculate number of tokens");
        }

        std::vector<llama_token> tokens(nPrompt);

        if (llama_tokenize(
                vocab,
                promptStr.c_str(),
                static_cast<int32_t>(promptStr.size()),
                tokens.data(),
                static_cast<int32_t>(tokens.size()),
                true,
                true
        ) < 0) {
            throw std::runtime_error("Failed to tokenize prompt");
        }

        auto batch = llama_batch_get_one(tokens.data(), nPrompt);

        auto samplerParams = llama_sampler_chain_default_params();

        auto sampler = llama_sampler_chain_init(samplerParams);
        if (!sampler) {
            throw std::runtime_error("Failed to initialize sampler");
        }

        try {
            llama_sampler_chain_add(
                    sampler,
                    llama_sampler_init_greedy()
            );

            llama_sampler_chain_add(
                    sampler,
                    llama_sampler_init_temp(temperature)
            );

            llama_sampler_chain_add(
                    sampler,
                    llama_sampler_init_top_p(topP, 0)
            );

            llama_sampler_chain_add(
                    sampler,
                    llama_sampler_init_min_p(minP, 0)
            );

            llama_sampler_chain_add(
                    sampler,
                    llama_sampler_init_penalties(
                            0,
                            penaltyRepeat,
                            penaltyFreq,
                            penaltyPresent
                    )
            );

            std::string result;
            llama_token new_token_id;

            for (int n_pos = 0; n_pos + batch.n_tokens < nPrompt + 32;) {
                if (llama_decode(it->second.get(), batch)) {
                    throw std::runtime_error("Failed to decode batch");
                }

                n_pos += batch.n_tokens;

                new_token_id = llama_sampler_sample(sampler, it->second.get(), -1);
                if (llama_vocab_is_eog(vocab, new_token_id)) {
                    break;
                }

                char token_output[256];
                int token_len = llama_token_to_piece(vocab, new_token_id, token_output, sizeof(token_output), 0, true);

                if (token_len < 0) {
                    throw std::runtime_error("Failed to convert token to piece");
                }

                result.append(token_output, token_len);

                batch = llama_batch_get_one(&new_token_id, 1);
            }

            llama_sampler_free(sampler);

            return env->NewStringUTF(result.c_str());
        } catch (...) {
            llama_sampler_free(sampler);

            throw;
        }
    } catch (const std::exception &e) {
        handleException(env, e.what());

        return nullptr;
    }
}

JNIEXPORT void JNICALL
Java_com_github_numq_ttt_llama_NativeLlamaTextToText_freeNative(JNIEnv *env, jclass thisClass, jlong handle) {
    std::shared_lock<std::shared_mutex> lock(mutex);

    try {
        if (pointers.erase(handle) == 0) {
            handleException(env, "Unable to free native pointer");
        }
    } catch (const std::exception &e) {
        handleException(env, e.what());
    }
}