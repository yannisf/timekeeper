package eu.frlab.timekeeper

enum class Command(val cmd: String) {
    START("start"),
    STOP("stop"),
    TICK("tick"),
    REPORT("report");
}

enum class ErrorCode(val code: Int, val description: String) {
    INVALID_ARGUMENTS(1, "Expected one of: ${Command.entries.joinToString(", ") { it.cmd }}"),
    CANNOT_START(2, "Cannot start a new entry when one is already open."),
    CANNOT_STOP(3, "Cannot stop an entry when none is open."),
    GENERAL_ERROR(4, "An unexpected error occurred.");
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
        when (command) {
            Command.START -> manager.start()
            Command.STOP -> manager.stop()
            Command.TICK -> manager.tick()
            Command.REPORT -> manager.todayReport()
        }
    } catch (t: Throwable) {
        Manager.error(ErrorCode.GENERAL_ERROR, t.message)
    }
}
