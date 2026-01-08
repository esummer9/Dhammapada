package com.ediapp.dhammapada

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ediapp.dhammapada.data.DhammapadaItem
import com.ediapp.dhammapada.ui.theme.MyKeywordTheme

class App : Application() {
    companion object {
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
class DetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val itemId = intent.getLongExtra("item_id", -1)

        setContent {
            MyKeywordTheme {
                DetailScreen(itemId = itemId)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(itemId: Long) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(App.instance) }
    var item by remember { mutableStateOf<DhammapadaItem?>(null) }
    var userInput by remember { mutableStateOf("") }
    var charCount by remember { mutableStateOf(0) }
    var accuracy by remember { mutableStateOf(0.0) }
    var isThresholdMet by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    val focusRequester = remember { FocusRequester() }
    var isWritingVisible by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        item = dbHelper.getItemById(itemId)
        if (item!!.myContent == null)
            userInput = ""
        else
            userInput = item!!.myContent.toString()
    }

    DisposableEffect(Unit) {
        onDispose {
            if (item != null && isThresholdMet) {
                Log.d("updateWriteDate", userInput)
                dbHelper.updateWriteDate(item!!.id, userInput)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("내손안의 법구경") },
                navigationIcon = {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (item != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {

                Text(text = item!!.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
                Text(text = item!!.content, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 공유 버튼
                    Button(onClick = {
                        val shareText = "\uD83D\uDE4F ${item!!.title}\n\n${item!!.content}"
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, item!!.title)
                        context.startActivity(shareIntent)
                    }) {
                        Text("공유")
                    }

                    // 듣기 버튼
                    Button(onClick = {
                        tts?.speak(item!!.content, TextToSpeech.QUEUE_FLUSH, null, null)
                    }) {
                        Text("듣기")
                    }

                    // 쓰기 버튼
                    Button(onClick = {
                        isWritingVisible = !isWritingVisible
                    }) {
                        Text("쓰기")
                    }
                }

                if (isWritingVisible) {
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = {
                            userInput = it
                            charCount = it.length
                            accuracy = calculateAccuracy(item!!.content, it)
                            if (accuracy >= 0) {
                                isThresholdMet = true
                            }
                        },
                        label = { Text("내용을 따라 입력하세요") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        minLines = 5
                    )

                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        Text(text = "글자 수: $charCount")
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = "일치율: ${String.format("%.2f", accuracy)}%")
                    }
                }
            }
        }
    }
}

fun calculateAccuracy(original: String, typed: String): Double {
    if (original.isEmpty() || typed.isEmpty()) {
        return 0.0
    }

    val cleanedOriginal = original.replace(Regex("[\\s\\p{Punct}]"), "")
    val cleanedTyped = typed.replace(Regex("[\\s\\p{Punct}]"), "")

    if (cleanedOriginal.isEmpty() || cleanedTyped.isEmpty()) {
        return 0.0
    }

    val maxLength = maxOf(cleanedOriginal.length, cleanedTyped.length)
    var correctChars = 0
    for (i in 0 until minOf(cleanedOriginal.length, cleanedTyped.length)) {
        if (cleanedOriginal[i] == cleanedTyped[i]) {
            correctChars++
        }
    }
    return (correctChars.toDouble() / maxLength) * 100
}