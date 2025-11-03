package eu.frlab.timekeeper

import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess


val DatabasePath = "${System.getProperty("user.home")}/.timekeeper"
val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

fun LocalTime.toTime(): String = this.format(TimeFormatter)

class Manager(
    private val database: Database? = null,
    private val dateTimeProvider: DateTimeProvider = SystemDateTimeProvider(),
    private val errorHandler: ErrorHandler = SystemErrorHandler()
) {

    companion object {
        fun error(errorCode: ErrorCode, additionalInfo: String? = null): Nothing {
            System.err.println(errorCode.description)
            additionalInfo?.let { System.err.println(it) }
            exitProcess(errorCode.code)
        }
    }

    private val db = database ?: run {
        File(DatabasePath).mkdirs()
        Database("$DatabasePath/timekeeper.db").apply { createTimeEntriesTable() }
    }

    fun start() {
        val today = dateTimeProvider.getCurrentDate()
        val now = dateTimeProvider.getCurrentTime()
        if (db.hasOpenEntryForDate(today)) errorHandler.handleError(ErrorCode.CANNOT_START) else startInternal(today, now)
    }

    fun stop() {
        val today = dateTimeProvider.getCurrentDate()
        val now = dateTimeProvider.getCurrentTime()
        if (db.hasOpenEntryForDate(today)) stopInternal(today, now) else errorHandler.handleError(ErrorCode.CANNOT_STOP)
    }

    fun tick() {
        val today = dateTimeProvider.getCurrentDate()
        val now = dateTimeProvider.getCurrentTime()
        if (db.hasOpenEntryForDate(today)) stopInternal(today, now) else startInternal(today, now)
    }

    private fun startInternal(today: String, now: String) {
        val updatedRecords = db.startEntry(today, now)
        if (updatedRecords == 1) println("Started at [$today $now]")
    }

    private fun stopInternal(today: String, now: String) {
        val updatedRecords = db.stopEntry(today, now)
        if (updatedRecords == 1) println("Stopped at [$today $now]")
    }

}
