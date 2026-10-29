package dev.pipos.launcher

import dev.pipos.launcher.ui.TerminalCommand
import dev.pipos.launcher.ui.parseCommand
import dev.pipos.launcher.data.ThemeProfile
import org.junit.Assert.*
import org.junit.Test

class TerminalCommandTest {
    @Test fun `plain text is not a command`() { assertNull(parseCommand("spotify")) }
    @Test fun `open parses arguments`() { assertEquals(TerminalCommand.Open("Google Maps"), parseCommand(">open Google Maps")) }
    @Test fun `theme parses case insensitive`() { assertEquals(TerminalCommand.Theme(ThemeProfile.AMBER), parseCommand(">THEME amber")) }
    @Test fun `invalid theme rejected`() { assertEquals(TerminalCommand.Invalid, parseCommand(">theme blue")) }
    @Test fun `empty open rejected`() { assertEquals(TerminalCommand.Invalid, parseCommand(">open")) }
    @Test fun `builtins`() {
        assertEquals(TerminalCommand.Help, parseCommand(">help"))
        assertEquals(TerminalCommand.Apps, parseCommand(">apps"))
        assertEquals(TerminalCommand.Battery, parseCommand(">battery"))
        assertEquals(TerminalCommand.Storage, parseCommand(">storage"))
        assertEquals(TerminalCommand.Settings, parseCommand(">settings"))
    }
}
