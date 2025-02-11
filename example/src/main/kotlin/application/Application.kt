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
import com.github.numq.textgeneration.TextGeneration
import interaction.InteractionScreen

const val APP_NAME = "Text generation"

fun main(args: Array<String>) {
    val modelPath = args.first()

    val pathToBinaries = Thread.currentThread().contextClassLoader.getResource("bin")?.file

    checkNotNull(pathToBinaries) { "Binaries not found" }

    TextGeneration.Llama.loadCUDA(
        ggmlBase = "$pathToBinaries\\ggml-base.dll",
        ggmlCpu = "$pathToBinaries\\ggml-cpu.dll",
        ggmlCuda = "$pathToBinaries\\ggml-cuda.dll",
        ggmlRpc = "$pathToBinaries\\ggml-rpc.dll",
        ggml = "$pathToBinaries\\ggml.dll",
        llama = "$pathToBinaries\\llama.dll",
        textGeneration = "$pathToBinaries\\text-generation.dll"
    ).getOrThrow()

    singleWindowApplication(state = WindowState(width = 512.dp, height = 512.dp), title = APP_NAME) {
        val textGeneration = remember { TextGeneration.Llama.create(modelPath = modelPath).getOrThrow() }

        val (throwable, setThrowable) = remember { mutableStateOf<Throwable?>(null) }

        DisposableEffect(Unit) {
            onDispose {
                textGeneration.close()
            }
        }

        MaterialTheme {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                InteractionScreen(
                    textGeneration = textGeneration,
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