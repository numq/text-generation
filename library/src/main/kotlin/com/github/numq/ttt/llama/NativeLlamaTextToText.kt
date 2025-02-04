package com.github.numq.ttt.llama

import java.lang.ref.Cleaner

internal class NativeLlamaTextToText(
    modelPath: String,
    contextSize: Int,
    batchSize: Int,
) : AutoCloseable {
    private val nativeHandle =
        initNative(modelPath = modelPath, contextSize = contextSize, batchSize = batchSize).also { handle ->
            require(handle != -1L) { "Unable to initialize native library" }
        }

    private val cleanable = cleaner.register(this) { freeNative(nativeHandle) }

    private companion object {
        const val DEFAULT_TEMPERATURE = 1f
        const val DEFAULT_TOP_P = 1f
        const val DEFAULT_MIN_P = 0f
        const val DEFAULT_PENALTY_REPEAT = 1f
        const val DEFAULT_PENALTY_FREQ = 0f
        const val DEFAULT_PENALTY_PRESENT = 0f

        private val cleaner: Cleaner = Cleaner.create()

        @JvmStatic
        private external fun initNative(modelPath: String, contextSize: Int, batchSize: Int): Long

        @JvmStatic
        private external fun applyTemplateNative(handle: Long, messages: Array<LlamaMessage>): String

        @JvmStatic
        private external fun calculateTokensNative(handle: Long, prompt: String): Int

        @JvmStatic
        private external fun generateNative(
            handle: Long,
            prompt: String,
            temperature: Float,
            topP: Float,
            minP: Float,
            penaltyRepeat: Float,
            penaltyFreq: Float,
            penaltyPresent: Float,
        ): String

        @JvmStatic
        private external fun freeNative(handle: Long)
    }

    fun applyTemplate(messages: Array<LlamaMessage>) = applyTemplateNative(handle = nativeHandle, messages = messages)

    fun generate(
        prompt: String,
        temperature: Float = DEFAULT_TEMPERATURE,
        topP: Float = DEFAULT_TOP_P,
        minP: Float = DEFAULT_MIN_P,
        penaltyRepeat: Float = DEFAULT_PENALTY_REPEAT,
        penaltyFreq: Float = DEFAULT_PENALTY_FREQ,
        penaltyPresent: Float = DEFAULT_PENALTY_PRESENT,
    ) = generateNative(
        handle = nativeHandle,
        prompt = prompt,
        temperature = temperature,
        topP = topP,
        minP = minP,
        penaltyRepeat = penaltyRepeat,
        penaltyFreq = penaltyFreq,
        penaltyPresent = penaltyPresent
    )

    fun calculateTokens(prompt: String) = calculateTokensNative(handle = nativeHandle, prompt = prompt)

    override fun close() = cleanable.clean()
}