package eu.frlab.timekeeper

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalTime
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
                val manager = Manager("./testdb", minIntervalSeconds = 5)
                val initialOutput = readAndReset()
                initialOutput shouldStartWith "Initializing database at:"

                manager.status(LocalDate.now(), LocalTime.now())
                val report1 = readAndReset()
                report1 shouldStartWith "No entries for today"

                manager.tick(LocalDate.now(), LocalTime.now())
                Thread.sleep(1000)
                val tick1 = readAndReset()
                tick1 shouldStartWith "Started at:"

                manager.status(LocalDate.now(), LocalTime.now())
                val report2 = readAndReset()
                report2 shouldStartWith "Working since:"
                report2 shouldContain "Work duration today:"
                report2 shouldContain "No breaks taken yet"

                manager.tick(LocalDate.now(), LocalTime.now())
                Thread.sleep(1000)
                val tick2 = readAndReset()
                tick2 shouldContain "Discarded interval (less than 5s)"

                manager.status(LocalDate.now(), LocalTime.now())
                val report3 = readAndReset()
                report3 shouldStartWith "No entries for today"

                manager.tick(LocalDate.now(), LocalTime.now())
                Thread.sleep(1000)
                val tick3 = readAndReset()
                tick3 shouldStartWith "Started at:"

                manager.status(LocalDate.now(), LocalTime.now())
                val report4 = readAndReset()
                report4 shouldStartWith "Working since:"
                report4 shouldContain "Work duration today:"
                report4 shouldContain "No breaks taken yet"

                manager.tick(LocalDate.now(), LocalTime.now())
                Thread.sleep(1000)
                val tick4 = readAndReset()
                tick4 shouldContain "Discarded interval (less than 5s)"

                manager.status(LocalDate.now(), LocalTime.now())
                val report5 = readAndReset()
                report5 shouldStartWith "No entries for today"

            } finally {
                Files.deleteIfExists(Path("./testdb"))
            }
        }
    }

    @Test
    @DisplayName("Should extend interval after short break")
    fun `Should extend interval after short break`() {

        captureConsoleOutput { readAndReset ->
            try {
                val manager = Manager("./testdb2", minIntervalSeconds = 2, shortBreakThresholdSeconds = 3)
                val initialOutput = readAndReset()
                initialOutput shouldStartWith "Initializing database at:"

                manager.tick(LocalDate.now(), LocalTime.now())
                Thread.sleep(3000)
                val tick1 = readAndReset()
                tick1 shouldStartWith "Started at:"

                manager.tick(LocalDate.now(), LocalTime.now())
                Thread.sleep(1000)
                val tick2 = readAndReset()
                tick2 shouldContain "Work duration today:"
                tick2 shouldContain "Interval started at:"
                tick2 shouldContain "Stopped at:"
                tick2 shouldContain "Enjoy your break"

                // Short break (< 3 seconds) - should extend
                Thread.sleep(1000)
                manager.tick(LocalDate.now(), LocalTime.now())
                val tick3 = readAndReset()
                tick3 shouldContain "Resumed work:"
                tick3 shouldContain "break:"

                manager.status(LocalDate.now(), LocalTime.now())
                val report = readAndReset()
                report shouldContain "Working since:"

            } finally {
                Files.deleteIfExists(Path("./testdb2"))
            }
        }
    }
}
