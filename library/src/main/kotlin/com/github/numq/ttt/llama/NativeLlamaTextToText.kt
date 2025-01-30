package com.github.numq.ttt.llama

import java.lang.ref.Cleaner

internal class NativeLlamaTextToText(modelPath: String) : AutoCloseable {
    private val nativeHandle = initNative(modelPath = modelPath).also { handle ->
        require(handle != -1L) { "Unable to initialize native library" }
    }

    private val cleanable = cleaner.register(this) { freeNative(nativeHandle) }

    private companion object {
        val cleaner: Cleaner = Cleaner.create()

        @JvmStatic
        external fun initNative(modelPath: String): Long

        @JvmStatic
        external fun generateNative(handle: Long, prompt: String): String

        @JvmStatic
        external fun freeNative(handle: Long)
    }

    fun generate(prompt: String) = generateNative(handle = nativeHandle, prompt = prompt)

    override fun close() = cleanable.clean()
}