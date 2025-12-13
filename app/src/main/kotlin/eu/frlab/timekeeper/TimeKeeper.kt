package eu.frlab.timekeeper

enum class Command(val cmd: String) {
    TICK("tick"),
    STATUS("status");
}

enum class ErrorCode(val code: Int, val description: String) {
    INVALID_ARGUMENTS(1, "Expected one of: ${Command.entries.joinToString(", ") { it.cmd }}"),
    GENERAL_ERROR(2, "An unexpected error occurred.");
}

fun main(args: Array<String>) {

    //Argument parsing and validation
    if (args.size != 1) Manager.error(ErrorCode.INVALID_ARGUMENTS)
    val command = args.first().let { arg ->
        Command.entries.firstOrNull { it.cmd == arg }
    } ?: Manager.error(ErrorCode.INVALID_ARGUMENTS)

    val databasePath = System.getenv("TIMEKEEPER_DB_PATH")
        ?: "${System.getProperty("user.home")}/.timekeeper.db"

    //Command execution
    try {
        val manager = Manager(databasePath)
        val today = java.time.LocalDate.now()
        val now = java.time.LocalTime.now()

        when (command) {
            Command.TICK -> manager.tick(today, now)
            Command.STATUS -> manager.status(today, now)
        }
    } catch (t: Throwable) {
        Manager.error(ErrorCode.GENERAL_ERROR, t.message)
    }
}
