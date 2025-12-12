package eu.frlab.timekeeper.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CliExecutor {
    suspend fun executeTick(): String = withContext(Dispatchers.IO) {
        executeCommand("tick")
    }

    suspend fun executeStatus(): String = withContext(Dispatchers.IO) {
        executeCommand("status")
    }

    private fun executeCommand(command: String): String {
        val process = ProcessBuilder(
            "timekeeper", command
        ).apply {
            // Preserve environment variables (for TIMEKEEPER_DB_PATH)
            environment().putAll(System.getenv())
            redirectErrorStream(true)
        }.start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            throw RuntimeException("Command 'timekeeper $command' failed with exit code $exitCode:\n$output")
        }

        return output
    }
}
