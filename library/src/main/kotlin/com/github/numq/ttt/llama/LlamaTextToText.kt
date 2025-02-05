package com.github.numq.ttt.llama

import com.github.numq.ttt.TextToText

internal class LlamaTextToText(
    private val nativeLlamaTextToText: NativeLlamaTextToText,
    basePrompt: String,
) : TextToText.Llama {
    private val systemMessage = LlamaMessage(role = "system", content = basePrompt.trim())

    override val messages = mutableListOf(systemMessage)

    override fun generate(prompt: String) = runCatching {
        val userMessage = LlamaMessage(role = "user", content = prompt.trim())

        messages.add(userMessage)

        val response = nativeLlamaTextToText.generate(messages = messages.toTypedArray()).trim()

        val assistantMessage = LlamaMessage(role = "assistant", content = response)

        messages.add(assistantMessage)

        return@runCatching userMessage to assistantMessage
    }

    override fun reset() = runCatching { messages.clear() }

    override fun close() = runCatching { nativeLlamaTextToText.close() }.getOrDefault(Unit)
}