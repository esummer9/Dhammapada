package com.ediapp.dhammapada

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.ediapp.dhammapada.ui.favorites.MemoFragment
import com.ediapp.dhammapada.ui.home.HomeFragment
import com.ediapp.dhammapada.ui.profile.KeywordFragment
import com.ediapp.dhammapada.ui.theme.MyKeywordTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyKeywordTheme {
                MyKeywordApp()
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

    ModalNavigationDrawer(
        drawerContent = {

        },
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
                        onClick = { currentDestination = it }
                    )
                }
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    Column {
                        TopAppBar(
                            title = { Text(text = "K-유틸리티") },
                            navigationIcon = {
                                Box {
                                    IconButton(onClick = { menuExpanded = true }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("어바웃") },
                                            onClick = {
                                                context.startActivity(Intent(context, AboutActivity::class.java))
                                                menuExpanded = false
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("도움말") },
                                            onClick = {
                                                context.startActivity(Intent(context, HelpActivity::class.java))
                                                menuExpanded = false
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = { Text("오픈소스") },
                                            onClick = {
                                                context.startActivity(Intent(context,
                                                    OpenSourceActivity::class.java))
                                                menuExpanded = false
                                            }
                                        )



                                    }
                                }
                            }
                        )
                        Divider(color = Color.Gray, thickness = 1.dp)
                    }
                }
            ) { scaffoldPadding ->
                Box(modifier = Modifier.padding(scaffoldPadding)) {
                    when (currentDestination) {
                        AppDestinations.HOME -> HomeFragment()
                        AppDestinations.MEMO -> MemoFragment()
                        AppDestinations.KEYWORD -> KeywordFragment()
                    }
                }
            }
        }
    }
}


enum class AppDestinations(
    val label: String,
    val icon: Any,
) {
    HOME("Home", R.drawable.home),
    MEMO("Memo", R.drawable.memo),
    KEYWORD("Keyword", R.drawable.keyword),
}
