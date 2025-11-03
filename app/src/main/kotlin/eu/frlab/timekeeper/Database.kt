package eu.frlab.timekeeper

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.time.Duration
import java.time.LocalTime

class Database(dbName: String) {

    companion object {
        val create = """
            CREATE TABLE IF NOT EXISTS time_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                start TEXT NOT NULL,
                stop TEXT
            )
        """.trimIndent()

        val check = "SELECT COUNT(*) AS count FROM time_entries WHERE date = ? AND stop IS NULL"
        val start = "INSERT INTO time_entries (date, start) VALUES (?, ?)"
        val stop = "UPDATE time_entries SET stop = ? WHERE date = ? and stop IS NULL"
        val allForDay = "SELECT * FROM time_entries WHERE date = ? ORDER BY id ASC"
    }

    private val conn: Connection

    init {
        val url = "jdbc:sqlite:$dbName"
        conn = DriverManager.getConnection(url)
    }

    fun createTimeEntriesTable() = conn.createStatement().use {
        it.execute(create)
    }

    fun hasOpenEntryForDate(today: String) = conn.prepareStatement(check).use {
        it.setString(1, today)
        val rs = it.executeQuery()
        rs.getInt("count") > 0
    }

    fun startEntry(today: String, now: String) = conn.prepareStatement(start).use {
        it.setString(1, today)
        it.setString(2, now)
        it.executeUpdate()
    }

    fun stopEntry(today: String, now: String) = conn.prepareStatement(stop).use {
        it.setString(1, now)
        it.setString(2, today)
        it.executeUpdate()
    }

    fun dateEntries(date: String) = conn.prepareStatement(allForDay).use {
        it.setString(1, date)
        it.executeQuery().use {
            rs -> generateSequence { if (rs.next()) TimeEntry(rs) else null }.toList()
        }
    }
}

data class TimeEntry(
    val id: Int,
    val date: String,
    val start: String,
    val stop: String?
) {
    constructor(rs: ResultSet) : this(
        id = rs.getInt("id"),
        date = rs.getString("date"),
        start = rs.getString("start"),
        stop = rs.getString("stop")
    )

    fun durationInMinutes(): Long {
        val startLocalTime = LocalTime.parse(start)
        val stopLocalTime = stop?.let {
            LocalTime.parse(it)
        } ?: LocalTime.now()

        return Duration.between(startLocalTime, stopLocalTime).toMinutes()
    }
}