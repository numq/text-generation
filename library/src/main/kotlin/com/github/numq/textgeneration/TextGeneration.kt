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

            /**
             * Loads the CPU-based native libraries required for Whisper speech recognition.
             *
             * This method must be called before creating a Whisper instance.
             *
             * @param ggmlBase The path to the `ggml-base` binary.
             * @param ggmlCpu The path to the `ggml-cpu` binary.
             * @param ggmlRpc The path to the `ggml-rpc` binary.
             * @param ggml The path to the `ggml` binary.
             * @param llama The path to the `llama` binary.
             * @param textGeneration The path to the `text-generation` binary.
             * @return A [Result] indicating the success or failure of the operation.
             */
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

            /**
             * Loads the CUDA-based native libraries required for Whisper speech recognition.
             *
             * This method must be called before creating a Whisper instance.
             *
             * @param ggmlBase The path to the `ggml-base` binary.
             * @param ggmlCpu The path to the `ggml-cpu` binary.
             * @param ggmlCuda The path to the `ggml-cuda` binary.
             * @param ggmlRpc The path to the `ggml-rpc` binary.
             * @param ggml The path to the `ggml` binary.
             * @param llama The path to the `llama` binary.
             * @param textGeneration The path to the `text-generation` binary.
             * @return A [Result] indicating the success or failure of the operation.
             */
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

            /**
             * Creates a new instance of [TextGeneration] using the Whisper implementation.
             *
             * This method initializes the Llama text generation with the specified model.
             *
             * @param modelPath the path to the Llama model file.
             * @param systemPrompt the optional system prompt that will be used as the system message.
             * @param contextSize the size of the context window.
             * @param batchSize the batch size.
             * @return a [Result] containing the created instance if successful.
             * @throws IllegalStateException if the native libraries are not loaded or if there is an issue with the underlying native libraries.
             */
            fun create(
                modelPath: String,
                systemPrompt: String = "",
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
                    systemPrompt = systemPrompt
                )
            }
        }

        /**
         * Retrieves the conversation history.
         *
         * @return A [Result] containing a list of [LlamaMessage] objects representing the conversation history.
         */
        suspend fun history(): Result<List<LlamaMessage>>

        /**
         * Generates a response based on the provided prompt.
         *
         * @param prompt The input text prompt to generate a response from.
         * @return A [Result] containing a [LlamaExchange] object with the generated response.
         */
        suspend fun generate(prompt: String): Result<LlamaExchange>

        /**
         * Resets the conversation history and clears the current context.
         *
         * @return A [Result] indicating the success or failure of the operation.
         */
        suspend fun reset(): Result<Unit>

        override fun close() {
            loadState = LoadState.Unloaded
        }
    }
}