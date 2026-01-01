package com.ediapp.dhammapada.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ediapp.dhammapada.DatabaseHelper
import com.ediapp.dhammapada.data.DhammapadaItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ListFragment(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    var itemList by remember { mutableStateOf(emptyList<DhammapadaItem>()) }

    LaunchedEffect(Unit) {
        itemList = dbHelper.getAllLists()
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(itemList) {
            item -> ListItem(item = item)
        }
    }
}

@Composable
fun ListItem(item: DhammapadaItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = item.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Row(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = if (item.readTime > 0) {
                        SimpleDateFormat("yyyy.MM.dd (E)", Locale.getDefault()).format(Date(item.readTime))
                    } else {
                        "읽지 않음"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "읽은 횟수: ${item.readCount}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
