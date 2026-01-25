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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.unit.sp
import com.ediapp.dhammapada.data.DhammapadaItem
import com.ediapp.dhammapada.ui.theme.MyKeywordTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

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

    var isBookmarked by remember { mutableStateOf(false) }

    val sharedPref = remember { context.getSharedPreferences("DhammapadaPrefs", Context.MODE_PRIVATE) }
    val useTts = sharedPref.getBoolean("use_tts", false)
    val useWriting = sharedPref.getBoolean("use_writing", true)
    val fontSizeLarge = sharedPref.getBoolean("font_size_large", false)

    var showAnimationForAccuracy by remember { mutableStateOf<Int?>(null) }
    var hasAnimated50 by remember { mutableStateOf(false) }
    var hasAnimated90 by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        item = dbHelper.getItemById(itemId)
        item?.let {
            userInput = it.myContent ?: ""
            isBookmarked = it.bookmark == 1
        }
        isWritingVisible = useWriting
    }

    LaunchedEffect(item, userInput) {
        item?.let {
            accuracy = calculateAccuracy(it.content, userInput)
            charCount = userInput.length
            if (accuracy >= 99 && !hasAnimated90) {
                hasAnimated90 = true
                showAnimationForAccuracy = 90
            } else if (accuracy >= 60 && !isThresholdMet) {
                isThresholdMet = true
            } else if (accuracy >= 50 && !hasAnimated50) {
                hasAnimated50 = true
                showAnimationForAccuracy = 50
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

    val titleTextStyle = if (fontSizeLarge) MaterialTheme.typography.titleLarge.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize * 1.3f) else MaterialTheme.typography.titleLarge
    val contentTextStyle = if (fontSizeLarge) MaterialTheme.typography.bodyLarge.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize * 1.3f) else MaterialTheme.typography.bodyLarge

    Box(modifier = Modifier.fillMaxSize()) {
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
                            isBookmarked = !isBookmarked
                            dbHelper.updateBookmarkStatus(itemId, isBookmarked)
                        }) {
                            Icon(
                                painter = painterResource(if (isBookmarked) R.drawable.bookmark else R.drawable.bookmark_border),
                                contentDescription = "북마크",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                        IconButton(onClick = {
                            val shareText = "\uD83D\uDE4F ${item?.title}\n${item?.content}"
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
                    Text(text = item!!.title, style = titleTextStyle, modifier = Modifier.padding(bottom = 8.dp))
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
                        style = contentTextStyle,
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
                            Text(text = "일치율: ${String.format("%.1f", accuracy)}%")
                        }
                    }
                }
            }
        }

        if (showAnimationForAccuracy != null) {
            val (targetSize, duration) = when (showAnimationForAccuracy) {
                50 -> 40f to 2000L
                90 -> 90f to 4000L
                else -> 10f to 1000L
            }

            val animSize = remember { Animatable(10f) }
            val animAlpha = remember { Animatable(1f) }

            LaunchedEffect(showAnimationForAccuracy) {
                launch {
                    animSize.animateTo(targetSize, tween(durationMillis = duration.toInt()))
                    delay(300)
                    animAlpha.animateTo(0f, tween(500))
                    showAnimationForAccuracy = null
                    animSize.snapTo(10f)
                    animAlpha.snapTo(1f)
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "\uD83D\uDE4F",
                    fontSize = animSize.value.sp,
                    modifier = Modifier.alpha(animAlpha.value)
                )
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