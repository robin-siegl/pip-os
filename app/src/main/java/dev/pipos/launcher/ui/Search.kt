package dev.pipos.launcher.ui

import dev.pipos.launcher.data.LaunchableApp

fun filterApps(apps: List<LaunchableApp>, query: String): List<LaunchableApp> {
    val term = query.trim().removePrefix(">").trim()
        .removePrefix("open ").trim()
    if (term.isBlank()) return apps
    return apps.filter { it.label.contains(term, ignoreCase = true) || it.packageName.contains(term, ignoreCase = true) }
}
