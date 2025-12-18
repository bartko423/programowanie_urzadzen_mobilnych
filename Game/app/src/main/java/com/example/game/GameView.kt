package com.example.game

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private val thread: GameThread
    private val player = Player(100f, 100f, context)

    // --- TŁO Z PARALAKSĄ ---
    private val background = BitmapFactory.decodeResource(resources, R.drawable.forest_bg)
    private var scaledBg: Bitmap? = null
    private val parallaxFactor = 0.3f

    // --- STATYCZNE PLATFORMY ---
    private val platforms = listOf(
        Platform(0f, 800f, 800f, 50f),
        Platform(400f, 670f, 300f, 50f),
        Platform(900f, 500f, 300f, 50f),
        Platform(1400f, 350f, 300f, 50f),

        Platform(1800f, 700f, 200f, 50f),
        Platform(2100f, 600f, 150f, 50f),
        Platform(2300f, 500f, 250f, 50f),
        Platform(2400f, 700f, 220f, 50f),
        Platform(3000f, 550f, 300f, 50f),
        Platform(3300f, 650f, 150f, 50f),
        Platform(3600f, 750f, 200f, 50f),
        Platform(3900f, 600f, 210f, 50f),
        Platform(4200f, 450f, 120f, 50f),
        Platform(4500f, 350f, 200f, 50f),
        Platform(4800f, 500f, 300f, 50f),
        Platform(5100f, 650f, 150f, 50f),
        Platform(5400f, 750f, 200f, 50f),
        Platform(5700f, 600f, 250f, 50f),
        Platform(6000f, 450f, 150f, 50f),
        Platform(6300f, 350f, 200f, 50f),
        Platform(6600f, 500f, 300f, 50f),
        Platform(6900f, 650f, 150f, 50f),
        Platform(7100f, 700f, 130f, 50f)
    )

    // --- RUCHOME PLATFORMY ---
    private val movingPlatforms = listOf(
        MovingPlatform(2800f, 700f, 200f, 50f, 650f, 750f, 2f),
        MovingPlatform(4000f, 500f, 150f, 50f, 450f, 550f, 2.5f),
        MovingPlatform(5500f, 600f, 250f, 50f, 550f, 650f, 1.8f),
        MovingPlatform(7350f, 550f, 200f, 50f, 500f, 600f, 1.8f)
    )

    private val coins = mutableListOf(
        Coin(200f, 750f),
        Coin(500f, 600f),
        Coin(950f, 450f),
        Coin(1450f, 300f),
        Coin(2600f, 680f),
        Coin(3100f, 530f),
        Coin(3600f, 720f),
        Coin(4200f, 430f),
        Coin(4800f, 480f),
        Coin(5400f, 580f)
    )

    private val castle = Castle(7600f, 150f)

    private var score = 0
    private var cameraOffsetX = 0f
    private var gameOver = false
    private var showStartScreen = true

    private var falls = 0

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

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        scaledBg = Bitmap.createScaledBitmap(background, width, height, true)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        if (showStartScreen && event.action == MotionEvent.ACTION_DOWN) {
            showStartScreen = false
            return true
        }

        if (gameOver && event.action == MotionEvent.ACTION_DOWN) {
            restartGame()
            return true
        }

        if (!showStartScreen) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (event.x < width / 2) player.moveLeft()
                    else player.moveRight()
                }
                MotionEvent.ACTION_UP -> {
                    player.stop()
                    player.jump()
                }
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
        falls = 0
        coins.forEach { it.collected = false }
        gameOver = false
        showStartScreen = true
    }

    fun update() {
        if (width == 0 || gameOver || showStartScreen) return

        player.update()

        // --- RUCH PLATFORM ---
        for (p in movingPlatforms) {
            p.update()
        }

        // --- KAMERA ---
        cameraOffsetX = player.x - width / 2f
        if (cameraOffsetX < 0) cameraOffsetX = 0f

        // --- KOLIZJE PIONOWE ---
        var standingOnPlatform = false

        fun checkVerticalCollision(px: Float, py: Float, pw: Float) {
            val bottom = player.hitboxBottom()
            val top = player.hitboxTop()
            val left = player.hitboxLeft()
            val right = player.hitboxRight()

            if (bottom + player.velocityY >= py &&
                top < py &&
                right > px &&
                left < px + pw &&
                player.velocityY >= 0
            ) {
                player.landOn(py)
                standingOnPlatform = true
            }
        }

        for (p in platforms) checkVerticalCollision(p.x, p.y, p.width)
        for (p in movingPlatforms) checkVerticalCollision(p.x, p.y, p.width)

        if (!standingOnPlatform) player.isFalling = true

        // --- KOLIZJE POZIOME ---
        fun checkHorizontalCollision(px: Float, py: Float, pw: Float, ph: Float) {
            val bottom = player.hitboxBottom()
            val top = player.hitboxTop()
            val left = player.hitboxLeft()
            val right = player.hitboxRight()

            if (bottom > py &&
                top < py + ph &&
                right > px &&
                left < px + pw
            ) {
                if (player.velocityX > 0) {
                    player.x = px - player.hitboxWidth - player.hitboxOffsetX
                } else if (player.velocityX < 0) {
                    player.x = px + pw - player.hitboxOffsetX
                }
                player.stop()
            }
        }

        for (p in platforms) checkHorizontalCollision(p.x, p.y, p.width, p.height)
        for (p in movingPlatforms) checkHorizontalCollision(p.x, p.y, p.width, p.height)

        // --- MONETY ---
        for (coin in coins) {
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

        // --- KONIEC GRY ---
        if (player.hitboxRight() > castle.x &&
            player.hitboxLeft() < castle.x + castle.width &&
            player.hitboxBottom() > castle.y &&
            player.hitboxTop() < castle.y + castle.height
        ) {
            gameOver = true
        }

        // --- UPADEK ---
        if (player.y > height) {
            falls++

            player.x = 100f
            player.y = 100f
            player.velocityX = 0f
            player.velocityY = 0f

            if (falls >= 3) gameOver = true
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        val paint = Paint()

        // --- TŁO Z PARALAKSĄ ---
        scaledBg?.let { bg ->
            val bgX = -(cameraOffsetX * parallaxFactor)
            val bgWidth = bg.width

            var drawX = bgX
            while (drawX < width) {
                canvas.drawBitmap(bg, drawX, 0f, null)
                drawX += bgWidth
            }
        }

        // --- KAMERA ---
        canvas.translate(-cameraOffsetX, 0f)

        // --- GRACZ ---
        player.draw(canvas)

        // --- PLATFORMY ---
        paint.color = Color.rgb(139, 69, 19)
        for (p in platforms) {
            canvas.drawRect(p.x, p.y, p.x + p.width, p.y + p.height, paint)
        }

        // --- RUCHOME PLATFORMY ---
        paint.color = Color.rgb(110, 50, 10)
        for (p in movingPlatforms) {
            canvas.drawRect(p.x, p.y, p.x + p.width, p.y + p.height, paint)
        }

        // --- MONETY ---
        paint.color = Color.YELLOW
        for (coin in coins) {
            if (!coin.collected) {
                canvas.drawCircle(
                    coin.x + coin.size / 2,
                    coin.y + coin.size / 2,
                    coin.size / 2,
                    paint
                )
            }
        }

        // --- ZAMEK ---
        paint.color = Color.rgb(139, 69, 19)
        canvas.drawRect(
            castle.x,
            castle.y,
            castle.x + castle.width,
            castle.y + castle.height,
            paint
        )

        // --- UI ---
        canvas.translate(cameraOffsetX, 0f)
        paint.color = Color.RED
        paint.textSize = 60f
        canvas.drawText("Score: $score", 50f, 100f, paint)

        // --- START SCREEN ---
        if (showStartScreen) {
            paint.color = Color.argb(200, 0, 0, 0)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            paint.color = Color.WHITE
            paint.textSize = 100f

            val title = "FOX ADVENTURE"
            val titleWidth = paint.measureText(title)
            canvas.drawText(title, width / 2f - titleWidth / 2f, height / 2f - 100f, paint)

            paint.textSize = 60f
            val subtitle = "Tap to Start"
            val subtitleWidth = paint.measureText(subtitle)
            canvas.drawText(subtitle, width / 2f - subtitleWidth / 2f, height / 2f + 20f, paint)

            return
        }

        // --- GAME OVER ---
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
