package com.example.snakegame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random
import android.content.Context
import androidx.compose.runtime.saveable.rememberSaveable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SnakeGame()
                }
            }
        }
    }
}

@Composable
fun SnakeGame() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("snake_game", Context.MODE_PRIVATE) }

    // Game state variables with persistence
    var gameState by rememberSaveable { mutableStateOf(GameState.NOT_STARTED) }
    var score by rememberSaveable { mutableStateOf(0) }
    var highScore by remember {
        mutableStateOf(prefs.getInt("high_score", 0))
    }
    var level by rememberSaveable { mutableStateOf(1) }
    var snake by rememberSaveable { mutableStateOf(createInitialSnake()) }
    var food by rememberSaveable { mutableStateOf(generateFood(snake)) }
    var direction by rememberSaveable { mutableStateOf(Direction.RIGHT) }
    var nextDirection by rememberSaveable { mutableStateOf(Direction.RIGHT) }
    var isPaused by rememberSaveable { mutableStateOf(false) }

    // Save high score when it changes
    LaunchedEffect(highScore) {
        prefs.edit().putInt("high_score", highScore).apply()
    }

    // Game configuration
    val gridSize = 20
    val config = LocalConfiguration.current
    val cellSize: Dp = (config.screenWidthDp.dp - 40.dp) / gridSize

    // Game loop
    LaunchedEffect(key1 = gameState, key2 = isPaused) {
        if (gameState == GameState.PLAYING && !isPaused) {
            while (gameState == GameState.PLAYING && !isPaused) {
                delay(150 - (level * 10).coerceAtMost(100).toLong()) // Speed increases with level

                // Update direction
                direction = nextDirection

                // Move snake
                val newSnake = moveSnake(snake, direction, gridSize)

                // Check for collisions
                if (checkCollision(newSnake, gridSize)) {
                    gameState = GameState.GAME_OVER
                    if (score > highScore) {
                        highScore = score
                    }
                    continue
                }

                // Check if food eaten
                if (newSnake[0] == food) {
                    score += 10 * level
                    snake = newSnake + snake.last() // Grow snake
                    food = generateFood(snake)

                    // Check level completion
                    if (score >= level * 100) {
                        level++
                        gameState = GameState.LEVEL_COMPLETE
                    }
                } else {
                    snake = newSnake
                }
            }
        }
    }

    // UI based on game state
    when (gameState) {
        GameState.NOT_STARTED -> {
            StartScreen(
                onStart = {
                    gameState = GameState.PLAYING
                    isPaused = false
                },
                highScore = highScore
            )
        }
        GameState.PLAYING -> {
            GameScreen(
                snake = snake,
                food = food,
                gridSize = gridSize,
                cellSize = cellSize,
                score = score,
                level = level,
                highScore = highScore,
                isPaused = isPaused,
                onDirectionChange = { newDirection ->
                    // Prevent 180 degree turns
                    if (direction.opposite != newDirection) {
                        nextDirection = newDirection
                    }
                },
                onPauseToggle = {
                    isPaused = !isPaused
                },
                onReset = {
                    gameState = GameState.NOT_STARTED
                    score = 0
                    level = 1
                    snake = createInitialSnake()
                    food = generateFood(snake)
                    direction = Direction.RIGHT
                    nextDirection = Direction.RIGHT
                    isPaused = false
                }
            )
        }
        GameState.GAME_OVER -> {
            GameOverScreen(
                score = score,
                highScore = highScore,
                onRestart = {
                    gameState = GameState.PLAYING
                    score = 0
                    level = 1
                    snake = createInitialSnake()
                    food = generateFood(snake)
                    direction = Direction.RIGHT
                    nextDirection = Direction.RIGHT
                    isPaused = false
                }
            )
        }
        GameState.LEVEL_COMPLETE -> {
            LevelCompleteScreen(
                level = level,
                score = score,
                onContinue = {
                    gameState = GameState.PLAYING
                    isPaused = false
                }
            )
        }
    }
}

@Composable
fun GameScreen(
    snake: List<Pair<Int, Int>>,
    food: Pair<Int, Int>,
    gridSize: Int,
    cellSize: Dp,
    score: Int,
    level: Int,
    highScore: Int,
    isPaused: Boolean,
    onDirectionChange: (Direction) -> Unit,
    onPauseToggle: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A2A6C))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Game info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Score: $score",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Level: $level",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Best: $highScore",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Pause overlay
        if (isPaused) {
            Box(
                modifier = Modifier
                    .size(cellSize * gridSize)
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PAUSED",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // Game board
            Box(
                modifier = Modifier
                    .size(cellSize * gridSize)
                    .border(2.dp, Color.White)
                    .background(Color(0xFF0A192F))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw food
                    drawCircle(
                        color = Color(0xFFFF5252),
                        center = Offset(
                            (food.first + 0.5f) * cellSize.toPx(),
                            (food.second + 0.5f) * cellSize.toPx()
                        ),
                        radius = cellSize.toPx() / 2 - 2.dp.toPx()
                    )

                    // Draw snake
                    snake.forEachIndexed { index, segment ->
                        val color = if (index == 0) Color(0xFF76FF03) else Color(0xFF4CAF50)
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(
                                segment.first * cellSize.toPx() + 1.dp.toPx(),
                                segment.second * cellSize.toPx() + 1.dp.toPx()
                            ),
                            size = androidx.compose.ui.geometry.Size(
                                cellSize.toPx() - 2.dp.toPx(),
                                cellSize.toPx() - 2.dp.toPx()
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                    }
                }
            }
        }

        // Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
            ) {
                Text("Reset", fontSize = 16.sp)
            }

            Button(
                onClick = onPauseToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPaused) Color(0xFF4CAF50) else Color(0xFFFFC107)
                )
            ) {
                Text(if (isPaused) "Resume" else "Pause", fontSize = 16.sp)
            }
        }

        // Direction controls
        Column(
            modifier = Modifier.padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Up button
            Button(
                onClick = { onDirectionChange(Direction.UP) },
                modifier = Modifier.width(80.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                enabled = !isPaused
            ) {
                Text("↑", fontSize = 24.sp)
            }

            // Middle row
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { onDirectionChange(Direction.LEFT) },
                    modifier = Modifier.width(80.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    enabled = !isPaused
                ) {
                    Text("←", fontSize = 24.sp)
                }
                Button(
                    onClick = { onDirectionChange(Direction.RIGHT) },
                    modifier = Modifier.width(80.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    enabled = !isPaused
                ) {
                    Text("→", fontSize = 24.sp)
                }
            }

            // Down button
            Button(
                onClick = { onDirectionChange(Direction.DOWN) },
                modifier = Modifier.width(80.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                enabled = !isPaused
            ) {
                Text("↓", fontSize = 24.sp)
            }
        }
    }
}

@Composable
fun StartScreen(onStart: () -> Unit, highScore: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A2A6C))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Snake Game",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "High Score: $highScore",
            color = Color.White,
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = onStart,
            modifier = Modifier
                .width(200.dp)
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text("Start Game", fontSize = 20.sp)
        }

        // Level preview
        Column(
            modifier = Modifier.padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Levels Preview",
                color = Color.White,
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LevelPreview(level = 1, color = Color(0xFF4CAF50))
                LevelPreview(level = 2, color = Color(0xFF2196F3))
                LevelPreview(level = 3, color = Color(0xFFFF9800))
            }
        }
    }
}

@Composable
fun LevelPreview(level: Int, color: Color) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.3f))
            .border(2.dp, color, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = level.toString(),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GameOverScreen(score: Int, highScore: Int, onRestart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A2A6C))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Game Over",
            color = Color(0xFFFF5252),
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Score: $score",
            color = Color.White,
            fontSize = 32.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "High Score: $highScore",
            color = Color.White,
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = onRestart,
            modifier = Modifier
                .width(200.dp)
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text("Play Again", fontSize = 20.sp)
        }
    }
}

@Composable
fun LevelCompleteScreen(level: Int, score: Int, onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A2A6C))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Level $level Complete!",
            color = Color(0xFF4CAF50),
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Score: $score",
            color = Color.White,
            fontSize = 32.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = onContinue,
            modifier = Modifier
                .width(200.dp)
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text("Continue", fontSize = 20.sp)
        }
    }
}

// Game logic functions
fun createInitialSnake(): List<Pair<Int, Int>> {
    return listOf(Pair(5, 10), Pair(4, 10), Pair(3, 10))
}

fun generateFood(snake: List<Pair<Int, Int>>): Pair<Int, Int> {
    var food: Pair<Int, Int>
    do {
        food = Pair(Random.nextInt(0, 20), Random.nextInt(0, 20))
    } while (food in snake)
    return food
}

fun moveSnake(snake: List<Pair<Int, Int>>, direction: Direction, gridSize: Int): List<Pair<Int, Int>> {
    val head = snake[0]
    val newHead = when (direction) {
        Direction.UP -> Pair(head.first, (head.second - 1).mod(gridSize))
        Direction.DOWN -> Pair(head.first, (head.second + 1).mod(gridSize))
        Direction.LEFT -> Pair((head.first - 1).mod(gridSize), head.second)
        Direction.RIGHT -> Pair((head.first + 1).mod(gridSize), head.second)
    }
    return listOf(newHead) + snake.dropLast(1)
}

fun checkCollision(snake: List<Pair<Int, Int>>, gridSize: Int): Boolean {
    val head = snake[0]

    // Check if snake hits itself
    if (snake.drop(1).any { it == head }) {
        return true
    }

    return false
}

// Data classes and enums
enum class GameState {
    NOT_STARTED, PLAYING, GAME_OVER, LEVEL_COMPLETE
}

enum class Direction {
    UP, DOWN, LEFT, RIGHT;

    val opposite: Direction
        get() = when (this) {
            UP -> DOWN
            DOWN -> UP
            LEFT -> RIGHT
            RIGHT -> LEFT
        }
}