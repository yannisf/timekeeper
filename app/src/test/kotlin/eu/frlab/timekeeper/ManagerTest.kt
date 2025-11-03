package eu.frlab.timekeeper

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.nio.file.Files

class ManagerTest {

    private lateinit var database: Database
    private lateinit var tempDbFile: File
    private lateinit var testDateTimeProvider: TestDateTimeProvider
    private lateinit var testErrorHandler: TestErrorHandler
    private lateinit var manager: Manager

    @BeforeEach
    fun setUp() {
        // Create temporary database for each test
        tempDbFile = Files.createTempFile("test_manager", ".db").toFile()
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
    @DisplayName("Should start entry successfully when no open entry exists")
    fun `should start entry when no open entry exists`() {
        manager.start()
        
        database.hasOpenEntryForDate("2024-01-01") shouldBe true
        testErrorHandler.errorCalled shouldBe false
    }

    @Test
    @DisplayName("Should trigger error when starting with existing open entry")
    fun `should trigger error when starting with existing open entry`() {
        // First start should succeed
        manager.start()
        database.hasOpenEntryForDate("2024-01-01") shouldBe true
        
        // Second start should trigger error
        assertThrows<TestErrorException> {
            manager.start()
        }
        testErrorHandler.errorCalled shouldBe true
        testErrorHandler.lastErrorCode shouldBe ErrorCode.CANNOT_START
    }

    @Test
    @DisplayName("Should stop entry successfully when open entry exists")
    fun `should stop entry when open entry exists`() {
        // Start an entry first
        manager.start()
        database.hasOpenEntryForDate("2024-01-01") shouldBe true
        
        // Now stop it
        testDateTimeProvider.time = "17:00:00"
        manager.stop()
        
        database.hasOpenEntryForDate("2024-01-01") shouldBe false
        testErrorHandler.errorCalled shouldBe false
    }

    @Test
    @DisplayName("Should trigger error when stopping with no open entry")
    fun `should trigger error when stopping with no open entry`() {
        assertThrows<TestErrorException> {
            manager.stop()
        }
        
        testErrorHandler.errorCalled shouldBe true
        testErrorHandler.lastErrorCode shouldBe ErrorCode.CANNOT_STOP
    }

    @Test
    @DisplayName("Should toggle from stop to start when no open entry exists")
    fun `should tick from stop to start when no open entry exists`() {
        manager.tick()
        
        database.hasOpenEntryForDate("2024-01-01") shouldBe true
        testErrorHandler.errorCalled shouldBe false
    }

    @Test
    @DisplayName("Should toggle from start to stop when open entry exists")
    fun `should tick from start to stop when open entry exists`() {
        // Start an entry first
        manager.start()
        database.hasOpenEntryForDate("2024-01-01") shouldBe true
        
        // Tick should stop it
        testDateTimeProvider.time = "17:00:00"
        manager.tick()
        
        database.hasOpenEntryForDate("2024-01-01") shouldBe false
        testErrorHandler.errorCalled shouldBe false
    }

    @Test
    @DisplayName("Should use provided date and time from DateTimeProvider")
    fun `should use provided date and time`() {
        testDateTimeProvider.date = "2024-12-31"
        testDateTimeProvider.time = "23:59:59"
        
        manager.start()
        
        database.hasOpenEntryForDate("2024-12-31") shouldBe true
        database.hasOpenEntryForDate("2024-01-01") shouldBe false
    }

    @Test
    @DisplayName("Should handle multiple entries across different dates")
    fun `should handle multiple entries across different dates`() {
        // Start entry on first date
        manager.start()
        database.hasOpenEntryForDate("2024-01-01") shouldBe true
        
        // Switch to different date and start another entry
        testDateTimeProvider.date = "2024-01-02"
        manager.start()
        
        // Both dates should have open entries
        database.hasOpenEntryForDate("2024-01-01") shouldBe true
        database.hasOpenEntryForDate("2024-01-02") shouldBe true
        testErrorHandler.errorCalled shouldBe false
    }

    // Test helper classes
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
    }

    private class TestErrorException(
        val errorCode: ErrorCode,
        val additionalInfo: String?
    ) : Exception("Test error: ${errorCode.description}")
}
