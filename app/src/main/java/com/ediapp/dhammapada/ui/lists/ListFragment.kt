package com.ediapp.dhammapada.ui.lists

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ediapp.dhammapada.DatabaseHelper
import com.ediapp.dhammapada.DetailActivity
import com.ediapp.dhammapada.data.DhammapadaItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ListFragment(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    var itemList by remember { mutableStateOf(emptyList<DhammapadaItem>()) }
    var searchQuery by remember { mutableStateOf("") }
    var filterOption by remember { mutableStateOf("사경무") }
    var refreshKey by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshKey++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(searchQuery, refreshKey, filterOption) {
        Log.d("ListFragment", "searchQuery: $searchQuery, refreshKey: $refreshKey, filterOption: $filterOption")

        val allItems = if (searchQuery.isBlank()) {
            dbHelper.getAllLists()
        } else {
            dbHelper.searchLists(searchQuery)
        }

        itemList = when (filterOption) {
            "사경" -> allItems.filter { it.writeDate > 0 }
            "사경무" -> allItems.filter { it.writeDate <= 0 }
            else -> allItems // "전체"
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("검색") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        val radioOptions = listOf("사경무", "사경", "전체")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            radioOptions.forEach { option ->
                Row(
                    Modifier
                        .selectable(
                            selected = (option == filterOption),
                            onClick = { filterOption = option },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (option == filterOption),
                        onClick = null // Recommended for accessibility
                    )
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        LazyColumn {
            items(itemList) { item ->
                ListItem(item = item, onClick = {
                    val intent = Intent(context, DetailActivity::class.java)
                    intent.putExtra("item_id", item.id)
                    context.startActivity(intent)
                })
            }
        }
    }
}

@Composable
fun ListItem(item: DhammapadaItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
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
                    text = "읽은 횟수: ${item.readCount}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = if (item.writeDate > 0) {
                        "쓰기:" + SimpleDateFormat("yyyy.MM.dd (E)", Locale.getDefault()).format(Date(item.writeDate))
                    } else {
                        "쓰지 않음"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = if (item.writeDate > 0) {
                        Modifier
                            .background(
                                color = Color.White,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(5.dp)
                    } else {
                        Modifier
                    }
                )
            }
        }
    }
}
