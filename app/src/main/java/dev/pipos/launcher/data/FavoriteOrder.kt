package dev.pipos.launcher.data

/** Pure ordering operations; shared by the launcher UI and unit tests. */
object FavoriteOrder {
    fun reconcile(saved: List<String>, legacy: Set<String>): List<String> =
        saved.distinct().filter { it in legacy } + (legacy - saved.toSet()).sorted()

    fun toggle(current: List<String>, key: String): List<String> =
        if (key in current) current - key else current + key

    /** Key-based reordering also works when some favorites are hidden from the UI. */
    fun moveByKey(current: List<String>, dragged: String, target: String): List<String> {
        val from = current.indexOf(dragged)
        val to = current.indexOf(target)
        return move(current, from, to)
    }

    fun move(current: List<String>, from: Int, to: Int): List<String> {
        if (from !in current.indices || to !in current.indices || from == to) return current
        return current.toMutableList().apply { add(to, removeAt(from)) }
    }
}
