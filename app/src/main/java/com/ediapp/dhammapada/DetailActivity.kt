package com.ediapp.dhammapada

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ediapp.dhammapada.data.DhammapadaItem
import com.ediapp.dhammapada.ui.theme.MyKeywordTheme
import java.util.Locale

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

    val sharedPref = remember { context.getSharedPreferences("DhammapadaPrefs", Context.MODE_PRIVATE) }
    val useTts = sharedPref.getBoolean("use_tts", false)
    val useWriting = sharedPref.getBoolean("use_writing", false)

    LaunchedEffect(itemId) {
        item = dbHelper.getItemById(itemId)
        userInput = item?.myContent ?: ""
        isWritingVisible = useWriting
    }

    LaunchedEffect(item, userInput) {
        item?.let {
            accuracy = calculateAccuracy(it.content, userInput)
            charCount = userInput.length
            if (accuracy >= 60) {
                isThresholdMet = true
            }
        }
    }

    DisposableEffect(Unit) {
        if (useTts) {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.KOREAN
                }
            }
        }
        onDispose {
            if (item != null && isThresholdMet) {
                Log.d("updateWriteDate", userInput)
                dbHelper.updateWriteDate(item!!.id, userInput, accuracy)
            }
            tts?.stop()
            tts?.shutdown()
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
                },
                actions = {
                    IconButton(onClick = {
                        val shareText = "\uD83D\uDE4F ${item?.title}\n\n${item?.content}"
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, item?.title)
                        context.startActivity(shareIntent)
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "공유")
                    }

                    if (useTts) {
                        IconButton(onClick = {
                            item?.let {
                                tts?.speak(
                                    it.content,
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    null
                                )
                            }
                        }) {
                            Icon(
                                painterResource(id = R.drawable.say),
                                tint = Color.Unspecified,
                                contentDescription = "듣기"
                            )
                        }
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
                Text(
                    text = buildAnnotatedString {
                        val originalText = item!!.content
                        val typedText = userInput

                        val cleanedOriginal = originalText.replace(Regex("[\\s\\p{Punct}]"), "").lowercase()
                        val cleanedTyped = typedText.replace(Regex("[\\s\\p{Punct}]"), "").lowercase()

                        var commonPrefixLength = 0
                        while (commonPrefixLength < cleanedOriginal.length &&
                            commonPrefixLength < cleanedTyped.length &&
                            cleanedOriginal[commonPrefixLength] == cleanedTyped[commonPrefixLength]) {
                            commonPrefixLength++
                        }

                        var splitIndex = 0
                        if (commonPrefixLength > 0) {
                            var nonPunctCount = 0
                            for (j in originalText.indices) {
                                if (!originalText[j].toString().matches(Regex("[\\s\\p{Punct}]"))) {
                                    nonPunctCount++
                                }
                                if (nonPunctCount == commonPrefixLength) {
                                    splitIndex = j + 1
                                    break
                                }
                            }
                        }

                        append(originalText.substring(0, splitIndex))

                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(originalText.substring(splitIndex))
                        }
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (isWritingVisible) {
                    LaunchedEffect(isWritingVisible) {
                        if(isWritingVisible) focusRequester.requestFocus()
                    }
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it },
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

    val cleanedOriginal = original.replace(Regex("[\\s\\p{Punct}]"), "").lowercase()
    val cleanedTyped = typed.replace(Regex("[\\s\\p{Punct}]"), "").lowercase()

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
