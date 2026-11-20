package com.lvviagem.app

import android.os.Environment
import java.io.File
import java.text.Normalizer

data class LvItem(val title: String, val media: File, val cover: File?)
data class LvGame(val title: String, val html: File, val cover: File?)
data class Library(val rootFound: Boolean, val movies: List<LvItem>, val music: List<LvItem>, val games: List<LvGame>)

/**
 * Estrutura esperada:
 *   /storage/emulated/0/LVviagem/filmes/<pasta do filme>/qualquer.mp4 + qualquer.jpg
 *   /storage/emulated/0/LVviagem/musicas/<pasta da musica>/qualquer.mp3 + qualquer.jpg
 *   /storage/emulated/0/LVviagem/jogos/<pasta do jogo>/index.html (+ arquivos do jogo)
 * Os nomes dos arquivos são livres: só a extensão importa.
 */
object LvStorage {
    private val VIDEO = setOf("mp4", "mkv", "webm", "m4v")
    private val AUDIO = setOf("mp3", "m4a", "ogg", "wav", "flac")
    private val IMAGE = setOf("jpg", "jpeg", "png")

    private fun norm(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

    fun load(): Library {
        val base = Environment.getExternalStorageDirectory()
        val root = File(base, "LVviagem").takeIf { it.isDirectory }
            ?: base.listFiles()?.firstOrNull { it.isDirectory && norm(it.name).replace(" ", "") == "lvviagem" }
            ?: File(base, "LVviagem").also { it.mkdirs() }
        if (!root.isDirectory) return Library(false, emptyList(), emptyList(), emptyList())
        val movieDir = sub(root, "film", "movie", "video") ?: File(root, "filmes").also { it.mkdirs() }
        val musicDir = sub(root, "music", "song") ?: File(root, "musicas").also { it.mkdirs() }
        val gameDir = sub(root, "jog", "game") ?: File(root, "jogos").also { it.mkdirs() }
        return Library(true, scan(movieDir, VIDEO), scan(musicDir, AUDIO), scanGames(gameDir))
    }

    private fun sub(root: File, vararg prefixes: String): File? =
        root.listFiles()?.firstOrNull { f -> f.isDirectory && prefixes.any { norm(f.name).startsWith(it) } }

    private fun scanGames(dir: File?): List<LvGame> {
        val entries = dir?.listFiles()?.sortedBy { it.name.lowercase() } ?: return emptyList()
        val htmlExt = setOf("html", "htm")
        return entries.mapNotNull { e ->
            if (e.isDirectory) {
                val files = e.listFiles()?.filter { it.isFile } ?: return@mapNotNull null
                val html = files.firstOrNull { it.name.equals("index.html", ignoreCase = true) }
                    ?: files.firstOrNull { it.extension.lowercase() in htmlExt }
                    ?: return@mapNotNull null
                LvGame(e.name, html, files.firstOrNull { it.extension.lowercase() in IMAGE })
            } else if (e.isFile && e.extension.lowercase() in htmlExt) {
                LvGame(e.nameWithoutExtension, e, null)
            } else null
        }
    }

    private fun scan(dir: File?, exts: Set<String>): List<LvItem> {
        val folders = dir?.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name.lowercase() }
            ?: return emptyList()
        return folders.flatMap { folder ->
            val files = folder.listFiles()?.filter { it.isFile && !it.name.startsWith(".") } ?: emptyList()
            val medias = files.filter { it.extension.lowercase() in exts }.sortedBy { it.name.lowercase() }
            val cover = files.firstOrNull { it.extension.lowercase() in IMAGE }
            medias.map { m -> LvItem(if (medias.size == 1) folder.name else m.nameWithoutExtension, m, cover) }
        }
    }
}
