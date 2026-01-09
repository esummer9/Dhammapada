package com.ediapp.dhammapada.ui.settings

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun SettingsFragment(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("DhammapadaPrefs", Context.MODE_PRIVATE) }
    var useTts by remember { mutableStateOf(sharedPref.getBoolean("use_tts", false)) }
    var useWriting by remember { mutableStateOf(sharedPref.getBoolean("use_writing", true)) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("법구경 사경")
            Switch(
                checked = useWriting,
                onCheckedChange = {
                    useWriting = it
                    with(sharedPref.edit()) {
                        putBoolean("use_writing", it)
                        apply()
                    }
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TTS(읽어주기) 사용")
            Switch(
                checked = useTts,
                onCheckedChange = {
                    useTts = it
                    with(sharedPref.edit()) {
                        putBoolean("use_tts", it)
                        apply()
                    }
                    if (it) {
                        tts = TextToSpeech(context) { status ->
                            if (status == TextToSpeech.SUCCESS) {
                                tts?.language = Locale.KOREAN
                            } else {
                                val installIntent = Intent()
                                installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                                context.startActivity(installIntent)
                            }
                        }
                    } else {
                        tts?.stop()
                        tts?.shutdown()
                        tts = null
                    }
                }
            )
        }
    }
}
