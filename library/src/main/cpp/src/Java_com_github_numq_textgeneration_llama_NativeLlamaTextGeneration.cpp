#include "Java_com_github_numq_textgeneration_llama_NativeLlamaTextGeneration.h"

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

static std::string generate(
        const llama_vocab *vocab,
        llama_context *ctx,
        llama_sampler *sampler,
        const std::string &prompt
) {
    std::string response;

    auto isFirst = llama_get_kv_cache_used_cells(ctx) == 0;

    auto nPromptTokens = -llama_tokenize(vocab, prompt.c_str(), static_cast<int>(prompt.size()), nullptr, 0, isFirst,
                                         true);
    std::vector<llama_token> promptTokens(nPromptTokens);
    if (llama_tokenize(
            vocab,
            prompt.c_str(),
            static_cast<int>(prompt.size()),
            promptTokens.data(),
            static_cast<int>(promptTokens.size()),
            isFirst,
            true
    ) < 0) {
        throw std::runtime_error("Failed to tokenize the prompt");
    }

    llama_batch batch = llama_batch_get_one(promptTokens.data(), static_cast<int>(promptTokens.size()));
    llama_token newTokenId;

    while (true) {
        auto nCtx = static_cast<int>(llama_n_ctx(ctx));
        auto nCtxUsed = llama_get_kv_cache_used_cells(ctx);
        if (nCtxUsed + batch.n_tokens > nCtx) {
            throw std::runtime_error("Context size exceeded");
        }

        if (llama_decode(ctx, batch)) {
            throw std::runtime_error("Failed to decode");
        }

        newTokenId = llama_sampler_sample(sampler, ctx, -1);

        if (llama_vocab_is_eog(vocab, newTokenId)) {
            break;
        }

        char buf[256];
        auto n = llama_token_to_piece(vocab, newTokenId, buf, sizeof(buf), 0, true);
        if (n < 0) {
            throw std::runtime_error("Failed to convert token to piece");
        }
        std::string piece(buf, n);
        response += piece;

        batch = llama_batch_get_one(&newTokenId, 1);
    }

    return response;
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
Java_com_github_numq_textgeneration_llama_NativeLlamaTextGeneration_initNative(JNIEnv *env, jclass thisClass,
                                                                               jstring modelPath,
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

JNIEXPORT jstring JNICALL
Java_com_github_numq_textgeneration_llama_NativeLlamaTextGeneration_generateNative(JNIEnv *env, jclass thisClass,
                                                                                   jlong handle,
                                                                                   jobjectArray messages,
                                                                                   jfloat temperature,
                                                                                   jfloat topP,
                                                                                   jfloat repetitionPenalty, jint topK,
                                                                                   jint seed) {
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
            auto roleChars = env->GetStringUTFChars(role, nullptr);
            chatMessages[i].role = roleChars;
            env->ReleaseStringUTFChars(role, roleChars);

            jfieldID contentFieldID = env->GetFieldID(messageClass, "content", "Ljava/lang/String;");
            auto content = reinterpret_cast<jstring>(env->GetObjectField(messageObj, contentFieldID));
            auto contentChars = env->GetStringUTFChars(content, nullptr);
            chatMessages[i].content = contentChars;
            env->ReleaseStringUTFChars(content, contentChars);
        }

        auto context = it->second.get();

        auto vocab = llama_model_get_vocab(model.get());
        if (!vocab) {
            throw std::runtime_error("Failed to get model vocab");
        }

        auto sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
        if (!sampler) {
            throw std::runtime_error("Failed to initialize sampler");
        }

        auto tmpl = llama_model_chat_template(model.get(), nullptr);

        std::vector<char> formatted(llama_n_ctx(context));

        int new_len = llama_chat_apply_template(tmpl, chatMessages.data(), chatMessages.size(), true,
                                                formatted.data(), static_cast<int32_t>(formatted.size()));

        if (new_len > static_cast<int>(formatted.size())) {
            formatted.resize(new_len);
            new_len = llama_chat_apply_template(tmpl, chatMessages.data(), chatMessages.size(), true, formatted.data(),
                                                static_cast<int32_t>(formatted.size()));
        }

        std::string formattedPrompt(formatted.begin(), formatted.begin() + new_len);

        try {
            llama_sampler_chain_add(
                    sampler,
                    llama_sampler_init_top_k(topK)
            );

            llama_sampler_chain_add(
                    sampler,
                    llama_sampler_init_top_p(topP, true)
            );

            llama_sampler_chain_add(
                    sampler,
                    seed > 0 ? llama_sampler_init_dist(seed) : llama_sampler_init_greedy()
            );

            llama_sampler_chain_add(
                    sampler,
                    llama_sampler_init_temp(temperature)
            );

            llama_sampler_chain_add(
                    sampler,
                    llama_sampler_init_penalties(
                            0,
                            repetitionPenalty,
                            0,
                            0
                    )
            );

            auto result = generate(vocab, context, sampler, formattedPrompt);
            if (result.empty()) {
                throw std::runtime_error("Unable to generate response");
            }

            return env->NewStringUTF(result.c_str());
        } catch (...) {
            llama_sampler_free(sampler);

            throw;
        }
    } catch (const std::exception &e) {
        handleException(env, e.what());
    }

    return nullptr;
}

JNIEXPORT void JNICALL
Java_com_github_numq_textgeneration_llama_NativeLlamaTextGeneration_freeNative(JNIEnv *env, jclass thisClass,
                                                                               jlong handle) {
    std::shared_lock<std::shared_mutex> lock(mutex);

    try {
        if (pointers.erase(handle) == 0) {
            handleException(env, "Unable to free native pointer");
        }
    } catch (const std::exception &e) {
        handleException(env, e.what());
    }
}