import com.github.numq.textgeneration.TextGeneration
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import kotlin.test.Test
import kotlin.test.assertTrue

class TextGenerationTest {
    companion object {
        private val modelPath = this::class.java.getResource(
            "model/tinyllama-1.1b-chat-v1.0.Q5_K_M.gguf"
        )?.path?.trimStart('/')!!

        private val llama by lazy { TextGeneration.Llama.create(modelPath = modelPath).getOrThrow() }

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            val pathToBinaries = this::class.java.getResource("bin")?.file

            TextGeneration.Llama.loadCUDA(
                ggmlBase = "$pathToBinaries\\ggml-base.dll",
                ggmlCpu = "$pathToBinaries\\ggml-cpu.dll",
                ggmlCuda = "$pathToBinaries\\ggml-cuda.dll",
                ggmlRpc = "$pathToBinaries\\ggml-rpc.dll",
                ggml = "$pathToBinaries\\ggml.dll",
                llama = "$pathToBinaries\\llama.dll",
                textGeneration = "$pathToBinaries\\text-generation.dll"
            ).getOrThrow()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            llama.close()
        }
    }

    @Test
    fun `should return non-blank output string`() = runTest {
        val result = llama.generate("What is Python?").getOrThrow()

        assertTrue(result.output.content.contains("programming language"))
    }
}