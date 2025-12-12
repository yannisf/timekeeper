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

class Manager(databasePath: String, private val minIntervalSeconds: Long = 60, private val shortBreakThresholdSeconds: Long = 60) {

    companion object {
        fun error(errorCode: ErrorCode, additionalInfo: String? = null): Nothing {
            val message = additionalInfo?.let { "${errorCode.description} $it" } ?: errorCode.description
            System.err.println(message)
            exitProcess(errorCode.code)
        }
    }

    private val database = run {
        if (Files.notExists(Path(databasePath))) {
            println("Initializing database at: $databasePath")
        }
        Database(databasePath).apply { createTimeEntriesTable() }
    }

    fun tick() {
        val today = LocalDate.now().toString()
        val now = LocalTime.now().toTime()
        if (database.hasOpenEntryForDate(today)) stopInternal(today, now) else startInternal(today, now)
    }

    fun status() {
        val today = LocalDate.now().toString()
        val entries = database.dateEntries(today)
        val now = LocalTime.now()

        // Print current state
        if (entries.isEmpty()) {
            println("No entries for today")
        } else {
            val lastEntry = entries.last()
            if (lastEntry.stop == null) {
                // Currently working
                val startTime = LocalTime.parse(lastEntry.start)
                val workDuration = Duration.between(startTime, now).toPrettyString()
                println("Working since: ${lastEntry.start} ($workDuration)")
            } else {
                // On a break
                val breakStartTime = LocalTime.parse(lastEntry.stop)
                val breakDuration = Duration.between(breakStartTime, now).toPrettyString()
                println("On a break since: ${lastEntry.stop} ($breakDuration)")
            }
        }

        // Print daily summary
        if (entries.isNotEmpty()) {
            val workMinutes = entries.sumOf { it.durationInMinutes() }
            val prettyDuration = Duration.of(workMinutes, ChronoUnit.MINUTES).toPrettyString()
            val breakDurationMinutes = entries.zipWithNext { previous, next ->
                val previousStop = LocalTime.parse(previous.stop!!)
                val nextStart = LocalTime.parse(next.start)
                Duration.between(previousStop, nextStart).toMinutes()
            }.sum()

            println("Work duration today: $prettyDuration")
            if (entries.size - 1 > 0) {
                println("Breaks: ${entries.size - 1} ($breakDurationMinutes minutes)")
            } else {
                println("No breaks taken yet")
            }
        }
    }

    private fun startInternal(today: String, now: String) {
        val entries = database.dateEntries(today)

        // Check if we should extend the previous interval instead of creating a new one
        if (entries.isNotEmpty()) {
            val lastEntry = entries.last()
            if (lastEntry.stop != null) {
                val breakDurationSeconds = Duration.between(LocalTime.parse(lastEntry.stop), LocalTime.parse(now)).seconds
                if (breakDurationSeconds <= shortBreakThresholdSeconds) {
                    // Extend the previous interval
                    val updatedRecords = database.clearStopTimeOfLastEntry(today)
                    if (updatedRecords == 1) {
                        val breakDuration = Duration.ofSeconds(breakDurationSeconds).toPrettyString()
                        println("Resumed work: extending interval from ${lastEntry.start} (break: $breakDuration)")
                    }
                    return
                }
            }
        }

        printStartReport(entries, now)

        val updatedRecords = database.startEntry(today, now)
        if (updatedRecords == 1) println("Started at: $now")
    }

    private fun stopInternal(today: String, now: String) {
        val entries = database.dateEntries(today)

        if (entries.isNotEmpty()) {
            val currentEntry = entries.last()
            val currentIntervalSeconds = Duration.between(LocalTime.parse(currentEntry.start), LocalTime.parse(now)).seconds

            if (currentIntervalSeconds < minIntervalSeconds) {
                val deletedRecords = database.deleteOpenEntry(today)
                if (deletedRecords == 1) println("Discarded interval (less than ${minIntervalSeconds}s)")
                return
            }
        }

        printStopReport(entries, now)

        val updatedRecords = database.stopEntry(today, now)
        if (updatedRecords == 1) {
            println("Stopped at: $now")
            println("Enjoy your break!")
        }
    }

    private fun printStartReport(entries: List<TimeEntry>, now: String) {
        if (entries.isEmpty()) return

        // Calculate total work time today (only completed entries)
        val completedWorkMinutes = entries.sumOf { it.durationInMinutes() }
        val prettyWorkDuration = Duration.of(completedWorkMinutes, ChronoUnit.MINUTES).toPrettyString()
        println("Work duration today: $prettyWorkDuration")

        // Calculate breaks between consecutive completed entries
        val breakCount = entries.size - 1  // Breaks are gaps between existing entries
        if (breakCount > 0) {
            val breakDurationMinutes = entries.zipWithNext { previous, next ->
                val previousStop = LocalTime.parse(previous.stop!!)
                val nextStart = LocalTime.parse(next.start)
                Duration.between(previousStop, nextStart).toMinutes()
            }.sum()
            println("Breaks: $breakCount ($breakDurationMinutes minutes)")
        } else {
            println("No breaks taken yet")
        }
    }

    private fun printStopReport(entries: List<TimeEntry>, now: String) {
        if (entries.isEmpty()) return

        val currentEntry = entries.last()

        // Calculate total work time today
        val completedWorkMinutes = entries.dropLast(1).sumOf { it.durationInMinutes() }
        val currentIntervalMinutes = Duration.between(LocalTime.parse(currentEntry.start), LocalTime.parse(now)).toMinutes()
        val totalWorkMinutes = completedWorkMinutes + currentIntervalMinutes
        val prettyWorkDuration = Duration.of(totalWorkMinutes, ChronoUnit.MINUTES).toPrettyString()
        println("Work duration today: $prettyWorkDuration")

        // Calculate breaks
        val breakCount = entries.size - 1
        if (breakCount > 0) {
            val breakDurationMinutes = entries.zipWithNext { previous, next ->
                val previousStop = LocalTime.parse(previous.stop!!)
                val nextStart = LocalTime.parse(next.start)
                Duration.between(previousStop, nextStart).toMinutes()
            }.sum()
            println("Breaks: $breakCount ($breakDurationMinutes minutes)")
        } else {
            println("No breaks taken yet")
        }

        val startTime = currentEntry.start
        println("Interval started at: $startTime")
    }
}
