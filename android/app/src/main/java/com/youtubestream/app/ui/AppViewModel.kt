package com.youtubestream.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.youtubestream.app.App
import com.youtubestream.app.di.AppContainer

/** Builds a ViewModel from the app's [AppContainer] (Hilt deferred). */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (AppContainer) -> VM): VM {
    val container = (LocalContext.current.applicationContext as App).container
    return viewModel(factory = viewModelFactory { initializer { create(container) } })
}
