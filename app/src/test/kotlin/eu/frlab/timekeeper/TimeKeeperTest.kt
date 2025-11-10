package eu.frlab.timekeeper

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import kotlin.io.path.Path

typealias ConsoleReader = () -> String

inline fun captureConsoleOutput(block: (ConsoleReader) -> Unit) {
    val buffer = ByteArrayOutputStream()
    val originalOut = System.out
    val originalErr = System.err

    return PrintStream(buffer).use { printStream ->
        try {
            System.setOut(printStream)
            System.setErr(printStream)
            block {
                val content = buffer.toString()
                buffer.reset()
                content
            }
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
        }
    }
}


class TimeKeeperTest {

    @Test
    @DisplayName("Should start, stop, tick, report")
    fun `Should start, stop, tick, report`() {

        captureConsoleOutput { readAndReset ->
            try {
                val manager = Manager("./testdb")
                val initialOutput = readAndReset()
                initialOutput shouldStartWith "Initializing a new database file at"

                manager.todayReport()
                val report1 = readAndReset()
                report1 shouldStartWith "No entries for today"

                manager.start()
                Thread.sleep(1000)
                val start1 = readAndReset()
                start1 shouldStartWith "Started at"

                manager.todayReport()
                val report2 = readAndReset()
                report2 shouldStartWith "Working since"
                report2 shouldContain  "Today's work duration"
                report2 shouldContain  "Today's breaks"

                manager.stop()
                Thread.sleep(1000)
                val stop1 = readAndReset()
                stop1 shouldStartWith "Stopped at"

                manager.todayReport()
                val report3 = readAndReset()
                report3 shouldStartWith "Stopped working at"
                report3 shouldContain  "Today's work duration"
                report3 shouldContain  "Today's breaks"

                manager.tick()
                Thread.sleep(1000)
                val tick1 = readAndReset()
                tick1 shouldStartWith "Started at"

                manager.todayReport()
                val report4 = readAndReset()
                report4 shouldStartWith "Working since"
                report4 shouldContain  "Today's work duration"
                report4 shouldContain  "Today's breaks"

                manager.tick()
                Thread.sleep(1000)
                val tick2 = readAndReset()
                tick2 shouldStartWith "Stopped at"

                manager.todayReport()
                val report5 = readAndReset()
                report5 shouldStartWith "Stopped working at"
                report5 shouldContain  "Today's work duration"
                report5 shouldContain  "Today's breaks"

            } finally {
                Files.deleteIfExists(Path("./testdb"))
            }
        }
    }
}
