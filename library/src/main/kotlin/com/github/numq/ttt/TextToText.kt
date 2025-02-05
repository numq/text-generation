package com.github.numq.ttt

import com.github.numq.ttt.llama.LlamaMessage
import com.github.numq.ttt.llama.LlamaTextToText
import com.github.numq.ttt.llama.NativeLlamaTextToText

interface TextToText : AutoCloseable {
    interface Llama : TextToText {
        companion object {
            private const val DEFAULT_CONTEXT_SIZE = 2048
            private const val DEFAULT_BATCH_SIZE = 4096

            private var isLoaded = false

            fun load(
                ggmlbase: String,
                ggmlrpc: String,
                ggmlcpu: String,
                ggmlcuda: String,
                ggml: String,
                llama: String,
                libttt: String,
            ) = runCatching {
                System.load(ggmlbase)
                System.load(ggmlrpc)
                System.load(ggmlcpu)
                System.load(ggmlcuda)
                System.load(ggml)
                System.load(llama)
                System.load(libttt)
            }.onSuccess {
                isLoaded = true
            }

            fun create(
                modelPath: String,
                basePrompt: String,
                contextSize: Int = DEFAULT_CONTEXT_SIZE,
                batchSize: Int = DEFAULT_BATCH_SIZE,
            ): Result<Llama> = runCatching {
                check(isLoaded) { "Native binaries were not loaded" }

                LlamaTextToText(
                    nativeLlamaTextToText = NativeLlamaTextToText(
                        modelPath = modelPath,
                        contextSize = contextSize,
                        batchSize = batchSize
                    ),
                    basePrompt = basePrompt
                )
            }
        }

        val messages: List<LlamaMessage>

        fun generate(prompt: String): Result<Pair<LlamaMessage, LlamaMessage>>

        fun reset(): Result<Unit>
    }
}