package eu.frlab.timekeeper

import java.nio.file.Files
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.io.path.Path
import kotlin.system.exitProcess


val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

fun Duration.toPrettyString(): String = buildString {
    val hours = toHours()
    val minutes = toMinutesPart()
    val seconds = toSecondsPart()

    if (hours > 0) append("${hours}h ")
    if (minutes > 0) append("${minutes}m ")
    if (seconds > 0 || isEmpty()) append("${seconds}s")
}.trim()

fun LocalTime.toTime(): String = this.format(TimeFormatter)

class Manager(
    databasePath: String, private val minIntervalSeconds: Long = 60, private val shortBreakThresholdSeconds: Long = 60
) {

    private val database = run {
        if (Files.notExists(Path(databasePath))) println("Initializing database at: $databasePath")
        Database(databasePath).apply { createTimeEntriesTable() }
    }

    fun tick(today: LocalDate, now: LocalTime) {
        if (database.hasOpenEntryForDate(today.toString())) stopInternal(today, now) else startInternal(today, now)
    }

    fun status(today: LocalDate, now: LocalTime) {
        val entries = database.dateEntries(today.toString())

        // Print current state
        when {
            entries.isEmpty() -> println("No entries for today")
            else -> {
                val lastEntry = entries.last()
                when (lastEntry.stop) {
                    null -> {
                        // Currently working
                        val startTime = LocalTime.parse(lastEntry.start)
                        val duration = Duration.between(startTime, now).toPrettyString()
                        println("Working since: ${lastEntry.start} ($duration)")
                    }
                    else -> {
                        // On a break
                        val stopTime = LocalTime.parse(lastEntry.stop)
                        val duration = Duration.between(stopTime, now).toPrettyString()
                        println("On a break since: ${lastEntry.stop} ($duration)")
                    }
                }
            }
        }

        // Print daily summary
        if (entries.isNotEmpty()) {
            printWorkDuration(calculateWorkMinutes(entries, now))
            val (breakCount, breakMinutes) = calculateBreakStatistics(entries)
            printBreakSummary(breakCount, breakMinutes)
        }
    }

    private fun startInternal(today: LocalDate, now: LocalTime) {
        val entries = database.dateEntries(today.toString())

        val shouldExtend = entries.lastOrNull()?.let { lastEntry ->
            lastEntry.stop != null &&
            Duration.between(LocalTime.parse(lastEntry.stop), now).seconds <= shortBreakThresholdSeconds
        } ?: false

        if (shouldExtend) {
            // Extend interval
            val lastEntry = entries.last()
            val breakDuration = Duration.between(LocalTime.parse(lastEntry.stop!!), now)
            database.clearStopTimeOfLastEntry(today.toString()).takeIf { it == 1 }?.let {
                println("Resumed work: extending interval from ${lastEntry.start} (break: ${breakDuration.toPrettyString()})")
            }
        } else {
            // Normal start flow
            printStartReport(entries, now)
            database.startEntry(today.toString(), now.toTime()).takeIf { it == 1 }?.let {
                println("Started at: ${now.toTime()}")
            }
        }
    }

    private fun stopInternal(today: LocalDate, now: LocalTime) {
        val entries = database.dateEntries(today.toString())

        val shouldDiscard = entries.lastOrNull()?.let { currentEntry ->
            val intervalDuration = Duration.between(LocalTime.parse(currentEntry.start), now)
            intervalDuration.seconds < minIntervalSeconds
        } ?: false

        if (shouldDiscard) {
            // Discard interval
            database.deleteOpenEntry(today.toString()).takeIf { it == 1 }?.let {
                println("Discarded interval (less than ${minIntervalSeconds}s)")
            }
        } else {
            // Normal stop flow
            printStopReport(entries, now)
            database.stopEntry(today.toString(), now.toTime()).takeIf { it == 1 }?.let {
                println("Stopped at: ${now.toTime()}")
                println("Enjoy your break!")
            }
        }
    }

    private fun printStartReport(entries: List<TimeEntry>, now: LocalTime) {
        if (entries.isEmpty()) return

        printWorkDuration(calculateWorkMinutes(entries, now))
        val (breakCount, breakMinutes) = calculateBreakStatistics(entries)

        // Count the current break ending now
        val currentBreak = entries.lastOrNull()?.stop?.let { stop ->
            Duration.between(LocalTime.parse(stop), now).toMinutes()
        } ?: 0L

        printBreakSummary(breakCount + if (currentBreak > 0) 1 else 0, breakMinutes + currentBreak)
    }

    private fun printStopReport(entries: List<TimeEntry>, now: LocalTime) {
        if (entries.isEmpty()) return

        printWorkDuration(calculateWorkMinutes(entries, now))
        val (breakCount, breakMinutes) = calculateBreakStatistics(entries)
        printBreakSummary(breakCount, breakMinutes)

        println("Interval started at: ${entries.last().start}")
    }

    private fun calculateBreakStatistics(entries: List<TimeEntry>) = if (entries.size >= 2) {
        val breakMinutes = entries.zipWithNext { previous, next ->
            val lastStopTime = LocalTime.parse(previous.stop!!)
            val nextStartTime = LocalTime.parse(next.start)
            Duration.between(lastStopTime, nextStartTime).toMinutes()
        }.sum()
        (entries.size - 1) to breakMinutes
    } else 0 to 0L

    private fun calculateWorkMinutes(entries: List<TimeEntry>, now: LocalTime) = if (entries.isNotEmpty()) {
        val lastEntry = entries.last()
        if (lastEntry.stop == null) {
            // Last entry is open - calculate with provided 'now'
            val completedMinutes = entries.dropLast(1).sumOf { it.durationInMinutes() }
            val currentMinutes = Duration.between(LocalTime.parse(lastEntry.start), now).toMinutes()
            completedMinutes + currentMinutes
        } else {
            // All entries are closed - just sum them up
            entries.sumOf { it.durationInMinutes() }
        }
    } else 0L

    private fun printWorkDuration(workMinutes: Long) {
        val duration = Duration.of(workMinutes, ChronoUnit.MINUTES).toPrettyString()
        println("Work duration today: $duration")
    }

    private fun printBreakSummary(breakCount: Int, breakMinutes: Long) = when {
        breakCount > 0 -> println("Breaks: $breakCount ($breakMinutes minutes)")
        else -> println("No breaks taken yet")
    }

    companion object {
        fun error(errorCode: ErrorCode, additionalInfo: String? = null): Nothing {
            val message = additionalInfo?.let { "${errorCode.description} $it" } ?: errorCode.description
            System.err.println(message)
            exitProcess(errorCode.code)
        }
    }

}
