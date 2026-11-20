package com.lvviagem.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

data class P(val x: Int, val y: Int)

private const val GRID = 16
private val START_SNAKE = listOf(P(8, 8), P(7, 8), P(6, 8))

@Composable
fun SnakeGame() {
    var snake by remember { mutableStateOf(START_SNAKE) }
    var dir by remember { mutableStateOf(P(1, 0)) }
    var pending by remember { mutableStateOf(P(1, 0)) }
    var food by remember { mutableStateOf(P(12, 8)) }
    var alive by remember { mutableStateOf(true) }
    var started by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }

    LaunchedEffect(started, alive) {
        while (started && alive) {
            delay(140)
            dir = pending
            val head = snake.first()
            val nh = P(head.x + dir.x, head.y + dir.y)
            if (nh.x !in 0 until GRID || nh.y !in 0 until GRID || nh in snake.dropLast(1)) {
                alive = false
            } else {
                val ate = nh == food
                val ns = listOf(nh) + if (ate) snake else snake.dropLast(1)
                snake = ns
                if (ate) {
                    score++
                    val free = (0 until GRID).flatMap { x -> (0 until GRID).map { y -> P(x, y) } }.filter { it !in ns }
                    if (free.isNotEmpty()) food = free.random()
                }
            }
        }
    }

    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Pontos: $score", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(12.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Lv.Surface)
                .pointerInput(Unit) {
                    var total = Offset.Zero
                    detectDragGestures(
                        onDragStart = { total = Offset.Zero },
                        onDragEnd = {
                            val nd = if (abs(total.x) > abs(total.y)) {
                                if (total.x > 0) P(1, 0) else P(-1, 0)
                            } else {
                                if (total.y > 0) P(0, 1) else P(0, -1)
                            }
                            if (nd.x != -dir.x || nd.y != -dir.y) pending = nd
                            if (alive) started = true
                        },
                    ) { _, amount -> total += amount }
                },
        ) {
            val cell = size.width / GRID
            snake.forEachIndexed { i, p ->
                drawRoundRect(
                    color = if (i == 0) Lv.Accent else Color(0xFFC9923A),
                    topLeft = Offset(p.x * cell + 1f, p.y * cell + 1f),
                    size = Size(cell - 2f, cell - 2f),
                    cornerRadius = CornerRadius(cell / 4f),
                )
            }
            drawCircle(Color(0xFFE5655B), radius = cell / 2.5f, center = Offset(food.x * cell + cell / 2, food.y * cell + cell / 2))
        }
        Spacer(Modifier.size(16.dp))
        if (!alive) {
            Text("Fim de jogo", fontSize = 18.sp)
            Spacer(Modifier.size(8.dp))
            Button(onClick = {
                snake = START_SNAKE; dir = P(1, 0); pending = P(1, 0); food = P(12, 8)
                score = 0; alive = true; started = false
            }) { Text("Jogar de novo") }
        } else if (!started) {
            Button(onClick = { started = true }) { Text("Começar") }
        }
    }
}

private val LINES = listOf(
    listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
    listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
    listOf(0, 4, 8), listOf(2, 4, 6),
)

private fun winnerOf(b: List<String>): String? =
    LINES.firstOrNull { l -> b[l[0]].isNotEmpty() && b[l[0]] == b[l[1]] && b[l[1]] == b[l[2]] }?.let { b[it[0]] }

private fun findLine(b: List<String>, mark: String): Int? =
    LINES.firstNotNullOfOrNull { l ->
        val cells = l.map { b[it] }
        if (cells.count { it == mark } == 2 && cells.count { it.isEmpty() } == 1) l[cells.indexOf("")] else null
    }

private fun cpuMove(b: List<String>): Int =
    findLine(b, "O") ?: findLine(b, "X") ?: (if (b[4].isEmpty()) 4 else null)
        ?: b.indices.filter { b[it].isEmpty() }.random()

@Composable
fun TicTacToeGame() {
    var board by remember { mutableStateOf(List(9) { "" }) }
    val winner = winnerOf(board)
    val full = board.none { it.isEmpty() }

    fun tap(i: Int) {
        if (board[i].isNotEmpty() || winnerOf(board) != null) return
        val nb = board.toMutableList()
        nb[i] = "X"
        if (winnerOf(nb) == null && nb.any { it.isEmpty() }) nb[cpuMove(nb)] = "O"
        board = nb
    }

    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            when {
                winner == "X" -> "Você ganhou!"
                winner == "O" -> "O celular ganhou"
                full -> "Empate"
                else -> "Sua vez (X)"
            },
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.size(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (r in 0 until 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (c in 0 until 3) {
                        val i = r * 3 + c
                        Box(
                            Modifier.size(96.dp).clip(RoundedCornerShape(8.dp)).background(Lv.Surface).clickable { tap(i) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(board[i], fontSize = 44.sp, fontWeight = FontWeight.Bold, color = if (board[i] == "X") Lv.Accent else Lv.Text)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.size(20.dp))
        Button(onClick = { board = List(9) { "" } }) { Text("Novo jogo") }
    }
}
