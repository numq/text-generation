package com.github.numq.textgeneration.llama

sealed class LlamaMessage private constructor(val role: LlamaRole, open val content: String) {
    data class System(override val content: String) : LlamaMessage(role = LlamaRole.SYSTEM, content = content)

    data class Input(override val content: String) : LlamaMessage(role = LlamaRole.USER, content = content)

    data class Output(override val content: String) : LlamaMessage(role = LlamaRole.ASSISTANT, content = content)
}