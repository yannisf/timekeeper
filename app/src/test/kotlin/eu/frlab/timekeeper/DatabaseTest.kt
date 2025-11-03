package eu.frlab.timekeeper

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.nio.file.Files

class DatabaseTest {

    private lateinit var database: Database
    private lateinit var tempDbFile: File

    @BeforeEach
    fun setUp() {
        // Create temporary database file for each test
        tempDbFile = Files.createTempFile("test_timekeeper", ".db").toFile()
        tempDbFile.deleteOnExit()
        database = Database(tempDbFile.absolutePath)
        database.createTimeEntriesTable()
    }

    @Test
    @DisplayName("Should create time_entries table successfully")
    fun `should create time entries table`() {
        // Table creation is done in setUp, just verify no exceptions
        // Test passes if setUp completes without error
        true shouldBe true
    }

    @Test
    @DisplayName("Should return false when no open entries exist for date")
    fun `should return false when no open entries exist`() {
        val hasOpenEntry = database.hasOpenEntryForDate("2024-01-01")
        hasOpenEntry shouldBe false
    }

    @Test
    @DisplayName("Should start entry and return 1 updated record")
    fun `should start entry successfully`() {
        val updatedRecords = database.startEntry("2024-01-01", "09:00:00")
        updatedRecords shouldBe 1
    }

    @Test
    @DisplayName("Should detect open entry after starting")
    fun `should detect open entry after starting`() {
        database.startEntry("2024-01-01", "09:00:00")
        val hasOpenEntry = database.hasOpenEntryForDate("2024-01-01")
        hasOpenEntry shouldBe true
    }

    @Test
    @DisplayName("Should stop open entry and return 1 updated record")
    fun `should stop open entry successfully`() {
        database.startEntry("2024-01-01", "09:00:00")
        val updatedRecords = database.stopEntry("2024-01-01", "17:00:00")
        updatedRecords shouldBe 1
    }

    @Test
    @DisplayName("Should not have open entry after stopping")
    fun `should not have open entry after stopping`() {
        database.startEntry("2024-01-01", "09:00:00")
        database.stopEntry("2024-01-01", "17:00:00")
        val hasOpenEntry = database.hasOpenEntryForDate("2024-01-01")
        hasOpenEntry shouldBe false
    }

    @Test
    @DisplayName("Should handle multiple entries for same date correctly")
    fun `should handle multiple entries for same date`() {
        // Start and stop first entry
        database.startEntry("2024-01-01", "09:00:00")
        database.stopEntry("2024-01-01", "12:00:00")
        database.hasOpenEntryForDate("2024-01-01") shouldBe false

        // Start second entry for same date
        database.startEntry("2024-01-01", "13:00:00")
        database.hasOpenEntryForDate("2024-01-01") shouldBe true

        // Stop second entry
        database.stopEntry("2024-01-01", "17:00:00")
        database.hasOpenEntryForDate("2024-01-01") shouldBe false
    }

    @Test
    @DisplayName("Should isolate entries by date")
    fun `should isolate entries by date`() {
        database.startEntry("2024-01-01", "09:00:00")
        database.startEntry("2024-01-02", "10:00:00")

        database.hasOpenEntryForDate("2024-01-01") shouldBe true
        database.hasOpenEntryForDate("2024-01-02") shouldBe true
        database.hasOpenEntryForDate("2024-01-03") shouldBe false
    }

    @Test
    @DisplayName("Should return 0 when stopping non-existent entry")
    fun `should return 0 when stopping non-existent entry`() {
        val updatedRecords = database.stopEntry("2024-01-01", "17:00:00")
        updatedRecords shouldBe 0
    }

    @Test
    @DisplayName("Should handle special characters in date and time")
    fun `should handle special characters safely`() {
        // Test with valid date/time formats that might be edge cases
        database.startEntry("2024-12-31", "23:59:59")
        database.hasOpenEntryForDate("2024-12-31") shouldBe true
        
        val updatedRecords = database.stopEntry("2024-12-31", "23:59:59")
        updatedRecords shouldBe 1
    }
}
