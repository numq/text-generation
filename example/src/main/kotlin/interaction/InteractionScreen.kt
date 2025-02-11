package interaction

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import com.github.numq.textgeneration.TextGeneration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import message.ChatMessage
import message.ChatMessageItem
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun InteractionScreen(textGeneration: TextGeneration.Llama, handleThrowable: (Throwable) -> Unit) {
    val generationScope = rememberCoroutineScope { Dispatchers.Default }

    var generationJob by remember { mutableStateOf<Job?>(null) }

    val (prompt, setPrompt) = remember { mutableStateOf("") }

    val chatMessages = remember { mutableStateListOf<ChatMessage>() }

    val listState = rememberLazyListState()

    var isGenerating by remember { mutableStateOf(false) }

    var requestCancellation by remember { mutableStateOf(false) }

    fun sendChatMessage(prompt: String) {
        generationJob = generationScope.launch {
            isGenerating = true

            textGeneration.generate(prompt = prompt).onSuccess { (userMessage, assistantMessage) ->
                chatMessages.add(
                    ChatMessage.User(
                        id = System.nanoTime(),
                        message = userMessage,
                        timestamp = System.currentTimeMillis().milliseconds
                    )
                )

                setPrompt("")

                chatMessages.add(
                    ChatMessage.Assistant(
                        id = System.nanoTime(),
                        message = assistantMessage,
                        timestamp = System.currentTimeMillis().milliseconds
                    )
                )
            }.onFailure(handleThrowable)

            generationJob = null

            isGenerating = false
        }
    }

    LaunchedEffect(requestCancellation) {
        if (requestCancellation) {
            generationJob?.cancelAndJoin()
            generationJob = null

            isGenerating = false

            requestCancellation = false
        }
    }

    LaunchedEffect(chatMessages.size) {
        listState.animateScrollToItem(listState.layoutInfo.totalItemsCount)
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.CenterVertically)
        ) {
            LazyColumn(modifier = Modifier.weight(1f), state = listState) {
                items(chatMessages, key = { it.id }) { message ->
                    ChatMessageItem(message)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = prompt,
                    onValueChange = setPrompt,
                    modifier = Modifier.weight(1f).onKeyEvent { keyEvent ->
                        if (keyEvent.key == Key.Enter) {
                            sendChatMessage(prompt)

                            return@onKeyEvent true
                        }

                        false
                    },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            setPrompt("")
                        }, enabled = prompt.isNotBlank()) {
                            Icon(Icons.Default.Clear, null)
                        }
                    },
                    enabled = !isGenerating
                )
                Box(modifier = Modifier.padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                    when {
                        isGenerating -> IconButton(onClick = {
                            requestCancellation = true
                        }) {
                            Icon(Icons.Default.Cancel, null)
                        }

                        else -> IconButton(onClick = {
                            sendChatMessage(prompt)
                        }, enabled = prompt.isNotBlank()) {
                            Icon(Icons.AutoMirrored.Filled.Send, null)
                        }
                    }
                }
            }
        }
    }
}