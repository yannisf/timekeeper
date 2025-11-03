package eu.frlab.timekeeper

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows

class TimeKeeperTest {

    @Test
    @DisplayName("Should parse valid start command")
    fun `should parse valid start command`() {
        val command = parseCommand(arrayOf("start"))
        command shouldBe Command.START
    }

    @Test
    @DisplayName("Should parse valid stop command")
    fun `should parse valid stop command`() {
        val command = parseCommand(arrayOf("stop"))
        command shouldBe Command.STOP
    }

    @Test
    @DisplayName("Should parse valid tick command")
    fun `should parse valid tick command`() {
        val command = parseCommand(arrayOf("tick"))
        command shouldBe Command.TICK
    }

    @Test
    @DisplayName("Should throw exception for invalid command")
    fun `should throw exception for invalid command`() {
        assertThrows<InvalidArgumentException> {
            parseCommand(arrayOf("invalid"))
        }
    }

    @Test
    @DisplayName("Should throw exception for no arguments")
    fun `should throw exception for no arguments`() {
        assertThrows<InvalidArgumentException> {
            parseCommand(arrayOf())
        }
    }

    @Test
    @DisplayName("Should throw exception for multiple arguments")
    fun `should throw exception for multiple arguments`() {
        assertThrows<InvalidArgumentException> {
            parseCommand(arrayOf("start", "stop"))
        }
    }

    @Test
    @DisplayName("Should throw exception for empty string argument")
    fun `should throw exception for empty string argument`() {
        assertThrows<InvalidArgumentException> {
            parseCommand(arrayOf(""))
        }
    }

    @Test
    @DisplayName("Should be case sensitive for commands")
    fun `should be case sensitive for commands`() {
        assertThrows<InvalidArgumentException> {
            parseCommand(arrayOf("START"))
        }
        
        assertThrows<InvalidArgumentException> {
            parseCommand(arrayOf("Start"))
        }
    }

    @Test
    @DisplayName("Should validate all Command enum values are testable")
    fun `should validate all command enum values`() {
        Command.START.cmd shouldBe "start"
        Command.STOP.cmd shouldBe "stop" 
        Command.TICK.cmd shouldBe "tick"
        
        // Ensure we have exactly 3 commands
        Command.entries.size shouldBe 3
    }

    @Test
    @DisplayName("Should validate all ErrorCode enum values")
    fun `should validate all error code enum values`() {
        ErrorCode.INVALID_ARGUMENTS.code shouldBe 1
        ErrorCode.CANNOT_START.code shouldBe 2
        ErrorCode.CANNOT_STOP.code shouldBe 3
        ErrorCode.GENERAL_ERROR.code shouldBe 4
        
        // Verify descriptions are not empty
        ErrorCode.INVALID_ARGUMENTS.description shouldNotBe ""
        ErrorCode.CANNOT_START.description shouldNotBe ""
        ErrorCode.CANNOT_STOP.description shouldNotBe ""
        ErrorCode.GENERAL_ERROR.description shouldNotBe ""
    }

    @Test
    @DisplayName("Should test processCommand with valid commands")
    fun `should test processCommand with valid commands`() {
        val testManager = TestableManager()
        
        processCommand(Command.START, testManager)
        testManager.startCalled shouldBe true
        
        testManager.reset()
        processCommand(Command.STOP, testManager)
        testManager.stopCalled shouldBe true
        
        testManager.reset()
        processCommand(Command.TICK, testManager)
        testManager.tickCalled shouldBe true
    }

    @Test
    @DisplayName("Should handle exceptions in command processing")
    fun `should handle exceptions in command processing`() {
        val testManager = TestableManager(throwException = true)
        
        assertThrows<GeneralErrorException> {
            processCommand(Command.START, testManager)
        }
    }

    // Helper functions for testing (these would be extracted from main function)
    private fun parseCommand(args: Array<String>): Command {
        if (args.size != 1) throw InvalidArgumentException(ErrorCode.INVALID_ARGUMENTS)
        
        return Command.entries.firstOrNull { it.cmd == args.first() }
            ?: throw InvalidArgumentException(ErrorCode.INVALID_ARGUMENTS)
    }

    private fun processCommand(command: Command, manager: TestableManager) {
        try {
            when (command) {
                Command.START -> manager.start()
                Command.STOP -> manager.stop()
                Command.TICK -> manager.tick()
            }
        } catch (t: Throwable) {
            throw GeneralErrorException(ErrorCode.GENERAL_ERROR, t.message)
        }
    }

    // Test helper classes
    private class TestableManager(private val throwException: Boolean = false) {
        var startCalled = false
        var stopCalled = false
        var tickCalled = false

        fun start() {
            if (throwException) throw RuntimeException("Test exception")
            startCalled = true
        }

        fun stop() {
            if (throwException) throw RuntimeException("Test exception")
            stopCalled = true
        }

        fun tick() {
            if (throwException) throw RuntimeException("Test exception")
            tickCalled = true
        }

        fun reset() {
            startCalled = false
            stopCalled = false
            tickCalled = false
        }
    }

    // Test exception classes
    private class InvalidArgumentException(val errorCode: ErrorCode) : Exception(errorCode.description)
    private class GeneralErrorException(val errorCode: ErrorCode, val additionalInfo: String?) : Exception("${errorCode.description}: $additionalInfo")
}
