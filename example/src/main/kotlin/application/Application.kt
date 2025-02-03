package application

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import com.github.numq.ttt.TextToText
import interaction.InteractionScreen

const val APP_NAME = "Text-To-Text"

fun main(args: Array<String>) {
    val modelPath = args.first()

    val pathToBinaries = Thread.currentThread().contextClassLoader.getResource("bin")?.file

    checkNotNull(pathToBinaries) { "Binaries not found" }

    TextToText.Llama.load(
        ggmlbase = "$pathToBinaries\\ggml-base.dll",
        ggmlrpc = "$pathToBinaries\\ggml-rpc.dll",
        ggmlcpu = "$pathToBinaries\\ggml-cpu.dll",
        ggmlcuda = "$pathToBinaries\\ggml-cuda.dll",
        ggml = "$pathToBinaries\\ggml.dll",
        llama = "$pathToBinaries\\llama.dll",
        libttt = "$pathToBinaries\\libttt.dll"
    ).getOrThrow()

    singleWindowApplication(state = WindowState(width = 512.dp, height = 512.dp), title = APP_NAME) {
        val textToText = remember { TextToText.Llama.create(modelPath = modelPath).getOrThrow() }

        val (throwable, setThrowable) = remember { mutableStateOf<Throwable?>(null) }

        DisposableEffect(Unit) {
            onDispose {
                textToText.close()
            }
        }

        MaterialTheme {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                InteractionScreen(
                    textToText = textToText,
                    handleThrowable = setThrowable
                )
                throwable?.let { t ->
                    Snackbar(
                        modifier = Modifier.padding(8.dp),
                        action = {
                            Button(onClick = { setThrowable(null) }) { Text("Dismiss") }
                        }
                    ) { Text(t.localizedMessage) }
                }
            }
        }
    }
}