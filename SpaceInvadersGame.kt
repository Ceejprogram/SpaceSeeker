import android.content.Context
import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spaceseeker.R
import kotlinx.coroutines.delay
import kotlin.math.abs

data class Player(val position: Offset, val speed: Float = 500f)
data class Bullet(val position: Offset, val velocity: Offset)
data class Enemy(val position: Offset, var health: Int = 1, val maxHealth: Int = 1)
data class EnemyBullet(val position: Offset, val velocity: Offset)

object MusicPlayer {
    private var mediaPlayer: MediaPlayer? = null

    fun start(context: Context) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, R.raw.background_music).apply {
                isLooping = true // Loop the music
                start() // Start playing the music
            }
        }
    }

    fun stop() {
        mediaPlayer?.let {
            it.stop()
            it.release()
            mediaPlayer = null
        }
    }
}

@Composable
fun SpaceInvadersGame() {
    val context = LocalContext.current

    // Start the music when the game starts
    LaunchedEffect(Unit) {
        MusicPlayer.start(context)
        SoundEffectPlayer.loadSounds(context)
    }

    // Release MediaPlayer when the game is over
    DisposableEffect(Unit) {
        onDispose {
            MusicPlayer.stop()
            SoundEffectPlayer.release()
        }
    }

    // Game state variables
    var player by remember { mutableStateOf(Player(Offset.Zero)) }
    var bullets by remember { mutableStateOf(emptyList<Bullet>()) }
    var enemyBullets by remember { mutableStateOf(emptyList<EnemyBullet>()) }
    var enemies by remember { mutableStateOf(emptyList<Enemy>()) }
    var score by remember { mutableStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }
    var victory by remember { mutableStateOf(false) }
    var currentStage by remember { mutableStateOf(1) }
    var screenWidth by remember { mutableStateOf(0f) }
    var screenHeight by remember { mutableStateOf(0f) }
    var enemyDirection by remember { mutableStateOf(1f) }
    var enemyVerticalSpeed by remember { mutableStateOf(0f) }
    var gameStarted by remember { mutableStateOf(false) } // New state variable

    // Load images
    val startGameBitmap =
        ImageBitmap.imageResource(id = R.drawable.game_start) // Load as ImageBitmap
    val spaceshipBitmap = ImageBitmap.imageResource(id = R.drawable.spaceship)
    val invader1Bitmap = ImageBitmap.imageResource(id = R.drawable.invader1)
    val invader2Bitmap = ImageBitmap.imageResource(id = R.drawable.invader2)
    val invader3Bitmap = ImageBitmap.imageResource(id = R.drawable.invader3)
    val playerBulletBitmap = ImageBitmap.imageResource(id = R.drawable.missle1)
    val enemyBulletBitmap = ImageBitmap.imageResource(id = R.drawable.evilmissle1)
    val livesBitmap = ImageBitmap.imageResource(id = R.drawable.crafts) // Load the crafts image
    val backgroundBitmap =
        ImageBitmap.imageResource(id = R.drawable.space_background) // Load the background image

    // Background position
    var backgroundY by remember { mutableStateOf(0f) }

    // Cooldown for boss shooting
    var bossShootCooldown by remember { mutableStateOf(0) }
    val bossShootInterval = 1000 // milliseconds

    // Lives variable
    var lives by remember { mutableStateOf(3) }

    val enemySize = when (currentStage) {
        4 -> 60f
        5 -> 120f
        else -> 40f
    }
    val playerSize = 50f

    fun createStage(stage: Int): List<Enemy> {
        return when (stage) {
            1 -> {
                val baseX = screenWidth / 4
                List(2) { row ->
                    List(3) { col ->
                        Enemy(
                            Offset(baseX + col * 150f, -100f + row * -150f),
                            health = 1,
                            maxHealth = 1
                        ) // 1 health
                    }
                }.flatten()
            }

            2 -> {
                val baseX = screenWidth / 6
                List(3) { row ->
                    List(4) { col ->
                        Enemy(
                            Offset(baseX + col * 100f, -100f + row * -150f),
                            health = 2,
                            maxHealth = 2
                        ) // 2 health
                    }
                }.flatten()
            }

            3 -> {
                val baseX = screenWidth / 8
                List(4) { row ->
                    List(6) { col ->
                        Enemy(
                            Offset(baseX + col * 70f, -100f + row * -150f),
                            health = 3,
                            maxHealth = 3
                        ) // 3 health
                    }
                }.flatten()
            }

            4 -> {
                val baseX = (screenWidth - (4 * 100f)) / 2
                List(3) { row ->
                    List(5) { col ->
                        Enemy(
                            Offset(baseX + col * 100f, -400f + row * -150f),
                            health = 3,
                            maxHealth = 3
                        ) // 3 health
                    }
                }.flatten()
                    .plus(
                        listOf(
                            Enemy(Offset(screenWidth * 0.25f, -200f), health = 5, maxHealth = 5),
                            Enemy(Offset(screenWidth * 0.75f, -200f), health = 5, maxHealth = 5)
                        )
                    )
            }

            5 -> listOf(Enemy(Offset(screenWidth / 2, -250f), health = 20, maxHealth = 20)) // Boss
            else -> emptyList()
        }
    }

    LaunchedEffect(currentStage, screenWidth, screenHeight) {
        if (screenWidth > 0 && screenHeight > 0) {
            enemies = createStage(currentStage)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            if (!gameOver && !victory) {
                // Update background position for scrolling effect
                backgroundY += 4 // Adjust speed as needed

                if (gameStarted) {
                    // Enemy movement
                    val moveSpeed = if (currentStage == 4) {
                        1.3f // Reduced speed for level 4
                    } else {
                        1.8f + currentStage * 0.45f // Original speed for other levels
                    }
                    val updatedEnemies = enemies.map { enemy ->
                        val newX = enemy.position.x + (moveSpeed * enemyDirection)
                        val newY = enemy.position.y + enemyVerticalSpeed
                        enemy.copy(position = Offset(newX, newY))
                    }

                    // Enemy shooting logic
                    if (currentStage <= 3) {
                        if (Math.random() < 0.02) { // 2% chance to shoot
                            val enemyToShoot = enemies.randomOrNull()
                            enemyToShoot?.let {
                                enemyBullets = enemyBullets + EnemyBullet(
                                    position = it.position.copy(y = it.position.y + enemySize),
                                    velocity = Offset(0f, 400f) // Bullet speed
                                )
                            }
                        }
                    } else if (currentStage == 4) {
                        // Reduce the number of bullets fired by enemies
                        enemies.forEach { enemy ->
                            if (Math.random() < 0.01) { // 1% chance to shoot (reduced from 2%)
                                enemyBullets = enemyBullets + EnemyBullet(
                                    position = enemy.position.copy(y = enemy.position.y + enemySize),
                                    velocity = Offset(0f, 380f) // Bullet speed
                                )
                            }
                        }
                    } else if (currentStage == 5) { // Assuming stage 5 is the final boss
                        // Check if the boss can shoot based on cooldown
                        if (bossShootCooldown <= 0) {
                            enemies.forEach { enemy ->
                                enemyBullets = enemyBullets + listOf(
                                    EnemyBullet(
                                        position = enemy.position.copy(y = enemy.position.y + enemySize),
                                        velocity = Offset(-200f, 400f) // Left bullet (south-west)
                                    ),
                                    EnemyBullet(
                                        position = enemy.position.copy(y = enemy.position.y + enemySize),
                                        velocity = Offset(0f, 400f) // Center bullet (straight down)
                                    ),
                                    EnemyBullet(
                                        position = enemy.position.copy(y = enemy.position.y + enemySize),
                                        velocity = Offset(200f, 400f) // Right bullet (south-east)
                                    )
                                )
                            }
                            bossShootCooldown = bossShootInterval // Reset cooldown
                        }
                    }

                    // Decrease cooldown timer
                    if (bossShootCooldown > 0) {
                        bossShootCooldown -= 16 // Assuming 16 ms per frame
                    }

                    // Boundary checking
                    val minX = updatedEnemies.minOfOrNull { it.position.x } ?: 0f
                    val maxX = updatedEnemies.maxOfOrNull { it.position.x } ?: 0f

                    if (maxX > screenWidth - 50f || minX < 50f) {
                        enemyDirection *= -1
                        enemyVerticalSpeed = 45f // Increased downward speed
                    } else {
                        enemyVerticalSpeed = 0f
                    }

                    enemies = updatedEnemies

                    // Update player bullets
                    bullets = bullets
                        .map { it.copy(position = it.position + it.velocity * 0.016f) }
                        .filter { it.position.y > 0f } // Keep bullets on screen

                    // Update enemy bullets
                    enemyBullets = enemyBullets
                        .map { it.copy(position = it.position + it.velocity * 0.016f) }
                        .filter { it.position.y < screenHeight } // Keep enemy bullets on screen

                    // Collision detection for player bullets
                    val hitBullets = mutableSetOf<Bullet>()
                    val enemiesAfterCollision = enemies.map { enemy ->  // Renamed variable
                        var currentEnemy = enemy
                        bullets.forEach { bullet ->
                            if (abs(bullet.position.x - enemy.position.x) < enemySize / 2 &&
                                abs(bullet.position.y - enemy.position.y) < enemySize / 2
                            ) {
                                currentEnemy = currentEnemy.copy(health = currentEnemy.health - 1)
                                score += 10
                                hitBullets.add(bullet)
                            }
                        }
                        currentEnemy
                    }

                    // Remove bullets that hit any enemy and update enemies list
                    bullets = bullets.filterNot { it in hitBullets }
                    enemies =
                        enemiesAfterCollision.filter { it.health > 0 }  // Use renamed variable

                    // Check for collisions with enemy bullets
                    if (enemyBullets.any { bullet ->
                            abs(bullet.position.x - player.position.x) < playerSize / 2 &&
                                    abs(bullet.position.y - player.position.y) < playerSize / 2
                        }) {
                        lives -= 1 // Decrease lives by 1
                        enemyBullets = enemyBullets.filterNot { bullet ->
                            abs(bullet.position.x - player.position.x) < playerSize / 2 &&
                                    abs(bullet.position.y - player.position.y) < playerSize / 2
                        } // Remove the bullet that hit the player

                        if (lives <= 0) {
                            gameOver = true // Set game over if no lives left
                        }
                    }

                    // Stage progression
                    if (enemies.isEmpty()) {
                        if (currentStage == 5) {
                            victory = true
                        } else {
                            currentStage++
                            enemyDirection = 1f
                            enemies = createStage(currentStage)
                            bullets = emptyList()
                        }
                    }

                    // Game over check
                    if (enemies.any { it.position.y > screenHeight - 150f }) {
                        gameOver = true
                    }
                }

                delay(16)
            } else {
                // Stop the game loop if game is over or victory
                delay(100) // Just to prevent busy waiting
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(gameStarted) {  // Reinitialize input handling when game starts
                    if (gameStarted) {
                        detectDragGestures { _, dragAmount ->
                            player = player.copy(
                                position = player.position.copy(
                                    x = (player.position.x + dragAmount.x)
                                        .coerceIn(50f, screenWidth - 50f)
                                )
                            )
                        }
                    } else {
                        detectTapGestures {
                            gameStarted = true // Start the game on tap
                        }
                    }
                }
                .pointerInput(gameStarted) { // Separate tap listener for shooting
                    if (gameStarted) {
                        detectTapGestures {
                            bullets = bullets + Bullet(
                                position = player.position.copy(
                                    y = player.position.y - playerSize
                                ),
                                velocity = Offset(0f, -800f)
                            )
                            SoundEffectPlayer.playShootSound()
                        }
                    }
                }
        ) {
            // Draw the moving background with two layers
            val verticalOffset = backgroundY % size.height

            // Draw the first layer of the background
            drawImage(backgroundBitmap, topLeft = Offset(0f, verticalOffset))

            // Draw the second layer of the background
            drawImage(backgroundBitmap, topLeft = Offset(0f, verticalOffset - size.height))

            screenWidth = size.width
            screenHeight = size.height
            if (player.position == Offset.Zero) {
                player = Player(Offset(screenWidth / 2, screenHeight - 100f))
            }

            // Draw player centered
            drawImage(
                spaceshipBitmap,
                topLeft = player.position - Offset(
                    spaceshipBitmap.width / 2f,
                    spaceshipBitmap.height / 2f
                )
            )

            // Draw player bullets
            bullets.forEach { bullet ->
                drawImage(
                    playerBulletBitmap,
                    topLeft = bullet.position - Offset(
                        playerBulletBitmap.width / 2f,
                        playerBulletBitmap.height / 2f
                    )
                )
            }

            // Draw enemy bullets
            enemyBullets.forEach { bullet ->
                drawImage(
                    enemyBulletBitmap,
                    topLeft = bullet.position - Offset(
                        enemyBulletBitmap.width / 2f,
                        enemyBulletBitmap.height / 2f
                    )
                )
            }

            // Draw enemies
            enemies.forEach { enemy ->
                val enemyBitmap = when (enemy.maxHealth) {
                    5 -> invader2Bitmap // Yellow enemies
                    20 -> invader3Bitmap // Boss
                    else -> invader1Bitmap // Green enemies
                }
                drawImage(
                    enemyBitmap,
                    topLeft = enemy.position - Offset(
                        enemyBitmap.width / 2f,
                        enemyBitmap.height / 2f
                    )
                )

                // Health bar
                if (enemy.maxHealth > 1) {
                    drawCircle(
                        Color.Red.copy(alpha = 0.3f),
                        enemySize * (enemy.health.toFloat() / enemy.maxHealth),
                        enemy.position
                    )
                }
            }

            // Draw lives as images in the top-right corner
            for (i in 0 until lives) {
                drawImage(
                    livesBitmap,
                    topLeft = Offset(screenWidth - 100f - i * 90f, 10f) // Adjust position as needed
                )
            }

            // Draw the start game image if the game hasn't started
            if (!gameStarted) {
                drawImage(
                    startGameBitmap,
                    topLeft = Offset(
                        (screenWidth - startGameBitmap.width) / 2,
                        (screenHeight - startGameBitmap.height) / 2
                    )
                )
            }
        }

        // Handle spaceship movement and shooting
        if (gameStarted) {
            Modifier.pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    player = player.copy(
                        position = player.position.copy(
                            x = (player.position.x + dragAmount.x)
                                .coerceIn(50f, screenWidth - 50f)
                        )
                    )
                }
            }


        }

        Column(Modifier.padding(16.dp)) {
            Text("WAVE $currentStage", fontSize = 20.sp, color = Color.Red) // Smaller stage text
            if (gameOver) Text("GAME OVER!", fontSize = 32.sp, color = Color.Red)
            if (victory) Text("Successfully Defended!", fontSize = 32.sp, color = Color.Green)
        }

        if (gameOver || victory) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .pointerInput(Unit) { }  // Ensure touch priority
            ) {
                Button(
                    onClick = {
                        // Full game reset
                        player = Player(Offset.Zero)
                        bullets = emptyList()
                        enemyBullets = emptyList() // Reset enemy bullets
                        score = 0
                        lives = 3 // Reset lives
                        gameOver = false
                        victory = false
                        currentStage = 1
                        enemyDirection = 1f
                        enemyVerticalSpeed = 0f
                        // Force recreation of enemies
                        enemies = createStage(1)
                    }
                ) {
                    Text("Restart Game")
                }
            }
        }
    }
}

@Composable
@Preview
fun Preview() {
    SpaceInvadersGame()
}