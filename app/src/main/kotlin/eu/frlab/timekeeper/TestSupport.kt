package eu.frlab.timekeeper

import java.time.LocalDate
import java.time.LocalTime

// Date/Time Provider Interface for dependency injection
interface DateTimeProvider {
    fun getCurrentDate(): String
    fun getCurrentTime(): String
}

// System implementation using actual system time
class SystemDateTimeProvider : DateTimeProvider {
    override fun getCurrentDate(): String = LocalDate.now().toString()
    override fun getCurrentTime(): String = LocalTime.now().toTime()
}

// Error Handler Interface for testable error handling
interface ErrorHandler {
    fun handleError(errorCode: ErrorCode, additionalInfo: String? = null): Nothing
}

// System implementation that calls exitProcess
class SystemErrorHandler : ErrorHandler {
    override fun handleError(errorCode: ErrorCode, additionalInfo: String?): Nothing {
        Manager.error(errorCode, additionalInfo)
    }
}
