package eu.frlab.timekeeper.desktop

import androidx.compose.ui.window.application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.system.exitProcess

fun main() = application {
    val scope = CoroutineScope(Dispatchers.Default)
    val cliExecutor = CliExecutor()
    val trayManager = TrayManager(cliExecutor, scope) { exitProcess(0) }

    trayManager.initialize()
}
