package dev.pipos.launcher.media

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** No notification data is persisted or uploaded; this service only allows session discovery. */
class PipNotificationListener : NotificationListenerService()

private data class PlayingSession(
    val controller: MediaController,
    val title: String,
    val artist: String,
    val artwork: Bitmap?,
    val duration: Long,
    val position: Long,
    val updatedAt: Long,
    val speed: Float,
    val playing: Boolean,
    val canSeek: Boolean,
    val canSkipNext: Boolean,
    val canSkipPrevious: Boolean
)

private fun MediaController.snapshot(): PlayingSession {
    val metadata = metadata
    val state = playbackState
    val actions = state?.actions ?: 0L
    return PlayingSession(
        controller = this,
        title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: "UNKNOWN TRANSMISSION",
        artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: packageName,
        artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON),
        duration = (metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L).coerceAtLeast(0L),
        position = state?.position ?: 0L,
        updatedAt = state?.lastPositionUpdateTime ?: 0L,
        speed = state?.playbackSpeed ?: 0f,
        playing = state?.state == PlaybackState.STATE_PLAYING,
        canSeek = actions and PlaybackState.ACTION_SEEK_TO != 0L,
        canSkipNext = actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L,
        canSkipPrevious = actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L
    )
}

private fun hasMediaAccess(context: Context): Boolean {
    val manager = context.getSystemService(NotificationManager::class.java)
    return manager?.isNotificationListenerAccessGranted(ComponentName(context, PipNotificationListener::class.java)) == true
}

/**
 * Registers for media session and playback updates only while RADIO is visible.
 * The only timer below interpolates position once per second while audio is playing.
 */
@Composable
fun MediaControllerPanel(accent: Color) {
    val context = LocalContext.current
    var accessGranted by remember { mutableStateOf(hasMediaAccess(context)) }
    var sessions by remember { mutableStateOf<List<PlayingSession>>(emptyList()) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(context, lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) accessGranted = hasMediaAccess(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(context, accessGranted) {
        if (!accessGranted) {
            sessions = emptyList()
            onDispose { }
        } else {
            val manager = context.getSystemService(MediaSessionManager::class.java)
            val listenerComponent = ComponentName(context, PipNotificationListener::class.java)
            val handler = Handler(Looper.getMainLooper())
            val callbacks = mutableMapOf<MediaController, MediaController.Callback>()
            fun update() {
                sessions = callbacks.keys.mapNotNull { controller ->
                    runCatching { controller.snapshot() }.getOrNull()
                }.sortedByDescending { it.playing }
            }
            fun attach(controllers: List<MediaController>?) {
                callbacks.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
                callbacks.clear()
                controllers.orEmpty().forEach { controller ->
                    val callback = object : MediaController.Callback() {
                        override fun onPlaybackStateChanged(state: PlaybackState?) = update()
                        override fun onMetadataChanged(metadata: MediaMetadata?) = update()
                        override fun onSessionDestroyed() = update()
                    }
                    controller.registerCallback(callback, handler)
                    callbacks[controller] = callback
                }
                update()
            }
            val listener = MediaSessionManager.OnActiveSessionsChangedListener { attach(it) }
            val subscribed = runCatching {
                manager?.addOnActiveSessionsChangedListener(listener, listenerComponent, handler)
                attach(manager?.getActiveSessions(listenerComponent))
                true
            }.getOrElse {
                sessions = emptyList()
                false
            }
            onDispose {
                if (subscribed) runCatching { manager?.removeOnActiveSessionsChangedListener(listener) }
                callbacks.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
                callbacks.clear()
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("ACTIVE FREQUENCY", color = accent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        if (!accessGranted) {
            Text("MEDIA ACCESS DISABLED. ENABLE IT TO CONTROL OTHER PLAYERS.", color = accent.copy(alpha = .75f), fontFamily = FontFamily.Monospace)
            MediaAction("OPEN ACCESS SETTINGS", accent, true) {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        } else if (sessions.isEmpty()) {
            Text("NO ACTIVE TRANSMISSION", color = accent.copy(alpha = .7f), fontFamily = FontFamily.Monospace)
        }
        sessions.forEach { session ->
            key(session.controller.sessionToken) { MediaSessionCard(session, accent) }
        }
    }
}

@Composable
private fun MediaSessionCard(session: PlayingSession, accent: Color) {
    var clock by remember(session.controller.sessionToken) { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var draggingPosition by remember(session.controller.sessionToken) { mutableStateOf<Float?>(null) }
    LaunchedEffect(session.playing, session.updatedAt, session.controller.sessionToken) {
        clock = SystemClock.elapsedRealtime()
        while (session.playing) {
            delay(1000L)
            clock = SystemClock.elapsedRealtime()
        }
    }
    val position = MediaProgress.position(session.position, session.updatedAt, clock, session.speed, session.playing, session.duration)
    val progress = draggingPosition ?: position.toFloat()
    Column(Modifier.fillMaxWidth().background(accent.copy(alpha = .065f)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(90.dp).background(accent.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                val artwork = session.artwork
                if (artwork != null && !artwork.isRecycled) {
                    Image(bitmap = artwork.asImageBitmap(), contentDescription = "Album artwork for ${session.title}", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Canvas(Modifier.fillMaxSize().padding(17.dp)) {
                        val r = size.minDimension * .42f
                        drawCircle(accent, r, style = Stroke(width = 3f))
                        drawCircle(accent, r * .16f)
                        drawLine(accent, Offset(center.x, center.y - r), Offset(center.x, center.y - r * .68f), 2f)
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Text(session.title, color = accent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(session.artist, color = accent.copy(alpha = .7f), fontFamily = FontFamily.Monospace, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if (session.playing) "● SIGNAL RECEIVED" else "○ TRANSMISSION PAUSED", color = accent.copy(alpha = .7f), fontFamily = FontFamily.Monospace)
            }
        }
        if (session.duration > 0) {
            Slider(
                value = progress.coerceIn(0f, session.duration.toFloat()),
                onValueChange = { draggingPosition = it },
                onValueChangeFinished = {
                    draggingPosition?.let { if (session.canSeek) session.controller.transportControls.seekTo(it.toLong()) }
                    draggingPosition = null
                },
                valueRange = 0f..session.duration.toFloat(),
                enabled = session.canSeek,
                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = accent.copy(alpha = .2f)),
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(MediaProgress.timeCode(progress.toLong()), color = accent.copy(alpha = .8f), fontFamily = FontFamily.Monospace)
                Text(MediaProgress.timeCode(session.duration), color = accent.copy(alpha = .8f), fontFamily = FontFamily.Monospace)
            }
        } else {
            Text("POSITION UNAVAILABLE", color = accent.copy(alpha = .65f), fontFamily = FontFamily.Monospace)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { MediaAction("PREV", accent, session.canSkipPrevious) { session.controller.transportControls.skipToPrevious() } }
            Box(Modifier.weight(1.3f)) {
                MediaAction(if (session.playing) "PAUSE" else "PLAY", accent, true) {
                    if (session.playing) session.controller.transportControls.pause() else session.controller.transportControls.play()
                }
            }
            Box(Modifier.weight(1f)) { MediaAction("NEXT", accent, session.canSkipNext) { session.controller.transportControls.skipToNext() } }
        }
    }
}

@Composable
private fun MediaAction(label: String, accent: Color, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = .16f), contentColor = accent,
            disabledContainerColor = accent.copy(alpha = .05f), disabledContentColor = accent.copy(alpha = .35f)),
        contentPadding = PaddingValues(horizontal = 4.dp)) {
        Text(label, fontFamily = FontFamily.Monospace)
    }
}
