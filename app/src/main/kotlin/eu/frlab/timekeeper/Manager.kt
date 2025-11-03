package eu.frlab.timekeeper

import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.system.exitProcess


val DatabasePath = "${System.getProperty("user.home")}/.timekeeper"
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

class Manager {

    companion object {
        fun error(errorCode: ErrorCode, additionalInfo: String? = null): Nothing {
            System.err.println(errorCode.description)
            additionalInfo?.let { System.err.println(it) }
            exitProcess(errorCode.code)
        }
    }

    private val database = run {
        File(DatabasePath).mkdirs()
        Database("$DatabasePath/timekeeper.db").apply { createTimeEntriesTable() }
    }

    fun start() {
        val today = LocalDate.now().toString()
        val now = LocalTime.now().toTime()
        if (database.hasOpenEntryForDate(today)) error(ErrorCode.CANNOT_START) else startInternal(today, now)
    }

    fun stop() {
        val today = LocalDate.now().toString()
        val now = LocalTime.now().toTime()
        if (database.hasOpenEntryForDate(today)) stopInternal(today, now) else error(ErrorCode.CANNOT_STOP)
    }

    fun tick() {
        val today = LocalDate.now().toString()
        val now = LocalTime.now().toTime()
        if (database.hasOpenEntryForDate(today)) stopInternal(today, now) else startInternal(today, now)
    }

    fun todayReport() {
        val today = LocalDate.now().toString()
        val entries = database.dateEntries(today)
        if (entries.isEmpty()) {
            println("No entries for today ($today).")
        } else {
            val workMinutes = entries.sumOf { it.durationInMinutes() }
            val prettyDuration = Duration.of(workMinutes, ChronoUnit.MINUTES).toPrettyString()
            val breakDurationMinutes = entries.zipWithNext { previous, next ->
                val previousStop = LocalTime.parse(previous.stop!!)
                val nextStart = LocalTime.parse(next.start)
                Duration.between(previousStop, nextStart).toMinutes()
            }.sum()

            println("Today's work duration: $prettyDuration")
            println("Today's breaks: ${entries.size - 1} for a total of $breakDurationMinutes minutes")
        }
    }

    private fun startInternal(today: String, now: String) {
        val updatedRecords = database.startEntry(today, now)
        if (updatedRecords == 1) println("Started at [$today $now]")
    }

    private fun stopInternal(today: String, now: String) {
        val updatedRecords = database.stopEntry(today, now)
        if (updatedRecords == 1) println("Stopped at [$today $now]")
    }

}
