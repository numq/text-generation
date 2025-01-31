package com.github.numq.ttt.llama

import com.github.numq.ttt.TextToText

internal class LlamaTextToText(private val nativeLlamaTextToText: NativeLlamaTextToText) : TextToText.Llama {
    override fun generate(prompt: String) = runCatching {
        nativeLlamaTextToText.generate(prompt = prompt).trim()
    }

    override fun close() = runCatching { nativeLlamaTextToText.close() }.getOrDefault(Unit)
}