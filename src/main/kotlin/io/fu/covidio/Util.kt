package io.fu.covidio

import org.dyn4j.geometry.Vector2

fun Vector2.fastNormalize(): Vector2 {
    val lenSquared = this.magnitudeSquared
    val inverseSquareRoot = fiSqrt(lenSquared)
    return this.multiply(inverseSquareRoot)
}

// https://en.wikipedia.org/wiki/Fast_inverse_square_root
fun fiSqrt(x: Float): Float {
    val xHalf = 0.5f * x

    val i: Int = java.lang.Float.floatToIntBits(x)
        .let { 0x5f3759df - (it shr 1) }

    return java.lang.Float.intBitsToFloat(i)
        .let { it * (1.5f - xHalf * it * it) }
}

// https://en.wikipedia.org/wiki/Fast_inverse_square_root
fun fiSqrt(x: Double): Double {
    val xHalf = 0.5f * x

    val i: Long = java.lang.Double.doubleToLongBits(x)
        .let { 0x5fe6ec85e7de30daL - (it shr 1) }

    return java.lang.Double.longBitsToDouble(i)
        .let { it * (1.5f - xHalf * it * it) }
}
