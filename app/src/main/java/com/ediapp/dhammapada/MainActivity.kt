package com.ediapp.dhammapada

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.ediapp.dhammapada.ui.home.HomeFragment
import com.ediapp.dhammapada.ui.lists.ListFragment
import com.ediapp.dhammapada.ui.settings.SettingsFragment
import com.ediapp.dhammapada.ui.theme.MyKeywordTheme
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission grant result if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        askNotificationPermission()

        MobileAds.initialize(this)

        val sharedPref = getSharedPreferences("DhammapadaPrefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        var needsApply = false

        if (!sharedPref.getBoolean("data_inserted", false)) {
            val dbHelper = DatabaseHelper(this)
            dbHelper.insertInitialData(this)
            editor.putBoolean("data_inserted", true)
            needsApply = true
        }

        if (!sharedPref.contains("install_time")) {
            editor.putLong("install_time", System.currentTimeMillis())
            needsApply = true
        }
        if (!sharedPref.contains("read_index")) {
            editor.putInt("read_index", 0)
            needsApply = true
        }

        if (needsApply) {
            editor.apply()
        }

        enableEdgeToEdge()
        setContent {
            MyKeywordTheme {
                MyKeywordApp()
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewScreenSizes
@Composable
fun MyKeywordApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var menuExpanded by remember { mutableStateOf(false) }
    var refreshHomeTrigger by remember { mutableIntStateOf(0) }
    var homeFragmentActions by remember { mutableStateOf<(@Composable RowScope.() -> Unit)?>(null) }


    ModalNavigationDrawer(
        drawerContent = {},
        drawerState = drawerState
    ) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.forEach {
                    item(
                        icon = {
                            when (val icon = it.icon) {
                                is ImageVector -> Icon(
                                    icon,
                                    contentDescription = it.label,
                                    modifier = Modifier.size(25.dp)
                                )
                                is Int -> Icon(
                                    painterResource(id = icon),
                                    contentDescription = it.label,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                        },
                        label = { Text(it.label) },
                        selected = it == currentDestination,
                        onClick = { 
                            currentDestination = it 
                            if (it != AppDestinations.HOME) {
                                homeFragmentActions = null
                            }
                        }
                    )
                }
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    Column {
                        TopAppBar(
                            title = { Text(text = "내손안의 법구경") },
                            navigationIcon = {
                                Box {
                                    IconButton(onClick = { menuExpanded = true }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
//                                        DropdownMenuItem(
//                                            text = { Text("어바웃") },
//                                            onClick = {
//                                                context.startActivity(Intent(context, AboutActivity::class.java))
//                                                menuExpanded = false
//                                            }
//                                        )

                                        DropdownMenuItem(
                                            text = { Text("도움말") },
                                            onClick = {
                                                context.startActivity(Intent(context, HelpActivity::class.java))
                                                menuExpanded = false
                                            }
                                        )

//                                        DropdownMenuItem(
//                                            text = { Text("오픈소스") },
//                                            onClick = {
//                                                context.startActivity(Intent(context, OpenSourceActivity::class.java))
//                                                menuExpanded = false
//                                            }
//                                        )
                                    }
                                }
                            },
                            actions = {
                                if (currentDestination == AppDestinations.HOME) {
//                                    IconButton(onClick = { refreshHomeTrigger++ }) {
//                                        Icon(painterResource(id = R.drawable.arrowhead), contentDescription = "Next", tint = Color.Unspecified)
//                                    }
                                    homeFragmentActions?.let { it() }
                                }
                            }
                        )
                        Divider(color = Color.Gray, thickness = 1.dp)
                    }
                },
                content = { scaffoldPadding ->
                    Column(Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f).padding(scaffoldPadding)) {
                            when (currentDestination) {
                                AppDestinations.HOME -> HomeFragment(
                                    refreshTrigger = refreshHomeTrigger,
                                    setActions = { homeFragmentActions = it }
                                )
                                AppDestinations.MEMO -> ListFragment()
                                AppDestinations.KEYWORD -> SettingsFragment()
                            }
                        }
                        AndroidView(
                            modifier = Modifier.fillMaxWidth(),
                            factory = { context ->
                                AdView(context).apply {
                                    setAdSize(AdSize.BANNER)
                                    adUnitId = "ca-app-pub-9901915016619662/1755707787" // Test Ad Unit ID
                                    loadAd(AdRequest.Builder().build())
                                }
                            }
                        )
                    }
                }
            )
        }
    }
}


enum class AppDestinations(
    val label: String,
    val icon: Any,
) {
    HOME("Home", R.drawable.home),
    MEMO("Lists", R.drawable.memo),
    KEYWORD("Settings", R.drawable.settings),
}
