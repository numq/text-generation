package com.github.numq.textgeneration.llama

import java.lang.ref.Cleaner

internal class NativeLlamaTextGeneration(modelPath: String, contextSize: Int, batchSize: Int) : AutoCloseable {
    private val nativeHandle = initNative(
        modelPath = modelPath,
        contextSize = contextSize,
        batchSize = batchSize
    ).also { handle ->
        require(handle != -1L) { "Unable to initialize native library" }
    }

    private val cleanable = cleaner.register(this) { freeNative(nativeHandle) }

    private companion object {
        const val DEFAULT_TEMPERATURE = .98f
        const val DEFAULT_TOP_P = .37f
        const val DEFAULT_REPETITION_PENALTY = 1.18f
        const val DEFAULT_TOP_K = 100

        private val cleaner: Cleaner = Cleaner.create()

        @JvmStatic
        private external fun initNative(modelPath: String, contextSize: Int, batchSize: Int): Long

        @JvmStatic
        private external fun generateNative(
            handle: Long,
            messages: Array<NativeLlamaMessage>,
            temperature: Float,
            topP: Float,
            repetitionPenalty: Float,
            topK: Int,
            seed: Int,
        ): String

        @JvmStatic
        private external fun freeNative(handle: Long)
    }

    fun generate(
        messages: Array<NativeLlamaMessage>,
        temperature: Float = DEFAULT_TEMPERATURE,
        topP: Float = DEFAULT_TOP_P,
        repetitionPenalty: Float = DEFAULT_REPETITION_PENALTY,
        topK: Int = DEFAULT_TOP_K,
        seed: Int = 0,
    ) = generateNative(
        handle = nativeHandle,
        messages = messages,
        temperature = temperature,
        topP = topP,
        repetitionPenalty = repetitionPenalty,
        topK = topK,
        seed = seed
    )

    override fun close() = cleanable.clean()
}