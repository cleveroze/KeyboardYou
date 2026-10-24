package com.keyboardyou.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring

/** Пружины в духе Material 3 Expressive motion scheme. */
object Motion {
    /** Быстрое пространственное движение с отскоком — размеры, позиции, формы. */
    fun <T> fastSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.6f, stiffness = 800f)

    /** Обычное пространственное движение с отскоком. */
    fun <T> spatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.7f, stiffness = 380f)

    /** Эффекты (цвет, прозрачность) — без отскока. */
    fun <T> effects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)
}
