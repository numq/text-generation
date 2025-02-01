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
import com.github.numq.ttt.TextToText
import message.Message
import message.MessageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun InteractionScreen(textToText: TextToText, handleThrowable: (Throwable) -> Unit) {
    val generationScope = rememberCoroutineScope { Dispatchers.Default }

    var generationJob by remember { mutableStateOf<Job?>(null) }

    val (prompt, setPrompt) = remember { mutableStateOf("") }

    val messages = remember { mutableStateListOf<Message>() }

    val listState = rememberLazyListState()

    var isGenerating by remember { mutableStateOf(false) }

    var requestCancellation by remember { mutableStateOf(false) }

    val sendMessage: (String) -> Unit = remember {
        { prompt ->
            generationJob = generationScope.launch {
                isGenerating = true

                textToText.generate(prompt = prompt).onSuccess { result ->
                    messages.add(
                        Message.Request(
                            id = System.nanoTime(),
                            text = prompt,
                            timestamp = System.currentTimeMillis().milliseconds
                        )
                    )

                    setPrompt("")

                    messages.add(
                        Message.Response(
                            id = System.nanoTime(),
                            text = result,
                            timestamp = System.currentTimeMillis().milliseconds
                        )
                    )

                    generationJob = null
                }.onFailure(handleThrowable)

                isGenerating = false
            }
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

    LaunchedEffect(messages.size) {
        listState.animateScrollToItem(listState.layoutInfo.totalItemsCount)
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.CenterVertically)
        ) {
            LazyColumn(modifier = Modifier.weight(1f), state = listState) {
                items(messages, key = { it.id }) { message ->
                    MessageItem(message)
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
                            sendMessage(prompt)

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
                when {
                    isGenerating -> IconButton(onClick = {
                        requestCancellation = true
                    }) {
                        Icon(Icons.Default.Cancel, null)
                    }

                    else -> IconButton(onClick = {
                        sendMessage(prompt)
                    }, enabled = prompt.isNotBlank()) {
                        Icon(Icons.AutoMirrored.Filled.Send, null)
                    }
                }
            }
        }
    }
}