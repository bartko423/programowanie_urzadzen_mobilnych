package com.example.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas

// Klasa Player reprezentuje główną postać gry.
// Odpowiada za:
// - fizykę ruchu (grawitacja, skok, ruch poziomy),
// - hitbox i kolizje,
// - animacje (idle, run, jump),
// - wybór kierunku patrzenia,
// - rysowanie sprite’a.
//
// To samodzielny moduł — GameView nie musi znać szczegółów animacji ani fizyki.
// Dzięki temu kod gry jest czystszy i łatwiejszy do utrzymania.
class Player(
    var x: Float,   // pozycja sprite’a (lewy górny róg)
    var y: Float,   // y = hitboxTop (czyli górna krawędź hitboxa)
    private val context: Context
) {

    // ROZMIAR SPRITE’A
    // Sprite jest skalowany do 128x128 px, niezależnie od oryginalnych plików.
    // Dzięki temu animacje mają spójny rozmiar.
    val spriteSize = 128f
    val scale = spriteSize / 64f   // oryginalne sprite’y mają 64 px wysokości

    // HITBOX
    // Hitbox jest mniejszy niż sprite, aby kolizje były naturalne.
    // Wartości pochodzą z proporcji oryginalnego sprite’a.
    val hitboxWidth = 32f * scale
    val hitboxHeight = 46f * scale
    val hitboxOffsetY = 18f * scale
    val hitboxOffsetX = (spriteSize - hitboxWidth) / 2f
    // Dzięki temu hitbox jest wycentrowany i obejmuje tylko ciało lisa, nie futro.

    // FIZYKA
    var velocityY = 0f
    var velocityX = 0f

    private val gravity = 1.2f      // stałe przyspieszenie w dół
    private val jumpPower = -20f    // ujemne = ruch w górę
    private val moveSpeed = 10f     // prędkość pozioma

    var isFalling = true            // true = gracz w powietrzu
    private var facingRight = true  // kierunek patrzenia

    // ANIMACJE
    // Każda animacja to lista klatek (bitmap).
    // Nazwy plików muszą być zgodne z prefix + index (np. idle_right_0.png).
    private val idleRight = loadFrames("idle_right_", 8)
    private val idleLeft = loadFrames("idle_left_", 8)
    private val runRight = loadFrames("run_right_", 6)
    private val runLeft = loadFrames("run_left_", 6)
    private val jumpRight = loadFrames("jump_right_", 8)
    private val jumpLeft = loadFrames("jump_left_", 8)

    // Aktualnie wyświetlana animacja.
    private var currentFrames = idleRight
    private var currentFrameIndex = 0
    private var frameTimer = 0
    private val frameSpeed = 5   // im mniejsza wartość, tym szybsza animacja

    // ŁADOWANIE KLATEK ANIMACJI
    // Wczytuje kolejne pliki graficzne o nazwach prefix + index.
    // Jeśli plik nie istnieje — tworzy pustą bitmapę, aby uniknąć crasha.
    private fun loadFrames(prefix: String, count: Int): List<Bitmap> {
        val frames = mutableListOf<Bitmap>()

        for (i in 0 until count) {
            val resId = context.resources.getIdentifier(prefix + i, "drawable", context.packageName)

            // Wczytujemy bitmapę lub tworzymy pustą, jeśli nie istnieje.
            val bmp = if (resId != 0)
                BitmapFactory.decodeResource(context.resources, resId)
            else
                null

            // Skalujemy bitmapę do spriteSize.
            val finalBmp = if (bmp != null)
                Bitmap.createScaledBitmap(bmp, spriteSize.toInt(), spriteSize.toInt(), false)
            else
                Bitmap.createBitmap(spriteSize.toInt(), spriteSize.toInt(), Bitmap.Config.ARGB_8888)

            frames.add(finalBmp)
        }

        return frames
    }

    // GŁÓWNA LOGIKA GRACZA
    // Odpowiada za:
    // - ruch poziomy,
    // - grawitację,
    // - wybór animacji,
    // - przełączanie klatek animacji.
    fun update() {

        // RUCH POZIOMY
        x += velocityX

        // Ustawiamy kierunek patrzenia na podstawie prędkości.
        if (velocityX > 0) facingRight = true
        if (velocityX < 0) facingRight = false

        // GRAWITACJA
        velocityY += gravity
        y += velocityY

        // WYBÓR ANIMACJI
        // Logika animacji jest zależna od stanu gracza:
        // - jeśli spada → animacja skoku,
        // - jeśli się porusza → animacja biegu,
        // - jeśli stoi → animacja idle.
        val previousFrames = currentFrames

        currentFrames = when {
            isFalling -> if (facingRight) jumpRight else jumpLeft
            velocityX != 0f -> if (facingRight) runRight else runLeft
            else -> if (facingRight) idleRight else idleLeft
        }

        // Jeśli animacja się zmieniła — resetujemy licznik klatek.
        if (currentFrames !== previousFrames) {
            currentFrameIndex = 0
            frameTimer = 0
        }

        // PRZEŁĄCZANIE KLATEK ANIMACJI
        frameTimer++
        if (frameTimer >= frameSpeed) {
            frameTimer = 0
            currentFrameIndex++
            if (currentFrameIndex >= currentFrames.size)
                currentFrameIndex = 0
        }
    }

    // RYSOWANIE SPRITE’A
    // Sprite jest rysowany tak, aby hitbox był wyrównany z animacją.
    // y - (hitboxOffsetY / 2) + 20f to korekta wizualna — dopasowanie sprite’a do hitboxa.
    fun draw(canvas: Canvas) {
        canvas.drawBitmap(
            currentFrames[currentFrameIndex],
            x,
            y - (hitboxOffsetY / 2) + 20f,
            null
        )
    }

    // HITBOX — matematyczne granice kolizji.
    // GameView używa ich do wykrywania kolizji z platformami i monetami.
    fun hitboxLeft() = x + hitboxOffsetX
    fun hitboxRight() = hitboxLeft() + hitboxWidth
    fun hitboxTop() = y
    fun hitboxBottom() = y + hitboxHeight

    // RUCH POZIOMY
    fun moveLeft() { velocityX = -moveSpeed }
    fun moveRight() { velocityX = moveSpeed }
    fun stop() { velocityX = 0f }

    // SKOK
    // Skok jest możliwy tylko wtedy, gdy gracz stoi na platformie.
    fun jump() {
        if (!isFalling) {
            velocityY = jumpPower
            isFalling = true
        }
    }

    // LĄDOWANIE NA PLATFORMIE
    // Ustawiamy hitboxBottom = platformTop.
    // Dzięki temu gracz stoi idealnie na platformie.
    fun landOn(platformY: Float) {
        y = platformY - hitboxHeight
        velocityY = 0f
        isFalling = false
    }
}
