package com.github.numq.textgeneration

import com.github.numq.textgeneration.llama.LlamaExchange
import com.github.numq.textgeneration.llama.LlamaMessage
import com.github.numq.textgeneration.llama.LlamaTextGeneration
import com.github.numq.textgeneration.llama.NativeLlamaTextGeneration

interface TextGeneration : AutoCloseable {
    interface Llama : TextGeneration {
        companion object {
            private const val DEFAULT_CONTEXT_SIZE = 2048
            private const val DEFAULT_BATCH_SIZE = 4096

            private sealed interface LoadState {
                data object Unloaded : LoadState

                data object CPU : LoadState

                data object CUDA : LoadState
            }

            @Volatile
            private var loadState: LoadState = LoadState.Unloaded

            fun loadCPU(
                ggmlBase: String,
                ggmlCpu: String,
                ggmlRpc: String,
                ggml: String,
                llama: String,
                textGeneration: String,
            ) = runCatching {
                check(loadState is LoadState.Unloaded) { "Native binaries have already been loaded as ${loadState::class.simpleName}" }

                System.load(ggmlBase)
                System.load(ggmlCpu)
                System.load(ggmlRpc)
                System.load(ggml)
                System.load(llama)
                System.load(textGeneration)

                loadState = LoadState.CPU
            }

            fun loadCUDA(
                ggmlBase: String,
                ggmlCpu: String,
                ggmlCuda: String,
                ggmlRpc: String,
                ggml: String,
                llama: String,
                textGeneration: String,
            ) = runCatching {
                check(loadState is LoadState.Unloaded) { "Native binaries have already been loaded as ${loadState::class.simpleName}" }

                System.load(ggmlBase)
                System.load(ggmlCpu)
                System.load(ggmlCuda)
                System.load(ggmlRpc)
                System.load(ggml)
                System.load(llama)
                System.load(textGeneration)

                loadState = LoadState.CUDA
            }

            fun create(
                modelPath: String,
                basePrompt: String = "",
                contextSize: Int = DEFAULT_CONTEXT_SIZE,
                batchSize: Int = DEFAULT_BATCH_SIZE,
            ): Result<Llama> = runCatching {
                check(loadState !is LoadState.Unloaded) { "Native binaries were not loaded" }

                LlamaTextGeneration(
                    nativeLlamaTextGeneration = NativeLlamaTextGeneration(
                        modelPath = modelPath,
                        contextSize = contextSize,
                        batchSize = batchSize
                    ),
                    basePrompt = basePrompt
                )
            }
        }

        suspend fun history(): Result<List<LlamaMessage>>

        suspend fun generate(prompt: String): Result<LlamaExchange>

        suspend fun reset(): Result<Unit>

        override fun close() {
            loadState = LoadState.Unloaded
        }
    }
}