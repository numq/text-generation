import com.github.numq.ttt.TextToText
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import kotlin.test.Test
import kotlin.test.assertTrue

class TextToTextTest {
    companion object {
        private val modelPath = this::class.java.getResource(
            "model/tinyllama-1.1b-chat-v1.0.Q5_K_M.gguf"
        )?.path?.trimStart('/')!!

        private val llama by lazy { TextToText.Llama.create(modelPath = modelPath).getOrThrow() }

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            val pathToBinaries = this::class.java.getResource("bin")?.file

            TextToText.Llama.load(
                ggmlbase = "$pathToBinaries\\ggml-base.dll",
                ggmlrpc = "$pathToBinaries\\ggml-rpc.dll",
                ggmlcpu = "$pathToBinaries\\ggml-cpu.dll",
                ggmlcuda = "$pathToBinaries\\ggml-cuda.dll",
                ggml = "$pathToBinaries\\ggml.dll",
                llama = "$pathToBinaries\\llama.dll",
                libttt = "$pathToBinaries\\libttt.dll"
            ).getOrThrow()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            llama.close()
        }
    }

    @Test
    fun `should return non-blank output string`() {
        val result = llama.generate("What is Python?").getOrThrow()

        assertTrue(result.contains("programming language"))
    }
}