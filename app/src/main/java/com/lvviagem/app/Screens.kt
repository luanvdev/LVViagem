package com.lvviagem.app

import android.app.Activity
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun LvApp(hasAccess: Boolean, onRequestAccess: () -> Unit, music: MusicController) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (!hasAccess) PermissionScreen(onRequestAccess) else MainContent(music)
    }
}

@Composable
fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("LV Viagem", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Lv.Accent)
        Spacer(Modifier.size(16.dp))
        Text(
            "Para mostrar seus filmes e músicas, o app precisa de acesso aos arquivos do celular (pasta LVviagem).",
            textAlign = TextAlign.Center,
            color = Lv.Muted,
        )
        Spacer(Modifier.size(24.dp))
        Button(onClick = onRequest) { Text("Permitir acesso") }
    }
}

@Composable
fun MainContent(music: MusicController) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var movie by remember { mutableStateOf<LvItem?>(null) }
    var library by remember { mutableStateOf<Library?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload) {
        library = withContext(Dispatchers.IO) { LvStorage.load() }
    }

    val playing = movie
    if (playing != null) {
        VideoPlayerScreen(playing) { movie = null }
    } else {
        Scaffold(
            containerColor = Lv.Bg,
            bottomBar = {
                Column {
                    if (music.currentIndex >= 0) MiniPlayer(music)
                    NavigationBar(containerColor = Lv.Surface) {
                        listOf("🎬" to "Filmes", "🎵" to "Músicas", "🎮" to "Jogos").forEachIndexed { i, (icon, label) ->
                            NavigationBarItem(
                                selected = tab == i,
                                onClick = { tab = i },
                                icon = { Text(icon, fontSize = 20.sp) },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            },
        ) { pad ->
            Column(Modifier.padding(pad).fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("LV Viagem", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Lv.Accent, modifier = Modifier.weight(1f))
                    if (tab != 2) TextButton(onClick = { reload++ }) { Text("Atualizar") }
                }
                Box(Modifier.weight(1f).fillMaxSize()) {
                    val lib = library
                    if (tab == 2) {
                        GamesTab()
                    } else if (lib == null) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    } else if (tab == 0) {
                        MoviesTab(lib) { music.pause(); movie = it }
                    } else {
                        MusicTab(lib, music)
                    }
                }
            }
        }
    }
}

@Composable
fun Cover(file: File?, emoji: String, modifier: Modifier) {
    if (file != null) {
        AsyncImage(model = file, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        Box(modifier.background(Lv.Surface), contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = 28.sp)
        }
    }
}

@Composable
fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Lv.Muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun MoviesTab(lib: Library, onOpen: (LvItem) -> Unit) {
    if (!lib.rootFound) {
        EmptyState("Pasta não encontrada.\nCrie /storage/emulated/0/LVviagem/filmes e toque em Atualizar.")
    } else if (lib.movies.isEmpty()) {
        EmptyState("Nenhum filme encontrado.\nCrie uma pasta para cada filme dentro de LVviagem/filmes, com um .mp4 e um .jpg.")
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(140.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(lib.movies) { m ->
                Column(Modifier.clickable { onOpen(m) }) {
                    Cover(m.cover, "🎬", Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(8.dp)))
                    Spacer(Modifier.size(6.dp))
                    Text(m.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun MusicTab(lib: Library, music: MusicController) {
    if (!lib.rootFound) {
        EmptyState("Pasta não encontrada.\nCrie /storage/emulated/0/LVviagem/musicas e toque em Atualizar.")
    } else if (lib.music.isEmpty()) {
        EmptyState("Nenhuma música encontrada.\nCrie uma pasta para cada música dentro de LVviagem/musicas, com um .mp3 e um .jpg.")
    } else {
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            itemsIndexed(lib.music) { i, m ->
                val current = music.items == lib.music && music.currentIndex == i
                Row(
                    Modifier.fillMaxWidth().clickable { music.play(lib.music, i) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(m.cover, "🎵", Modifier.size(56.dp).clip(RoundedCornerShape(6.dp)))
                    Spacer(Modifier.size(14.dp))
                    Text(
                        m.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (current) Lv.Accent else Lv.Text,
                        fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
fun MiniPlayer(music: MusicController) {
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(music.currentIndex) {
        while (true) {
            val d = music.player.duration
            progress = if (d > 0) (music.player.currentPosition.toFloat() / d).coerceIn(0f, 1f) else 0f
            delay(500)
        }
    }
    val item = music.items.getOrNull(music.currentIndex)
    Column(Modifier.fillMaxWidth().background(Color(0xFF1D3248))) {
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = Lv.Accent)
        Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(item?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            TextButton(onClick = { music.previous() }) { Text("⏮", fontSize = 20.sp) }
            TextButton(onClick = { music.toggle() }) { Text(if (music.isPlaying) "⏸" else "▶", fontSize = 20.sp) }
            TextButton(onClick = { music.next() }) { Text("⏭", fontSize = 20.sp) }
        }
    }
}

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
    DisposableEffect(Unit) { onDispose { exo.release() } }
    BackHandler(onBack = onClose)

    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { player = exo } },
            modifier = Modifier.fillMaxSize(),
        )
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
            Text("← Voltar", color = Color.White)
        }
    }
}

@Composable
fun GamesTab() {
    var game by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = game != null) { game = null }

    Column(Modifier.fillMaxSize()) {
        if (game != null) {
            TextButton(onClick = { game = null }, modifier = Modifier.padding(horizontal = 8.dp)) { Text("← Jogos") }
        }
        when (game) {
            "snake" -> SnakeGame()
            "velha" -> TicTacToeGame()
            else -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GameCard("🐍", "Snake", "Deslize o dedo para guiar a cobra.") { game = "snake" }
                GameCard("⭕", "Jogo da Velha", "Você (X) contra o celular (O).") { game = "velha" }
            }
        }
    }
}

@Composable
fun GameCard(emoji: String, name: String, description: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Lv.Surface).clickable(onClick = onClick).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 32.sp)
        Spacer(Modifier.size(16.dp))
        Column {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(description, color = Lv.Muted, fontSize = 14.sp)
        }
    }
}
