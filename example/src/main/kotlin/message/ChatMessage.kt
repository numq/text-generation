package message

import com.github.numq.ttt.llama.LlamaMessage
import kotlin.time.Duration

sealed interface ChatMessage {
    val id: Long
    val message: LlamaMessage
    val timestamp: Duration

    data class User(
        override val id: Long,
        override val message: LlamaMessage,
        override val timestamp: Duration,
    ) : ChatMessage

    data class Assistant(
        override val id: Long,
        override val message: LlamaMessage,
        override val timestamp: Duration,
    ) : ChatMessage
}