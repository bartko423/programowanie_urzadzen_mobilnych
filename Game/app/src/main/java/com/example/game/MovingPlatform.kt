package com.example.game

class MovingPlatform(
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    val minY: Float,
    val maxY: Float,
    val speed: Float
) {
    private var direction = 1

    fun update() {
        y += speed * direction
        if (y < minY || y > maxY) {
            direction *= -1
        }
    }
}
