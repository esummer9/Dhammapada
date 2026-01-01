package com.ediapp.dhammapada.ui.home

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ediapp.dhammapada.DatabaseHelper
import com.ediapp.dhammapada.DetailActivity
import com.ediapp.dhammapada.R
import com.ediapp.dhammapada.data.DhammapadaItem
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.OutputStream
import java.util.Locale
import kotlin.math.abs

@Composable
fun HomeFragment(
    modifier: Modifier = Modifier, 
    refreshTrigger: Int, 
    setActions: (@Composable RowScope.() -> Unit) -> Unit
) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val sharedPref = remember { context.getSharedPreferences("DhammapadaPrefs", Context.MODE_PRIVATE) }
    var item by remember { mutableStateOf<DhammapadaItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val useTts = sharedPref.getBoolean("use_tts", false)
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    val buddaImageIds = remember {
        listOf(
            R.drawable.budda_korea_951111_1280,
            R.drawable.budda_neck_4331959_1280,
            R.drawable.budda_icicle_3043112_1280,
            R.drawable.budda_nature_7160839_1280,
            R.drawable.budda_buddhism_882879_1280,
            R.drawable.budda_lotuses_4880755_1280,
            R.drawable.budda_buddhism_5220871_1280,
            R.drawable.budda_img_20250101_122112300,
            R.drawable.buddda_img_0538,
            R.drawable.budda_img_20250101_121911767,
            R.drawable.budda_bulguksa_1604599_1280,
        )
    }
    var imageResId by remember { mutableStateOf(buddaImageIds.random()) }

    // TTS Initialization and Lifecycle
    DisposableEffect(Unit) {
        if (useTts) {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.KOREAN
                }
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                imageResId = buddaImageIds.random()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun loadItem(newItem: DhammapadaItem?) {
        if (newItem != null) {
            item = newItem
            dbHelper.updateReadStatus(newItem.id)
            with(sharedPref.edit()) {
                putInt("read_index", newItem.id.toInt())
                apply()
            }
        }
    }

    fun loadNextItem() {
        coroutineScope.launch {
            imageResId = buddaImageIds.random()
            isLoading = true
            val readIndex = sharedPref.getInt("read_index", 0)
            var nextItem = dbHelper.getNextItem(readIndex)
            if (nextItem == null) { 
                nextItem = dbHelper.getNextItem(0)
            }
            loadItem(nextItem)
            isLoading = false
        }
    }

    fun loadPreviousItem() {
        coroutineScope.launch {
            imageResId = buddaImageIds.random()
            isLoading = true
            val currentId = item?.id ?: sharedPref.getInt("read_index", 0).toLong()
            var prevItem = dbHelper.getPreviousItem(currentId)
            if (prevItem == null) {
                prevItem = dbHelper.getLastItem()
            }
            loadItem(prevItem)
            isLoading = false
        }
    }
    
    LaunchedEffect(item, isLoading, useTts, tts) {
        setActions {
            IconButton(
                onClick = { 
                    if (useTts && tts != null && item != null) {
                        tts?.speak(item!!.content, TextToSpeech.QUEUE_FLUSH, null, null)
                    } else if (useTts) {
                        Toast.makeText(context, "TTS 엔진을 초기화하고 있습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                         Toast.makeText(context, "설정에서 TTS 사용을 활성화해주세요.", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = useTts && !isLoading && item != null
            ) {
                Icon(painterResource(id = R.drawable.say),tint = Color.Unspecified,
                    modifier = Modifier.width(30.dp), contentDescription = "듣기")
            }

            IconButton(onClick = { 
                coroutineScope.launch {
                    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    view.draw(canvas)
                    saveBitmapToGallery(context, bitmap, "Dhammapada_Verse")
                }
            }, enabled = !isLoading && item != null) {
                Icon(painterResource(id = R.drawable.picture),tint = Color.Unspecified,
                     modifier = Modifier.width(30.dp), contentDescription = "저장"
                )
            }
        }
    }

    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            loadNextItem()
        }
    }

    LaunchedEffect(Unit) {
        loadNextItem()
    }

    val scrollState = rememberScrollState()
    var dragDistance by remember { mutableStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount -> 
                        change.consume()
                        dragDistance += dragAmount.x
                    },
                    onDragEnd = {
                        if (abs(dragDistance) > 100) { // Swipe threshold
                            if (dragDistance > 0) { // Swipe right
                                loadPreviousItem()
                            } else { // Swipe left
                                loadNextItem()
                            }
                        }
                        dragDistance = 0f
                    }
                )
            }
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = "Budda Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 350.dp)
                .padding(16.dp)
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else if (item != null) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = item!!.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp).semantics { contentDescription = "법구경 제목" }
                )
                Text(
                    text = item!!.content,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .semantics { contentDescription = "법구경 본문" }
                        .clickable {
                            val intent = Intent(context, DetailActivity::class.java)
                            intent.putExtra("item_id", item!!.id)
                            context.startActivity(intent)
                        }
                )
            }
        } else {
            Text("표시할 구절이 없습니다.", modifier = Modifier.padding(16.dp))
        }
    }
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, displayName: String) {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "${displayName}_${System.currentTimeMillis()}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Dhammapada")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    var uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

    try {
        uri?.let {
            val stream: OutputStream? = resolver.openOutputStream(it)
            stream?.use { 
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) {
                    throw IOException("Failed to save bitmap.")
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(it, values, null, null)
            }
            Toast.makeText(context, "이미지가 갤러리에 저장되었습니다.", Toast.LENGTH_SHORT).show()
        } ?: throw IOException("Failed to create new MediaStore record.")
    } catch (e: IOException) {
        uri?.let { resolver.delete(it, null, null) }
        Toast.makeText(context, "이미지 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
        e.printStackTrace()
    }
}
