package eu.frlab.timekeeper

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.nio.file.Files

class IntegrationTest {

    private lateinit var database: Database
    private lateinit var tempDbFile: File
    private lateinit var testDateTimeProvider: TestDateTimeProvider
    private lateinit var testErrorHandler: TestErrorHandler
    private lateinit var manager: Manager

    @BeforeEach
    fun setUp() {
        // Create temporary database for each test
        tempDbFile = Files.createTempFile("test_integration", ".db").toFile()
        tempDbFile.deleteOnExit()
        database = Database(tempDbFile.absolutePath)
        database.createTimeEntriesTable()

        // Create test implementations
        testDateTimeProvider = TestDateTimeProvider("2024-01-01", "09:00:00")
        testErrorHandler = TestErrorHandler()

        // Create manager with test dependencies
        manager = Manager(database, testDateTimeProvider, testErrorHandler)
    }

    @Test
    @DisplayName("Should complete full start-stop workflow")
    fun `should complete full start stop workflow`() {
        // Initially no open entries
        database.hasOpenEntryForDate("2024-01-01") shouldBe false

        // Start entry
        manager.start()
        database.hasOpenEntryForDate("2024-01-01") shouldBe true
        testErrorHandler.errorCalled shouldBe false

        // Change time and stop entry
        testDateTimeProvider.time = "17:00:00"
        manager.stop()
        database.hasOpenEntryForDate("2024-01-01") shouldBe false
        testErrorHandler.errorCalled shouldBe false
    }

    @Test
    @DisplayName("Should handle multiple start-stop cycles in same day")
    fun `should handle multiple start stop cycles same day`() {
        val date = "2024-01-01"

        // First cycle: 09:00 - 12:00
        testDateTimeProvider.time = "09:00:00"
        manager.start()
        database.hasOpenEntryForDate(date) shouldBe true

        testDateTimeProvider.time = "12:00:00"
        manager.stop()
        database.hasOpenEntryForDate(date) shouldBe false

        // Second cycle: 13:00 - 17:00
        testDateTimeProvider.time = "13:00:00"
        manager.start()
        database.hasOpenEntryForDate(date) shouldBe true

        testDateTimeProvider.time = "17:00:00"
        manager.stop()
        database.hasOpenEntryForDate(date) shouldBe false

        testErrorHandler.errorCalled shouldBe false
    }

    @Test
    @DisplayName("Should handle tick functionality end-to-end")
    fun `should handle tick functionality end to end`() {
        val date = "2024-01-01"

        // Initially no entries, tick should start
        testDateTimeProvider.time = "09:00:00"
        manager.tick()
        database.hasOpenEntryForDate(date) shouldBe true

        // Now there's an open entry, tick should stop
        testDateTimeProvider.time = "12:00:00"
        manager.tick()
        database.hasOpenEntryForDate(date) shouldBe false

        // No open entry again, tick should start
        testDateTimeProvider.time = "13:00:00"
        manager.tick()
        database.hasOpenEntryForDate(date) shouldBe true

        testErrorHandler.errorCalled shouldBe false
    }

    @Test
    @DisplayName("Should handle entries across multiple dates")
    fun `should handle entries across multiple dates`() {
        // Start entry on first date
        testDateTimeProvider.date = "2024-01-01"
        testDateTimeProvider.time = "09:00:00"
        manager.start()
        database.hasOpenEntryForDate("2024-01-01") shouldBe true

        // Start entry on second date (different day)
        testDateTimeProvider.date = "2024-01-02"
        testDateTimeProvider.time = "10:00:00"
        manager.start()
        database.hasOpenEntryForDate("2024-01-02") shouldBe true

        // Both dates should have open entries
        database.hasOpenEntryForDate("2024-01-01") shouldBe true
        database.hasOpenEntryForDate("2024-01-02") shouldBe true

        // Stop entry on second date
        testDateTimeProvider.time = "18:00:00"
        manager.stop()
        database.hasOpenEntryForDate("2024-01-02") shouldBe false
        database.hasOpenEntryForDate("2024-01-01") shouldBe true // Still open

        // Stop entry on first date
        testDateTimeProvider.date = "2024-01-01"
        testDateTimeProvider.time = "17:00:00"
        manager.stop()
        database.hasOpenEntryForDate("2024-01-01") shouldBe false

        testErrorHandler.errorCalled shouldBe false
    }

    @Test
    @DisplayName("Should handle error conditions in complete workflow")
    fun `should handle error conditions in complete workflow`() {
        val date = "2024-01-01"

        // Try to stop without starting
        assertThrows<TestErrorException> {
            manager.stop()
        }
        testErrorHandler.errorCalled shouldBe true
        testErrorHandler.lastErrorCode shouldBe ErrorCode.CANNOT_STOP
        testErrorHandler.reset()

        // Start successfully
        manager.start()
        database.hasOpenEntryForDate(date) shouldBe true
        testErrorHandler.errorCalled shouldBe false

        // Try to start again (should fail)
        assertThrows<TestErrorException> {
            manager.start()
        }
        testErrorHandler.errorCalled shouldBe true
        testErrorHandler.lastErrorCode shouldBe ErrorCode.CANNOT_START
        testErrorHandler.reset()

        // Stop successfully
        testDateTimeProvider.time = "17:00:00"
        manager.stop()
        database.hasOpenEntryForDate(date) shouldBe false
        testErrorHandler.errorCalled shouldBe false

        // Try to stop again (should fail)
        assertThrows<TestErrorException> {
            manager.stop()
        }
        testErrorHandler.errorCalled shouldBe true
        testErrorHandler.lastErrorCode shouldBe ErrorCode.CANNOT_STOP
    }

    @Test
    @DisplayName("Should persist data across database operations")
    fun `should persist data across database operations`() {
        // Create and populate database
        manager.start()
        testDateTimeProvider.time = "17:00:00"
        manager.stop()

        // Create new database instance pointing to same file
        val newDatabase = Database(tempDbFile.absolutePath)
        val newManager = Manager(newDatabase, testDateTimeProvider, testErrorHandler)

        // Should be able to start new entry (previous was closed)
        testDateTimeProvider.time = "18:00:00"
        newManager.start()
        newDatabase.hasOpenEntryForDate("2024-01-01") shouldBe true
        testErrorHandler.errorCalled shouldBe false
    }

    @Test
    @DisplayName("Should validate time formatting integration")
    fun `should validate time formatting integration`() {
        // Test edge case times
        testDateTimeProvider.time = "00:00:00"
        manager.start()
        database.hasOpenEntryForDate("2024-01-01") shouldBe true

        testDateTimeProvider.time = "23:59:59"
        manager.stop()
        database.hasOpenEntryForDate("2024-01-01") shouldBe false

        testErrorHandler.errorCalled shouldBe false
    }

    // Test helper classes (reused from ManagerTest)
    private class TestDateTimeProvider(
        var date: String,
        var time: String
    ) : DateTimeProvider {
        override fun getCurrentDate(): String = date
        override fun getCurrentTime(): String = time
    }

    private class TestErrorHandler : ErrorHandler {
        var errorCalled = false
        var lastErrorCode: ErrorCode? = null
        var lastAdditionalInfo: String? = null

        override fun handleError(errorCode: ErrorCode, additionalInfo: String?): Nothing {
            errorCalled = true
            lastErrorCode = errorCode
            lastAdditionalInfo = additionalInfo
            throw TestErrorException(errorCode, additionalInfo)
        }

        fun reset() {
            errorCalled = false
            lastErrorCode = null
            lastAdditionalInfo = null
        }
    }

    private class TestErrorException(
        val errorCode: ErrorCode,
        val additionalInfo: String?
    ) : Exception("Test error: ${errorCode.description}")
}
