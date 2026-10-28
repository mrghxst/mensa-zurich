// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mrghxst.ethmensa.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.mrghxst.ethmensa.ui.list.MensaListScreen
import io.github.mrghxst.ethmensa.ui.list.MensaListViewModel

@Composable
fun EthMensaApp() {
    val graph = LocalAppGraph.current
    val listViewModel: MensaListViewModel = viewModel(factory = viewModelFactory { initializer { MensaListViewModel(graph) } })
    MensaListScreen(
        viewModel = listViewModel,
        onOpenMensa = {},
        onOpenSettings = {},
        onOpenLegi = {},
    )
}
