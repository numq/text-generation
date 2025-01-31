package generation

import kotlin.time.Duration

sealed interface Message {
    val id: Long
    val text: String
    val timestamp: Duration

    data class Request(override val id: Long, override val text: String, override val timestamp: Duration) : Message

    data class Response(override val id: Long, override val text: String, override val timestamp: Duration) : Message
}