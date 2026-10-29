package dev.pipos.launcher.ui

import dev.pipos.launcher.data.LaunchableApp

enum class AppCategory { ALL, COMM, MEDIA, TOOLS, GAMES }

fun categoryFor(app: LaunchableApp): AppCategory {
    val name = "${app.label} ${app.packageName}".lowercase()
    return when {
        listOf("game", "play.games", "minecraft", "steam").any { name.contains(it) } -> AppCategory.GAMES
        listOf("music", "spotify", "youtube", "vlc", "photo", "camera", "podcast", "video").any { name.contains(it) } -> AppCategory.MEDIA
        listOf("message", "signal", "telegram", "discord", "whatsapp", "phone", "contacts", "gmail", "mail").any { name.contains(it) } -> AppCategory.COMM
        else -> AppCategory.TOOLS
    }
}
