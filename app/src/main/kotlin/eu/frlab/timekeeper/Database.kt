package eu.frlab.timekeeper

import java.sql.Connection
import java.sql.DriverManager

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
}