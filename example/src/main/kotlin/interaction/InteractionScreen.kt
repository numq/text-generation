package interaction

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import playback.PlaybackService
import playback.PlaybackState
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

@Composable
fun InteractionScreen(tts: TTS, playbackService: PlaybackService, handleThrowable: (Throwable) -> Unit) {
    var state by remember { mutableStateOf<PlaybackState>(PlaybackState.Empty) }

    var playbackJob by remember { mutableStateOf<Job?>(null) }

    suspend fun startPlaying(pcmBytes: ByteArray, targetFormat: AudioFormat) = runCatching {
        if (pcmBytes.isEmpty()) return@runCatching

        val inputStream = pcmBytes.inputStream()

        val format = AudioFormat(24_000f, 16, 1, true, false)

        val audioInputStream = AudioSystem.getAudioInputStream(
            targetFormat, AudioInputStream(
                inputStream,
                format,
                pcmBytes.size.toLong()
            )
        )

        playbackService.start(format).mapCatching {
            try {
                val buffer = ByteArray(4096)
                while (currentCoroutineContext().isActive) {
                    val bytesRead = audioInputStream.read(buffer)
                    if (bytesRead <= 0) break
                    playbackService.play(buffer.copyOf(bytesRead))
                }
            } catch (e: Exception) {
                println("Error during playback: ${e.localizedMessage}")
            } finally {
                playbackService.stop()
            }
        }.getOrThrow()
    }

    LaunchedEffect(state) {
        withContext(Dispatchers.Default) {
            playbackJob?.cancel()
            playbackJob = when (state) {
                is PlaybackState.Generated.Playing -> launch {
                    playbackService.stop().onFailure(handleThrowable)

                    val pcmBytes = (state as PlaybackState.Generated.Playing).pcmBytes

                    withContext(Dispatchers.IO) {
                        startPlaying(pcmBytes, AudioFormat(24_000f, 16, 1, true, false)).onFailure(handleThrowable)
                    }

                    playbackJob?.invokeOnCompletion {
                        state = PlaybackState.Generated.Stopped(pcmBytes)
                    }
                }

                else -> null
            }
        }
    }

    var generationJob by remember { mutableStateOf<Job?>(null) }

    val (text, setText) = remember { mutableStateOf("") }

    LaunchedEffect(text) {
        if (text.isNotBlank()) {
            generationJob?.cancel()
            generationJob = launch(Dispatchers.Default) {
                delay(500L)

                tts.generate(text = text, threads = 8).onSuccess { pcmBytes ->
//                    pcmBytes.inputStream().use { inputStream ->
//                        AudioSystem.write(
//                            AudioInputStream(
//                                inputStream,
//                                AudioFormat(24_000f, 16, 1, true, false),
//                                pcmBytes.size.toLong()
//                            ),
//                            AudioFileFormat.Type.WAVE,
//                            File("generated-${System.currentTimeMillis()}.wav")
//                        )
//                    }
                    state = PlaybackState.Generated.Playing(pcmBytes = pcmBytes)
                }.onFailure(handleThrowable).getOrThrow()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.CenterVertically)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            when (val currentState = state) {
                is PlaybackState.Empty -> Unit

                is PlaybackState.Generated -> when (currentState) {
                    is PlaybackState.Generated.Stopped -> IconButton(onClick = {
                        state = PlaybackState.Generated.Playing(
                            pcmBytes = (state as PlaybackState.Generated.Stopped).pcmBytes
                        )
                    }) {
                        Icon(Icons.Default.PlayCircle, null)
                    }

                    is PlaybackState.Generated.Playing -> IconButton(onClick = {
                        state = PlaybackState.Generated.Stopped(
                            pcmBytes = (state as PlaybackState.Generated.Playing).pcmBytes
                        )
                    }) {
                        Icon(Icons.Default.StopCircle, null)
                    }
                }
            }
        }
//        Column(modifier = Modifier.fillMaxWidth()) {
//            TextField(value = sampleRate.toString(), onValueChange = { value ->
//                sampleRate = value.toFloatOrNull() ?: 41_000f
//            })
//            TextField(value = sampleSizeInBits.toString(), onValueChange = { value ->
//                sampleSizeInBits = value.toIntOrNull() ?: 16
//            })
//            TextField(value = channels.toString(), onValueChange = { value ->
//                channels = value.toIntOrNull() ?: 2
//            })
//            Checkbox(checked = signed, onCheckedChange = {
//                signed = it
//            })
//            Checkbox(checked = bigEndian, onCheckedChange = {
//                bigEndian = it
//            })
//        }
        TextField(value = text,
            onValueChange = setText,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = {
                    setText("")
                }, enabled = text.isNotBlank()) {
                    Icon(Icons.Default.Clear, null)
                }
            })
    }
}