package com.ediapp.dhammapada.ui.home

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ediapp.dhammapada.DatabaseHelper
import com.ediapp.dhammapada.R
import com.ediapp.dhammapada.data.DhammapadaItem

@Composable
fun HomeFragment(modifier: Modifier = Modifier, refreshTrigger: Int) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val sharedPref = remember { context.getSharedPreferences("DhammapadaPrefs", Context.MODE_PRIVATE) }
    var item by remember { mutableStateOf<DhammapadaItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val buddaImageIds = remember {
        listOf(
            R.drawable.budda_korea_951111_1280,
            R.drawable.budda_neck_4331959_1280,
            R.drawable.budda_icicle_3043112_1280,
            R.drawable.budda_nature_7160839_1280,
            R.drawable.budda_buddhism_882879_1280,
            R.drawable.budda_lotuses_4880755_1280,
            R.drawable.budda_buddhism_5220871_1280,
            R.drawable.budda_songnisan_5309158_1280,
            R.drawable.budda_buddhism_5220871_1280,
        )
    }
    var imageResId by remember { mutableStateOf(buddaImageIds.random()) }

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

    LaunchedEffect(refreshTrigger) {
        // Only randomize image on user-triggered refresh, not on initial composition.
        if (refreshTrigger > 0) {
            imageResId = buddaImageIds.random()
        }
        isLoading = true
        val readIndex = sharedPref.getInt("read_index", 0)
        var nextItem = dbHelper.getNextItem(readIndex)

        if (nextItem == null) { // If we're at the end, loop back to the start.
            nextItem = dbHelper.getNextItem(0)
        }

        if (nextItem != null) {
            item = nextItem
            dbHelper.updateReadStatus(nextItem.id)
            with(sharedPref.edit()) {
                putInt("read_index", nextItem.id.toInt())
                apply()
            }
        }
        isLoading = false
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = "Budda Image",
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else if (item != null) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = item!!.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = item!!.content,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            Text("표시할 구절이 없습니다.", modifier = Modifier.padding(16.dp))
        }
    }
}
