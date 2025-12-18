package com.example.game

import android.content.Context
import android.graphics.*
import android.media.MediaPlayer
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

// GameView jest główną sceną gry i pełni rolę mini‑silnika.
// Odpowiada za:
// - pętlę gry (update + draw),
// - sterowanie,
// - kolizje,
// - kamerę,
// - rysowanie świata,
// - muzykę,
// - ekrany: startowy i Game Over.
//
// To centralny punkt logiki — wszystkie elementy gry są aktualizowane i rysowane tutaj.
class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private val thread: GameThread
    private val player = Player(100f, 100f, context)

    // MUZYKA W TLE
    // MediaPlayer odtwarza plik z res/raw i działa w pętli.
    // Muzyka startuje już na ekranie startowym, aby gra była bardziej „żywa”.
    private var mediaPlayer: MediaPlayer? = null

    private fun startMusic() {
        // Tworzymy MediaPlayer tylko raz — to oszczędza zasoby.
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, R.raw.background_music)
            mediaPlayer?.isLooping = true // muzyka gra bez końca
        }
        mediaPlayer?.start()
    }

    private fun stopMusic() {
        // Pauza zamiast stop — pozwala wznowić muzykę od tego samego miejsca.
        mediaPlayer?.pause()
    }

    // TŁO Z PARALAKSĄ
    // Paralaksa to efekt, w którym tło przesuwa się wolniej niż obiekty na pierwszym planie.
    // Dzięki temu gra wygląda głębiej i bardziej dynamicznie.
    private val background = BitmapFactory.decodeResource(resources, R.drawable.forest_bg)
    private var scaledBg: Bitmap? = null
    private val parallaxFactor = 0.3f // 30% prędkości kamery — klasyczna wartość dla platformówek

    // PLATFORMY STATYCZNE
    // To główne elementy poziomu, po których gracz może chodzić.
    // Każda platforma ma pozycję i rozmiar w przestrzeni świata.
    private val platforms = listOf(
        Platform(0f, 800f, 800f, 50f),
        Platform(400f, 670f, 300f, 50f),
        Platform(900f, 500f, 300f, 50f),
        Platform(1400f, 350f, 300f, 50f),

        // Dalsza część poziomu — projektowo tworzy rosnące wyzwanie.
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

    // PLATFORMY RUCHOME
    // Poruszają się góra–dół w określonym zakresie.
    // To klasyczny element platformówek — wymusza timing i zwiększa dynamikę.
    private val movingPlatforms = listOf(
        MovingPlatform(2800f, 700f, 200f, 50f, 650f, 750f, 2f),
        MovingPlatform(4000f, 500f, 150f, 50f, 450f, 550f, 2.5f),
        MovingPlatform(5500f, 600f, 250f, 50f, 550f, 650f, 1.8f),
        MovingPlatform(7350f, 550f, 200f, 50f, 500f, 600f, 1.8f)
    )

    // MONETY
    // Zbierane przez gracza — zwiększają wynik.
    // Rozmieszczone projektowo tak, aby prowadzić gracza przez poziom.
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

    // META POZIOMU — zamek.
    // Dotknięcie zamku kończy grę.
    private val castle = Castle(7600f, 150f)

    // STAN GRY
    private var score = 0
    private var cameraOffsetX = 0f // przesunięcie świata względem ekranu
    private var gameOver = false
    private var showStartScreen = true
    private var falls = 0 // licznik upadków poza ekran

    init {
        // SurfaceHolder pozwala rysować na SurfaceView.
        // Rejestrujemy callbacki, aby wiedzieć kiedy powierzchnia jest gotowa.
        holder.addCallback(this)

        // Tworzymy pętlę gry.
        thread = GameThread(holder, this)

        // Pozwala na odbieranie zdarzeń dotyku.
        isFocusable = true
    }

    // Wywoływane, gdy powierzchnia jest gotowa do rysowania.
    // Startujemy pętlę gry i muzykę.
    override fun surfaceCreated(holder: SurfaceHolder) {
        thread.running = true
        if (!thread.isAlive) thread.start()
        startMusic()
    }

    // Wywoływane, gdy powierzchnia jest niszczona.
    // Zatrzymujemy pętlę gry i zwalniamy zasoby muzyki.
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        thread.running = false
        try { thread.join() } catch (_: Exception) {}
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // Wywoływane przy zmianie rozmiaru ekranu.
    // Skalujemy tło, aby idealnie pasowało do ekranu.
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        scaledBg = Bitmap.createScaledBitmap(background, width, height, true)
    }

    // Obsługa dotyku — sterowanie, start gry, restart.
    override fun onTouchEvent(event: MotionEvent): Boolean {

        // Ekran startowy — pierwszy tap rozpoczyna grę.
        if (showStartScreen && event.action == MotionEvent.ACTION_DOWN) {
            showStartScreen = false
            return true
        }

        // Game Over — tap restartuje grę.
        if (gameOver && event.action == MotionEvent.ACTION_DOWN) {
            restartGame()
            return true
        }

        // Sterowanie graczem — uproszczone sterowanie mobilne.
        // Lewa połowa ekranu = ruch w lewo, prawa = ruch w prawo.
        // Po puszczeniu palca — skok.
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

    // Reset gry po Game Over.
    // Przywracamy wszystkie wartości do stanu początkowego.
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

        startMusic()
    }

    // Główna logika gry — aktualizowana w każdej klatce.
    fun update() {
        // Jeśli gra nie trwa — nie aktualizujemy logiki.
        if (width == 0 || gameOver || showStartScreen) return

        // Aktualizacja fizyki gracza — grawitacja, ruch, skok.
        player.update()

        // Aktualizacja platform ruchomych — zmiana pozycji góra–dół.
        for (p in movingPlatforms) p.update()

        // KAMERA
        // Kamera śledzi gracza, przesuwając cały świat w lewo.
        // Dzięki temu gracz pozostaje w centrum ekranu.
        // To klasyczne podejście w platformówkach 2D.
        cameraOffsetX = player.x - width / 2f
        if (cameraOffsetX < 0) cameraOffsetX = 0f

        // KOLIZJE PIONOWE
        // Wykrywamy lądowanie od góry.
        // Używamy bottom + velocityY, aby przewidzieć pozycję gracza w następnej klatce.
        // Dzięki temu unikamy przenikania przez platformy.
        var standingOnPlatform = false

        fun checkVertical(px: Float, py: Float, pw: Float) {
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

        for (p in platforms) checkVertical(p.x, p.y, p.width)
        for (p in movingPlatforms) checkVertical(p.x, p.y, p.width)

        if (!standingOnPlatform) player.isFalling = true

        // KOLIZJE POZIOME
        // Wykrywamy uderzenie w bok platformy.
        // To zapobiega „wchodzeniu” w ściany.
        fun checkHorizontal(px: Float, py: Float, pw: Float, ph: Float) {
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

        for (p in platforms) checkHorizontal(p.x, p.y, p.width, p.height)
        for (p in movingPlatforms) checkHorizontal(p.x, p.y, p.width, p.height)

        // ZBIERANIE MONET
        // Prosta kolizja AABB — jeśli hitbox gracza nachodzi na monetę, zbieramy ją.
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

        // DOTARCIE DO ZAMKU
        // Jeśli hitbox gracza nachodzi na zamek — koniec gry.
        if (player.hitboxRight() > castle.x &&
            player.hitboxLeft() < castle.x + castle.width &&
            player.hitboxBottom() > castle.y &&
            player.hitboxTop() < castle.y + castle.height
        ) {
            gameOver = true
            stopMusic()
        }

        // UPADEK POZA EKRAN
        // Jeśli gracz spadnie poniżej ekranu — respawn.
        // Po 3 upadkach — Game Over.
        if (player.y > height) {
            falls++

            player.x = 100f
            player.y = 100f
            player.velocityX = 0f
            player.velocityY = 0f

            if (falls >= 3) {
                gameOver = true
                stopMusic()
            }
        }
    }

    // Rysowanie całej sceny.
    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        val paint = Paint()

        // TŁO Z PARALAKSĄ
        // Tło przesuwa się wolniej niż świat, co tworzy efekt głębi.
        // Rysujemy je wielokrotnie, aby zakryć cały ekran.
        scaledBg?.let { bg ->
            val bgX = -(cameraOffsetX * parallaxFactor)
            val bgWidth = bg.width

            var drawX = bgX
            while (drawX < width) {
                canvas.drawBitmap(bg, drawX, 0f, null)
                drawX += bgWidth
            }
        }

        // PRZESUNIĘCIE KAMERY
        // Od tego momentu rysujemy świat przesunięty o cameraOffsetX.
        canvas.translate(-cameraOffsetX, 0f)

        // GRACZ
        player.draw(canvas)

        // PLATFORMY STATYCZNE
        paint.color = Color.rgb(139, 69, 19)
        for (p in platforms) {
            canvas.drawRect(p.x, p.y, p.x + p.width, p.y + p.height, paint)
        }

        // PLATFORMY RUCHOME
        paint.color = Color.rgb(110, 50, 10)
        for (p in movingPlatforms) {
            canvas.drawRect(p.x, p.y, p.x + p.width, p.y + p.height, paint)
        }

        // MONETY
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

        // ZAMEK
        paint.color = Color.rgb(139, 69, 19)
        canvas.drawRect(
            castle.x,
            castle.y,
            castle.x + castle.width,
            castle.y + castle.height,
            paint
        )

        // UI — wynik
        // Przenosimy canvas z powrotem, aby UI było przyklejone do ekranu.
        canvas.translate(cameraOffsetX, 0f)
        paint.color = Color.RED
        paint.textSize = 60f
        canvas.drawText("Score: $score", 50f, 100f, paint)

        // EKRAN STARTOWY
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

        // EKRAN GAME OVER
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
