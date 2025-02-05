package message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatMessageItem(chatMessage: ChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        horizontalAlignment = if (chatMessage is ChatMessage.User) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(space = 4.dp, alignment = Alignment.CenterVertically)
    ) {
        Card {
            Column(
                modifier = Modifier.padding(4.dp),
                horizontalAlignment = if (chatMessage is ChatMessage.Assistant) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.CenterVertically)
            ) {
                Text(if (chatMessage is ChatMessage.User) "User" else "Llama")
                Divider()
                Text(chatMessage.message.content)
            }
        }
        Text(SimpleDateFormat.getInstance().format(Date(chatMessage.timestamp.inWholeMilliseconds)))
    }
}