package com.example

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.navigation.Selection
import com.example.ui.screens.SplitViewRoot
import com.example.ui.theme.CityTheme
import com.example.ui.viewmodel.DocumentHubViewModel
import com.example.ui.viewmodel.DocumentViewModel
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val app by lazy { application as CityApplication }

    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.Factory(app.repository)
    }

    private val documentViewModel: DocumentViewModel by viewModels {
        DocumentViewModel.Factory(app.repository)
    }

    private val documentHubViewModel: DocumentHubViewModel by viewModels {
        DocumentHubViewModel.Factory(this, app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var currentThemeSetting by remember { mutableStateOf("LIGHT") }
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (currentThemeSetting) {
                "DARK" -> true
                "LIGHT" -> false
                else -> systemDark
            }

            CityTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    SplitViewRoot(
                        mainViewModel = mainViewModel,
                        documentViewModel = documentViewModel,
                        documentHubViewModel = documentHubViewModel,
                        repository = app.repository,
                        database = app.database,
                        currentTheme = currentThemeSetting,
                        onThemeChange = { currentThemeSetting = it }
                    )
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event != null && event.isCtrlPressed) {
            when (keyCode) {
                KeyEvent.KEYCODE_K -> {
                    mainViewModel.updateSearchQuery("")
                    return true
                }
                KeyEvent.KEYCODE_N -> {
                    documentViewModel.initNewNote()
                    mainViewModel.select(Selection.NewNote)
                    return true
                }
                KeyEvent.KEYCODE_S -> {
                    documentViewModel.saveCurrentNoteImmediately()
                    return true
                }
                KeyEvent.KEYCODE_P -> {
                    documentViewModel.togglePreviewMode()
                    return true
                }
            }
        }

        if (keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (mainViewModel.navigateBack()) {
                return true
            }
        }

        return super.onKeyDown(keyCode, event)
    }
}
