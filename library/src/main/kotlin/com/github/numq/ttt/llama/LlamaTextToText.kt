package com.github.numq.ttt.llama

import com.github.numq.ttt.TextToText

internal class LlamaTextToText(
    private val nativeLlamaTextToText: NativeLlamaTextToText,
    private val basePrompt: String,
) : TextToText.Llama {
    private val messages = mutableListOf<LlamaMessage>()

    override fun generate(prompt: String) = runCatching {
        val fullPrompt = basePrompt + "\n" + prompt.trim()

        val userMessage = LlamaMessage(role = "User", content = fullPrompt)

        messages.add(userMessage)

        val template = nativeLlamaTextToText.applyTemplate(messages = messages.toTypedArray())

        val response = nativeLlamaTextToText.generate(prompt = template).trim()

        val assistantMessage = LlamaMessage(role = "Assistant", content = response)

        messages.add(assistantMessage)

        return@runCatching response
    }

    override fun reset() = runCatching { messages.clear() }

    override fun close() = runCatching { nativeLlamaTextToText.close() }.getOrDefault(Unit)
}