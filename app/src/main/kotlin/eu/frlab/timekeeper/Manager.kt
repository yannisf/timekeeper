package eu.frlab.timekeeper

import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess


val DatabasePath = "${System.getProperty("user.home")}/.timekeeper"
val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

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
        Database("$DatabasePath/timekeeper.db")
            .apply { createTimeEntriesTable() }
    }

    fun start() {
        val today = LocalDate.now().toString()
        val now = LocalTime.now().toTime()
        if (database.hasOpenEntryForDate(today)) {
            error(ErrorCode.CANNOT_START)
        } else {
            startInternal(today, now)
        }
    }

    fun stop() {
        val today = LocalDate.now().toString()
        val now = LocalTime.now().toTime()
        if (database.hasOpenEntryForDate(today)) {
            stopInternal(today, now)
        } else {
            error(ErrorCode.CANNOT_STOP)
        }
    }

    fun tick() {
        val today = LocalDate.now().toString()
        val now = LocalTime.now().toTime()
        if (database.hasOpenEntryForDate(today)) {
            stopInternal(today, now)
        } else {
            startInternal(today, now)
        }
    }

    private fun startInternal(today: String, now: String) {
        val updatedRecords = database.startEntry(today, now)
        if (updatedRecords == 1) {
            println("Started at [$today $now]")
        }
    }

    private fun stopInternal(today: String, now: String) {
        val updatedRecords = database.stopEntry(today, now)
        if (updatedRecords == 1) {
            println("Stopped at [$today $now]")
        }
    }

}
