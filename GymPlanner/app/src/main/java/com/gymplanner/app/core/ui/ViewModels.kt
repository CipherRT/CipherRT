package com.gymplanner.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gymplanner.app.AppContainer
import com.gymplanner.app.GymApp

/** Creates a ViewModel with access to the app's repositories. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(key: String? = null, crossinline create: (AppContainer) -> VM): VM {
    val app = LocalContext.current.applicationContext as GymApp
    return viewModel(key = key, factory = viewModelFactory { initializer { create(app.container) } })
}

@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as GymApp).container
