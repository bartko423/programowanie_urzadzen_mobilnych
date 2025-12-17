package com.example.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private val thread: GameThread
    private val player = Player(100f, 100f, context)

    private val platforms = listOf(
        Platform(0f, 800f, 800f, 50f),
        Platform(400f, 650f, 300f, 50f),
        Platform(900f, 500f, 300f, 50f),
        Platform(1400f, 350f, 300f, 50f)
    )

    private val coins = mutableListOf(
        Coin(200f, 750f),
        Coin(500f, 600f),
        Coin(950f, 450f),
        Coin(1450f, 300f)
    )

    private val castle = Castle(1700f, 150f)

    private var score = 0
    private var cameraOffsetX = 0f
    private var gameOver = false

    init {
        holder.addCallback(this)
        thread = GameThread(holder, this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        thread.running = true
        if (!thread.isAlive) thread.start()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        thread.running = false
        try { thread.join() } catch (_: Exception) {}
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun onTouchEvent(event: MotionEvent): Boolean {

        if (gameOver && event.action == MotionEvent.ACTION_DOWN) {
            restartGame()
            return true
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (event.x < width / 2) {
                    player.moveLeft()
                } else {
                    player.moveRight()
                }
            }
            MotionEvent.ACTION_UP -> {
                player.stop()
                player.jump()
            }
        }
        return true
    }

    private fun restartGame() {
        player.x = 100f
        player.y = 100f
        player.velocityX = 0f
        player.velocityY = 0f
        score = 0
        coins.forEach { it.collected = false }
        gameOver = false
    }

    fun update() {
        if (width == 0 || gameOver) return

        player.update()

        // scrolling
        cameraOffsetX = player.x - width / 2f
        if (cameraOffsetX < 0) cameraOffsetX = 0f

        // --- 1. KOLIZJE PIONOWE ---
        var standingOnPlatform = false

        platforms.forEach { p ->
            val playerBottom = player.hitboxBottom()
            val playerTop = player.hitboxTop()
            val playerLeft = player.hitboxLeft()
            val playerRight = player.hitboxRight()

            val platformTop = p.y
            val platformLeft = p.x
            val platformRight = p.x + p.width

            if (playerBottom + player.velocityY >= platformTop &&
                playerTop < platformTop &&
                playerRight > platformLeft &&
                playerLeft < platformRight &&
                player.velocityY >= 0
            ) {
                player.landOn(platformTop)
                standingOnPlatform = true
            }

        }

        if (!standingOnPlatform) {
            player.isFalling = true
        }

        // --- 2. KOLIZJE POZIOME ---
        platforms.forEach { p ->
            val playerBottom = player.hitboxBottom()
            val playerTop = player.hitboxTop()
            val playerLeft = player.hitboxLeft()
            val playerRight = player.hitboxRight()

            val platformTop = p.y
            val platformBottom = p.y + p.height
            val platformLeft = p.x
            val platformRight = p.x + p.width

            if (playerBottom > platformTop &&
                playerTop < platformBottom &&
                playerRight > platformLeft &&
                playerLeft < platformRight
            ) {
                if (player.velocityX > 0) {
                    player.x = platformLeft - player.hitboxWidth - player.hitboxOffsetX
                } else if (player.velocityX < 0) {
                    player.x = platformRight - player.hitboxOffsetX
                }
                player.stop()
            }
        }

        // --- 3. MONETY ---
        coins.forEach { coin ->
            if (!coin.collected &&
                player.hitboxLeft() < coin.x + coin.size &&
                player.hitboxRight() > coin.x &&
                player.hitboxTop() < coin.y + coin.size &&
                player.hitboxBottom() > coin.y
            ) {
                coin.collected = true
                score++
            }
        }

        // --- 4. KONIEC GRY (zamek) ---
        val playerRight = player.hitboxRight()
        val playerBottom = player.hitboxBottom()

        if (playerRight > castle.x &&
            player.hitboxLeft() < castle.x + castle.width &&
            playerBottom > castle.y &&
            player.hitboxTop() < castle.y + castle.height
        ) {
            gameOver = true
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas.drawColor(Color.WHITE)

        val paint = Paint()

        canvas.translate(-cameraOffsetX, 0f)

        // gracz
        player.draw(canvas)

        // platformy
        paint.color = Color.BLACK
        platforms.forEach {
            canvas.drawRect(it.x, it.y, it.x + it.width, it.y + it.height, paint)
        }

        // monety
        paint.color = Color.YELLOW
        coins.forEach { coin ->
            if (!coin.collected) {
                canvas.drawCircle(
                    coin.x + coin.size / 2,
                    coin.y + coin.size / 2,
                    coin.size / 2,
                    paint
                )
            }
        }

        // zamek
        paint.color = Color.rgb(139, 69, 19)
        canvas.drawRect(
            castle.x,
            castle.y,
            castle.x + castle.width,
            castle.y + castle.height,
            paint
        )

        // wynik
        canvas.translate(cameraOffsetX, 0f)
        paint.color = Color.RED
        paint.textSize = 60f
        canvas.drawText("Score: $score", 50f, 100f, paint)

        if (gameOver) {
            paint.color = Color.argb(200, 0, 0, 0)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            paint.color = Color.WHITE
            paint.textSize = 100f
            canvas.drawText("GAME OVER", width / 2f - 250f, height / 2f - 100f, paint)

            paint.textSize = 60f
            canvas.drawText("Tap to Retry", width / 2f - 150f, height / 2f + 20f, paint)
        }
    }
}
