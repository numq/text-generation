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
fun MessageItem(message: Message) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        horizontalAlignment = if (message is Message.Request) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(space = 4.dp, alignment = Alignment.CenterVertically)
    ) {
        Card {
            Column(
                modifier = Modifier.padding(4.dp),
                horizontalAlignment = if (message is Message.Request) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.CenterVertically)
            ) {
                Text(if (message is Message.Request) "User" else "Llama")
                Divider()
                Text(message.text)
            }
        }
        Text(SimpleDateFormat.getInstance().format(Date(message.timestamp.inWholeMilliseconds)))
    }
}