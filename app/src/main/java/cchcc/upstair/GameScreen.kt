package cchcc.upstair

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlin.random.Random
import kotlin.random.nextInt

private val bgColor = Color.Red
private const val col = 6

@Composable
fun GameScreen() {
    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val (screenW, screenH) = with(density) { containerSize.width.toDp() to containerSize.height.toDp() }
    val game = remember {
        val cy = (stageHeight(screenH) / blockWidth(screenW, col)).toInt()
        Game(cx = col, cy = cy)
    }
    var score: Int by remember { mutableIntStateOf(game.score) }
    var stage: ImmutableList<ImmutableList<Block>> by remember { mutableStateOf(game.immutableStage()) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        GameStage(screenW = screenW, screenH = screenH, stage = stage)
        GameUi(
            screenW = screenW,
            screenH = screenH,
            score = score.toString(),
            onClickLeft = {
                if (game.canMoveLeft()) {
                    game.moveLeft()
                } else {
                    game.resetStage()
                }
                score = game.score
                stage = game.immutableStage()
            },
            onClickRight = {
                if (game.canMoveRight()) {
                    game.moveRight()
                } else {
                    game.resetStage()
                }
                score = game.score
                stage = game.immutableStage()
            }
        )
    }
}

@Composable
private fun BoxScope.GameStage(
    screenW: Dp,
    screenH: Dp,
    stage: ImmutableList<ImmutableList<Block>>,
) {
    val stageW = stageWidth(screenW)
    val stageH = stageHeight(screenH)
    val stageOffsetY = screenW * 0.25f * -1
    val blockWidth = blockWidth(screenW, col)
    Box(
        modifier = Modifier
            .offset(y = stageOffsetY)
            .size(stageW, stageH)
//            .background(Color.Green)
            .align(Alignment.Center),
    ) {
        for (y in stage.indices) {
            for (x in stage[y].indices) {
                BlockUnit(stage[y][x], blockWidth, x, y)
            }
        }
    }
}

private fun stageWidth(screenW: Dp): Dp = screenW * 0.7f
private fun stageHeight(screenH: Dp): Dp = screenH * 0.6f
private fun blockWidth(screenW: Dp, col: Int): Dp = stageWidth(screenW) / col

@Composable
private fun BlockUnit(block: Block, blockWidth: Dp, x: Int, y: Int) {
    val offsetX = blockWidth * x
    val offsetY = blockWidth * y
    Canvas(Modifier.size(blockWidth).offset(offsetX, offsetY)) {
        if (block.character) {
            val radius = ((blockWidth - 13.dp) / 2f).toPx()
            val center = Offset(blockWidth.toPx() / 2, blockWidth.toPx() / 2 - (5.dp).toPx())
            drawCircle(Color.Yellow, radius, center)
        }
        if (block.board) {
            val height = 10.dp
            val topLeft = Offset(0f, blockWidth.toPx() - height.toPx())
            val size = Size(blockWidth.toPx(), height.toPx())
            drawRect(Color.Black, topLeft, size)
        }
    }
}

@Composable
private fun BoxScope.GameUi(
    screenW: Dp,
    screenH: Dp,
    score: String,
    onClickLeft: () -> Unit,
    onClickRight: () -> Unit,
) {
    val uiW = stageWidth(screenW)
    Column(
        modifier = Modifier
            .width(uiW)
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(text = score, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .border(
                        width = (0.5).dp,
                        color = Color.Black,
                        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                    )
                    .clickable { onClickLeft() },
                text = "<",
                textAlign = TextAlign.Center,
                fontSize = 80.sp,
            )
            Text(
                modifier = Modifier
                    .weight(1f)
                    .border(
                        width = (0.5).dp,
                        color = Color.Black,
                        shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                    )
                    .clickable { onClickRight() },
                text = ">",
                textAlign = TextAlign.Center,
                fontSize = 80.sp,
            )
        }
    }
}

@Immutable
private data class Block(val board: Boolean, val character: Boolean)

private fun Game.immutableStage(): ImmutableList<ImmutableList<Block>> {
    return stage.map { it.toImmutableList() }.toImmutableList()
}

private class Game(
    val cx: Int,
    val cy: Int,
) {
    var score = 0
    lateinit var stage: Array<Array<Block>>
    private var characterX = 0
    private val characterY: Int
        get() = cy - 3
    private val random = Random(System.currentTimeMillis())

    init {
        resetStage()
    }

    private fun nextBoardIdx(y: Int): Int {
        val lastBoardIdx = stage[y + 1].indexOfFirst { it.board }
        val nextBoardIdx = when (lastBoardIdx) {
            0 -> 1
            cx - 1 -> cx - 2
            else -> lastBoardIdx + if (random.nextBoolean()) 1 else -1
        }
        return nextBoardIdx
    }

    fun resetStage() {
        stage = Array(cy) { Array(cx) { Block(board = false, character = false) } }
        val randomOffset = random.nextInt(0..1)
        characterX = cx / 2 + randomOffset
        stage[characterY][characterX] = Block(board = true, character = true)
        ((characterY - 1) downTo 0).forEach { y ->
            val nextBoardIdx = nextBoardIdx(y)
            stage[y][nextBoardIdx] = Block(board = true, character = false)
        }
        score = 0
    }

    fun canMoveRight(): Boolean = characterX != cx - 1 && stage[characterY - 1][characterX + 1].board

    fun canMoveLeft(): Boolean = characterX != 0 && stage[characterY - 1][characterX - 1].board

    private fun move(direction: Int) {
        stage[characterY - 1][characterX + direction] = Block(board = true, character = true)
        stage[characterY][characterX] = Block(board = true, character = false)
        (stage.lastIndex downTo 1).forEach { stage[it] = stage[it - 1] }
        val nextBoardIdx = nextBoardIdx(0)
        stage[0] = Array(cx) { Block(board = nextBoardIdx == it, character = false) }
        characterX += direction
        score++
    }

    fun moveRight() = move(1)

    fun moveLeft() = move(-1)
}

