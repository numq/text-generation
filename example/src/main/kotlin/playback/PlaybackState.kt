package playback

sealed interface PlaybackState {
    data object Empty : PlaybackState

    sealed interface Generated : PlaybackState {
        val pcmBytes: ByteArray

        data class Stopped(override val pcmBytes: ByteArray) : Generated {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Stopped

                return pcmBytes.contentEquals(other.pcmBytes)
            }

            override fun hashCode(): Int {
                return pcmBytes.contentHashCode()
            }
        }

        data class Playing(override val pcmBytes: ByteArray) : Generated {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Playing

                return pcmBytes.contentEquals(other.pcmBytes)
            }

            override fun hashCode(): Int {
                return pcmBytes.contentHashCode()
            }
        }
    }
}