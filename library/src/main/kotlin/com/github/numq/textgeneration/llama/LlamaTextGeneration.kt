package com.github.numq.textgeneration.llama

import com.github.numq.textgeneration.TextGeneration
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class LlamaTextGeneration(
    private val nativeLlamaTextGeneration: NativeLlamaTextGeneration,
    basePrompt: String,
) : TextGeneration.Llama {
    private val mutex = Mutex()

    private val systemMessage = LlamaMessage.System(basePrompt.trim())

    private val messages = mutableListOf<LlamaMessage>(systemMessage)

    override suspend fun history() = mutex.withLock { Result.success(messages.toList()) }

    override suspend fun generate(prompt: String) = mutex.withLock {
        runCatching {
            val userMessage = LlamaMessage.Input(content = prompt.trim())

            messages.add(userMessage)

            val response = nativeLlamaTextGeneration.generate(messages = messages.map { message ->
                NativeLlamaMessage(role = message.role.name.lowercase(), content = message.content)
            }.toTypedArray())

            val assistantMessage = LlamaMessage.Output(content = response.trim())

            messages.add(assistantMessage)

            LlamaExchange(input = userMessage, output = assistantMessage)
        }
    }

    override suspend fun reset() = mutex.withLock {
        runCatching {
            messages.clear()

            messages.add(systemMessage)

            Unit
        }
    }

    override fun close() = runCatching {
        super.close()

        nativeLlamaTextGeneration.close()
    }.getOrDefault(Unit)
}