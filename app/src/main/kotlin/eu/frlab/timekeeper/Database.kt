package eu.frlab.timekeeper

import java.sql.Connection
import java.sql.DriverManager

class Database(dbName: String) {
    private val conn: Connection

    init {
        val url = "jdbc:sqlite:$dbName"
        conn = DriverManager.getConnection(url)
    }

    fun createTimeEntriesTable() {
        val sql = """
            CREATE TABLE IF NOT EXISTS time_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                start TEXT NOT NULL,
                stop TEXT
            )
        """.trimIndent()

        conn.createStatement().use { it.execute(sql) }
    }

    fun hasOpenEntryForDate(today: String): Boolean {
        val sql = "SELECT COUNT(*) AS count FROM time_entries WHERE date = ? AND stop IS NULL"

        return conn.prepareStatement(sql).use {
            it.setString(1, today)
            val rs = it.executeQuery()
            rs.getInt("count") > 0
        }
    }

    fun startEntry(today: String, now: String): Int {
        val sql = "INSERT INTO time_entries (date, start) VALUES (?, ?)"

        return conn.prepareStatement(sql).use {
            it.setString(1, today)
            it.setString(2, now)
            it.executeUpdate()
        }
    }

    fun stopEntry(today: String, now: String): Int {
        val sql = "UPDATE time_entries SET stop = ? WHERE date = ? and stop IS NULL"

        return conn.prepareStatement(sql).use {
            it.setString(1, now)
            it.setString(2, today)
            it.executeUpdate()
        }
    }

}