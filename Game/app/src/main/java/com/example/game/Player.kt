package com.example.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas

class Player(
    var x: Float,
    var y: Float,   // y = HITBOX TOP
    private val context: Context
) {

    val spriteSize = 128f
    val scale = spriteSize / 64f

    val hitboxWidth = 32f * scale
    val hitboxHeight = 46f * scale
    val hitboxOffsetY = 18f * scale
    val hitboxOffsetX = (spriteSize - hitboxWidth) / 2f

    var velocityY = 0f
    var velocityX = 0f

    private val gravity = 1.2f
    private val jumpPower = -20f
    private val moveSpeed = 10f

    var isFalling = true
    private var facingRight = true

    private val idleRight = loadFrames("idle_right_", 8)
    private val idleLeft = loadFrames("idle_left_", 8)
    private val runRight = loadFrames("run_right_", 6)
    private val runLeft = loadFrames("run_left_", 6)
    private val jumpRight = loadFrames("jump_right_", 8)
    private val jumpLeft = loadFrames("jump_left_", 8)

    private var currentFrames = idleRight
    private var currentFrameIndex = 0
    private var frameTimer = 0
    private val frameSpeed = 5

    private fun loadFrames(prefix: String, count: Int): List<Bitmap> {
        val frames = mutableListOf<Bitmap>()
        for (i in 0 until count) {
            val resId = context.resources.getIdentifier(prefix + i, "drawable", context.packageName)
            val bmp = if (resId != 0) BitmapFactory.decodeResource(context.resources, resId) else null
            val finalBmp = if (bmp != null)
                Bitmap.createScaledBitmap(bmp, spriteSize.toInt(), spriteSize.toInt(), false)
            else
                Bitmap.createBitmap(spriteSize.toInt(), spriteSize.toInt(), Bitmap.Config.ARGB_8888)
            frames.add(finalBmp)
        }
        return frames
    }

    fun update() {
        x += velocityX

        if (velocityX > 0) facingRight = true
        if (velocityX < 0) facingRight = false

        velocityY += gravity
        y += velocityY

        val previousFrames = currentFrames

        currentFrames = when {
            isFalling -> if (facingRight) jumpRight else jumpLeft
            velocityX != 0f -> if (facingRight) runRight else runLeft
            else -> if (facingRight) idleRight else idleLeft
        }

        if (currentFrames !== previousFrames) {
            currentFrameIndex = 0
            frameTimer = 0
        }

        frameTimer++
        if (frameTimer >= frameSpeed) {
            frameTimer = 0
            currentFrameIndex++
            if (currentFrameIndex >= currentFrames.size) currentFrameIndex = 0
        }
    }

    fun draw(canvas: Canvas) {
        // spriteTop = hitboxTop - hitboxOffsetY
        canvas.drawBitmap(
            currentFrames[currentFrameIndex],
            x,
            y - (hitboxOffsetY / 2) + 20f,
            null
        )
    }

    fun hitboxLeft() = x + hitboxOffsetX
    fun hitboxRight() = hitboxLeft() + hitboxWidth
    fun hitboxTop() = y
    fun hitboxBottom() = y + hitboxHeight

    fun moveLeft() { velocityX = -moveSpeed }
    fun moveRight() { velocityX = moveSpeed }
    fun stop() { velocityX = 0f }

    fun jump() {
        if (!isFalling) {
            velocityY = jumpPower
            isFalling = true
        }
    }

    fun landOn(platformY: Float) {
        // hitboxBottom = platformTop
        y = platformY - hitboxHeight
        velocityY = 0f
        isFalling = false
    }
}
