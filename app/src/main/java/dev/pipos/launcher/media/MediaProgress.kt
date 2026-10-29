package dev.pipos.launcher.media

/** Android media positions are measured in milliseconds using elapsedRealtime. */
object MediaProgress {
    fun position(
        lastPositionMs: Long,
        updatedAtMs: Long,
        nowMs: Long,
        speed: Float,
        playing: Boolean,
        durationMs: Long
    ): Long {
        if (durationMs <= 0L) return 0L
        val advanced = if (playing && updatedAtMs > 0L) {
            ((nowMs - updatedAtMs).coerceAtLeast(0L) * speed.coerceAtLeast(0f)).toLong()
        } else 0L
        return (lastPositionMs.coerceAtLeast(0L) + advanced).coerceIn(0L, durationMs)
    }

    fun timeCode(ms: Long): String {
        val total = ms.coerceAtLeast(0L) / 1000
        return "%d:%02d".format(java.util.Locale.US, total / 60, total % 60)
    }
}
