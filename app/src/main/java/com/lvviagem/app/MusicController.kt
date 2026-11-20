package com.lvviagem.app

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class MusicController(context: Context) {
    val player: ExoPlayer = ExoPlayer.Builder(context).build()
    var items by mutableStateOf<List<LvItem>>(emptyList())
        private set
    var currentIndex by mutableIntStateOf(-1)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var shuffle by mutableStateOf(false)
        private set
    var repeat by mutableIntStateOf(Player.REPEAT_MODE_ALL)
        private set

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentIndex = player.currentMediaItemIndex
            }
            override fun onShuffleModeEnabledChanged(enabled: Boolean) { shuffle = enabled }
            override fun onRepeatModeChanged(repeatMode: Int) { repeat = repeatMode }
        })
        player.repeatMode = Player.REPEAT_MODE_ALL
    }

    fun play(list: List<LvItem>, index: Int) {
        if (items != list) {
            items = list
            player.setMediaItems(list.map { MediaItem.fromUri(Uri.fromFile(it.media)) })
        }
        currentIndex = index
        player.seekTo(index, 0)
        player.prepare()
        player.play()
    }

    fun toggle() { if (player.isPlaying) player.pause() else player.play() }
    fun pause() { player.pause() }
    fun next() { player.seekToNextMediaItem() }
    fun previous() { player.seekToPreviousMediaItem() }
    fun toggleShuffle() { player.shuffleModeEnabled = !player.shuffleModeEnabled }
    fun cycleRepeat() {
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
            else -> Player.REPEAT_MODE_ALL
        }
    }
    fun release() { player.release() }
}
