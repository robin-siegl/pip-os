package dev.pipos.launcher.ui

import dev.pipos.launcher.data.ThemeProfile

sealed interface TerminalCommand {
    data class Open(val app: String) : TerminalCommand
    data class Theme(val profile: ThemeProfile) : TerminalCommand
    data object Battery : TerminalCommand
    data object Storage : TerminalCommand
    data object Apps : TerminalCommand
    data object Settings : TerminalCommand
    data object Help : TerminalCommand
    data object Invalid : TerminalCommand
}

fun parseCommand(input: String): TerminalCommand? {
    if (!input.trim().startsWith(">")) return null
    val parts = input.trim().removePrefix(">").trim().split(Regex("\\s+"), limit = 2)
    val argument = parts.getOrElse(1) { "" }.trim()
    return when (parts.firstOrNull()?.lowercase()) {
        "open" -> if (argument.isBlank()) TerminalCommand.Invalid else TerminalCommand.Open(argument)
        "battery" -> TerminalCommand.Battery
        "storage" -> TerminalCommand.Storage
        "apps" -> TerminalCommand.Apps
        "settings" -> TerminalCommand.Settings
        "help" -> TerminalCommand.Help
        "theme" -> ThemeProfile.entries.firstOrNull { it.name.equals(argument, true) }?.let(TerminalCommand::Theme) ?: TerminalCommand.Invalid
        else -> TerminalCommand.Invalid
    }
}
