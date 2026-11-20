package com.lvviagem.app

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

fun fmt(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

@Composable
fun MiniPlayer(music: MusicController, onOpen: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(music.currentIndex) {
        while (true) {
            val d = music.player.duration
            progress = if (d > 0) (music.player.currentPosition.toFloat() / d).coerceIn(0f, 1f) else 0f
            delay(500)
        }
    }
    val item = music.items.getOrNull(music.currentIndex)
    Column(Modifier.fillMaxWidth().background(Color(0xFF1D3248)).clickable(onClick = onOpen)) {
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = Lv.Accent)
        Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(item?.cover, "🎵", Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)))
            Spacer(Modifier.size(10.dp))
            Text(item?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            TextButton(onClick = { music.previous() }) { Text("⏮", fontSize = 20.sp) }
            TextButton(onClick = { music.toggle() }) { Text(if (music.isPlaying) "⏸" else "▶", fontSize = 20.sp) }
            TextButton(onClick = { music.next() }) { Text("⏭", fontSize = 20.sp) }
        }
    }
}

@Composable
fun NowPlayingScreen(music: MusicController, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val item = music.items.getOrNull(music.currentIndex)
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            position = music.player.currentPosition
            val d = music.player.duration
            duration = if (d > 0) d else 0L
            delay(300)
        }
    }

    val shown = dragging ?: if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val elapsed = dragging?.let { (it * duration).toLong() } ?: position

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF2B4A68), Lv.Bg)))
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onClose) { Text("▾", fontSize = 28.sp) }
            Text("Tocando agora", color = Lv.Muted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Spacer(Modifier.size(48.dp))
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            Cover(
                item?.cover, "🎵",
                Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(1f)
                    .shadow(24.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp)),
            )
        }
        Text(
            item?.title ?: "",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.size(12.dp))
        Slider(
            value = shown,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { music.player.seekTo((it * duration).toLong()) }
                dragging = null
            },
            colors = SliderDefaults.colors(
                thumbColor = Lv.Accent,
                activeTrackColor = Lv.Accent,
                inactiveTrackColor = Color(0x44FFFFFF),
            ),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmt(elapsed), color = Lv.Muted, fontSize = 13.sp)
            Text(fmt(duration), color = Lv.Muted, fontSize = 13.sp)
        }
        Spacer(Modifier.size(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { music.toggleShuffle() }) {
                Text("🔀", fontSize = 22.sp, modifier = Modifier.alpha(if (music.shuffle) 1f else 0.4f))
            }
            TextButton(onClick = { music.previous() }) { Text("⏮", fontSize = 30.sp) }
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(Lv.Accent).clickable { music.toggle() },
                contentAlignment = Alignment.Center,
            ) {
                Text(if (music.isPlaying) "⏸" else "▶", fontSize = 30.sp, color = Color(0xFF1A1200))
            }
            TextButton(onClick = { music.next() }) { Text("⏭", fontSize = 30.sp) }
            TextButton(onClick = { music.cycleRepeat() }) {
                Text(
                    if (music.repeat == Player.REPEAT_MODE_ONE) "🔂" else "🔁",
                    fontSize = 22.sp,
                    modifier = Modifier.alpha(if (music.repeat == Player.REPEAT_MODE_OFF) 0.4f else 1f),
                )
            }
        }
        Spacer(Modifier.size(20.dp))
    }
}

private val SPEEDS = listOf(1f, 1.25f, 1.5f, 2f, 0.75f)

@Composable
fun VideoPlayerScreen(item: LvItem, onClose: () -> Unit) {
    val context = LocalContext.current
    val exo = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(item.media)))
            prepare()
            playWhenReady = true
        }
    }
    var isPlaying by remember { mutableStateOf(true) }
    var buffering by remember { mutableStateOf(true) }
    var ended by remember { mutableStateOf(false) }
    var controls by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var lockHint by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf<Float?>(null) }
    var speedIdx by remember { mutableIntStateOf(0) }
    var hint by remember { mutableStateOf<String?>(null) }
    var hintKey by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }

    DisposableEffect(exo) {
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                ended = state == Player.STATE_ENDED
            }
        }
        exo.addListener(l)
        onDispose {
            exo.removeListener(l)
            exo.release()
        }
    }

    LaunchedEffect(exo) {
        while (true) {
            position = exo.currentPosition
            val d = exo.duration
            if (d > 0) duration = d
            delay(300)
        }
    }
    LaunchedEffect(controls, isPlaying, tick, locked, dragging != null) {
        if (controls && isPlaying && !locked && dragging == null) {
            delay(3500)
            controls = false
        }
    }
    LaunchedEffect(lockHint, tick) {
        if (lockHint) {
            delay(2500)
            lockHint = false
        }
    }
    LaunchedEffect(hintKey) {
        if (hint != null) {
            delay(700)
            hint = null
        }
    }

    BackHandler {
        if (locked) { lockHint = true; tick++ } else onClose()
    }

    DisposableEffect(Unit) {
        val activity = context as? Activity
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    fun seekBy(ms: Long) {
        val d = exo.duration
        val max = if (d > 0) d else Long.MAX_VALUE
        val target = (exo.currentPosition + ms).coerceIn(0L, max)
        exo.seekTo(target)
        position = target
    }

    val shown = dragging ?: if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val elapsed = dragging?.let { (it * duration).toLong() } ?: position
    val showControls = controls && !locked

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exo
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Toque: mostra/esconde controles. Toque duplo: pula 10 s para o lado tocado.
        Box(
            Modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (locked) {
                            lockHint = true
                        } else {
                            controls = !controls
                        }
                        tick++
                    },
                    onDoubleTap = { off ->
                        if (!locked) {
                            if (off.x < size.width / 2) {
                                seekBy(-10_000L)
                                hint = "−10 s"
                            } else {
                                seekBy(10_000L)
                                hint = "+10 s"
                            }
                            hintKey++
                            tick++
                        }
                    },
                )
            },
        )

        if (showControls) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))) {
                Row(
                    Modifier.align(Alignment.TopStart).fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onClose) { Text("←", fontSize = 26.sp, color = Color.White) }
                    Text(
                        item.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { seekBy(-10_000L); tick++ }) {
                        Text("↺ 10", fontSize = 22.sp, color = Color.White)
                    }
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable {
                                if (ended) {
                                    exo.seekTo(0)
                                    exo.play()
                                } else if (isPlaying) {
                                    exo.pause()
                                } else {
                                    exo.play()
                                }
                                tick++
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (isPlaying && !ended) "⏸" else "▶", fontSize = 32.sp, color = Color.White)
                    }
                    TextButton(onClick = { seekBy(10_000L); tick++ }) {
                        Text("10 ↻", fontSize = 22.sp, color = Color.White)
                    }
                }
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                ) {
                    Slider(
                        value = shown,
                        onValueChange = { dragging = it },
                        onValueChangeFinished = {
                            dragging?.let {
                                val target = (it * duration).toLong()
                                exo.seekTo(target)
                                position = target
                            }
                            dragging = null
                            tick++
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Lv.Accent,
                            activeTrackColor = Lv.Accent,
                            inactiveTrackColor = Color(0x55FFFFFF),
                        ),
                    )
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(fmt(elapsed), color = Color.White, fontSize = 13.sp)
                        Text(fmt(duration), color = Color.White, fontSize = 13.sp)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { locked = true; controls = false; lockHint = false; tick++ }) {
                            Text("🔒 Bloquear", color = Color.White)
                        }
                        TextButton(onClick = {
                            speedIdx = (speedIdx + 1) % SPEEDS.size
                            exo.setPlaybackSpeed(SPEEDS[speedIdx])
                            tick++
                        }) {
                            val v = SPEEDS[speedIdx]
                            Text("Velocidade ${if (v == v.toInt().toFloat()) v.toInt().toString() else v.toString()}x", color = Color.White)
                        }
                    }
                }
            }
        }

        if (buffering && !ended && !showControls) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Lv.Accent)
        }

        hint?.let {
            Text(
                it,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }

        if (locked && lockHint) {
            Button(
                onClick = { locked = false; lockHint = false; controls = true; tick++ },
                modifier = Modifier.align(Alignment.CenterEnd).padding(24.dp),
            ) { Text("🔓 Desbloquear") }
        }
    }
}
