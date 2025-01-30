package com.github.numq.ttt

import com.github.numq.ttt.llama.LlamaTextToText
import com.github.numq.ttt.llama.NativeLlamaTextToText

interface TextToText : AutoCloseable {
    fun generate(prompt: String): Result<String>

    interface Llama : TextToText {
        companion object {
            private var isLoaded = false

            fun load(
                ggmlbase: String,
                ggmlcpu: String,
                ggmlcuda: String,
                ggmlrpc: String,
                ggml: String,
                llama: String,
                libttt: String,
            ) = runCatching {
                System.load(ggmlbase)
                System.load(ggmlcpu)
                System.load(ggmlcuda)
                System.load(ggmlrpc)
                System.load(ggml)
                System.load(llama)
                System.load(libttt)
            }.onSuccess {
                isLoaded = true
            }

            fun create(modelPath: String): Result<Llama> = runCatching {
                check(isLoaded) { "Native binaries were not loaded" }

                LlamaTextToText(nativeLlamaTextToText = NativeLlamaTextToText(modelPath = modelPath))
            }
        }
    }
}