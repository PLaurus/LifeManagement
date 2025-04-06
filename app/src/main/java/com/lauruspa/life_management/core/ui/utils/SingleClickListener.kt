package com.lauruspa.life_management.core.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.lauruspa.life_management.core.utils.date.toDelayMillis
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val DEFAULT_WINDOW_MS = 1_000L
private val DEFAULT_WINDOW_DURATION = DEFAULT_WINDOW_MS.milliseconds

@Composable
fun singleClickHandlerMs(windowMs: Long = DEFAULT_WINDOW_MS): (() -> Unit) -> Unit {
    return remember {
        object : (() -> Unit) -> Unit {
            val actualWindowMs = windowMs.coerceAtLeast(0)
            var lastEmission = 0L
            override fun invoke(listener: () -> Unit) {
                val currentTime = System.currentTimeMillis()
                val mayEmit = currentTime - lastEmission > actualWindowMs
                if (mayEmit) {
                    lastEmission = currentTime
                    listener()
                }
            }
        }
    }
}

@Composable
fun singleClickHandler(window: Duration = DEFAULT_WINDOW_DURATION): (() -> Unit) -> Unit {
    val windowMs = remember(window) { window.toDelayMillis() }
    return singleClickHandlerMs(windowMs)
}

@Composable
fun singleClickListenerMs(
    windowMs: Long = DEFAULT_WINDOW_MS,
    listener: () -> Unit
): () -> Unit {
    val clicksHandler = singleClickHandlerMs(windowMs)
    return { clicksHandler { listener() } }
}

@Suppress("unused")
@Composable
fun singleClickListener(
    window: Duration = DEFAULT_WINDOW_DURATION,
    listener: () -> Unit
): () -> Unit {
    return singleClickListenerMs(remember(window) { window.toDelayMillis() }, listener)
}

@Composable
fun <T> singleClickListenerMs(
    windowMs: Long = DEFAULT_WINDOW_MS,
    listener: (T) -> Unit
): (T) -> Unit {
    val clicksHandler = singleClickHandlerMs(windowMs)
    return { t -> clicksHandler { listener(t) } }
}

@Suppress("unused")
@Composable
fun <T> singleClickListener(
    window: Duration = DEFAULT_WINDOW_DURATION,
    listener: (T) -> Unit
): (T) -> Unit {
    return singleClickListenerMs(remember(window) { window.toDelayMillis() }, listener)
}

@Composable
fun <T1, T2> singleClickListenerMs(
    windowMs: Long = DEFAULT_WINDOW_MS,
    listener: (T1, T2) -> Unit
): (T1, T2) -> Unit {
    val clicksHandler = singleClickHandlerMs(windowMs)
    return { t1, t2 -> clicksHandler { listener(t1, t2) } }
}

@Suppress("unused")
@Composable
fun <T1, T2> singleClickListener(
    window: Duration = DEFAULT_WINDOW_DURATION,
    listener: (T1, T2) -> Unit
): (T1, T2) -> Unit {
    return singleClickListenerMs(remember(window) { window.toDelayMillis() }, listener)
}

@Composable
fun <T1, T2, T3> singleClickListenerMs(
    windowMs: Long = DEFAULT_WINDOW_MS,
    listener: (T1, T2, T3) -> Unit
): (T1, T2, T3) -> Unit {
    val clicksHandler = singleClickHandlerMs(windowMs)
    return { t1, t2, t3 -> clicksHandler { listener(t1, t2, t3) } }
}

@Suppress("unused")
@Composable
fun <T1, T2, T3> singleClickListener(
    window: Duration = DEFAULT_WINDOW_DURATION,
    listener: (T1, T2, T3) -> Unit
): (T1, T2, T3) -> Unit {
    return singleClickListenerMs(remember(window) { window.toDelayMillis() }, listener)
}

@Composable
fun <T1, T2, T3, T4> singleClickListenerMs(
    windowMs: Long = DEFAULT_WINDOW_MS,
    listener: (T1, T2, T3, T4) -> Unit
): (T1, T2, T3, T4) -> Unit {
    val clicksHandler = singleClickHandlerMs(windowMs)
    return { t1, t2, t3, t4 -> clicksHandler { listener(t1, t2, t3, t4) } }
}

@Suppress("unused")
@Composable
fun <T1, T2, T3, T4> singleClickListener(
    window: Duration = DEFAULT_WINDOW_DURATION,
    listener: (T1, T2, T3, T4) -> Unit
): (T1, T2, T3, T4) -> Unit {
    return singleClickListenerMs(remember(window) { window.toDelayMillis() }, listener)
}

@Composable
fun <T1, T2, T3, T4, T5> singleClickListenerMs(
    windowMs: Long = DEFAULT_WINDOW_MS,
    listener: (T1, T2, T3, T4, T5) -> Unit
): (T1, T2, T3, T4, T5) -> Unit {
    val clicksHandler = singleClickHandlerMs(windowMs)
    return { t1, t2, t3, t4, t5 ->
        clicksHandler { listener(t1, t2, t3, t4, t5) }
    }
}

@Suppress("unused")
@Composable
fun <T1, T2, T3, T4, T5> singleClickListener(
    window: Duration = DEFAULT_WINDOW_DURATION,
    listener: (T1, T2, T3, T4, T5) -> Unit
): (T1, T2, T3, T4, T5) -> Unit {
    return singleClickListenerMs(remember(window) { window.toDelayMillis() }, listener)
}


@Composable
fun <T1, T2, T3, T4, T5, T6> singleClickListenerMs(
    windowMs: Long = DEFAULT_WINDOW_MS,
    listener: (T1, T2, T3, T4, T5, T6) -> Unit
): (T1, T2, T3, T4, T5, T6) -> Unit {
    val clicksHandler = singleClickHandlerMs(windowMs)
    return { t1, t2, t3, t4, t5, t6 ->
        clicksHandler { listener(t1, t2, t3, t4, t5, t6) }
    }
}

@Suppress("unused")
@Composable
fun <T1, T2, T3, T4, T5, T6> singleClickListener(
    window: Duration = DEFAULT_WINDOW_DURATION,
    listener: (T1, T2, T3, T4, T5, T6) -> Unit
): (T1, T2, T3, T4, T5, T6) -> Unit {
    return singleClickListenerMs(remember(window) { window.toDelayMillis() }, listener)
}

@Composable
fun <T1, T2, T3, T4, T5, T6, T7> singleClickListenerMs(
    windowMs: Long = DEFAULT_WINDOW_MS,
    listener: (T1, T2, T3, T4, T5, T6, T7) -> Unit
): (T1, T2, T3, T4, T5, T6, T7) -> Unit {
    val clicksHandler = singleClickHandlerMs(windowMs)
    return { t1, t2, t3, t4, t5, t6, t7 ->
        clicksHandler { listener(t1, t2, t3, t4, t5, t6, t7) }
    }
}

@Suppress("unused")
@Composable
fun <T1, T2, T3, T4, T5, T6, T7> singleClickListener(
    window: Duration = DEFAULT_WINDOW_DURATION,
    listener: (T1, T2, T3, T4, T5, T6, T7) -> Unit
): (T1, T2, T3, T4, T5, T6, T7) -> Unit {
    return singleClickListenerMs(remember(window) { window.toDelayMillis() }, listener)
}

@Composable
fun <T1, T2, T3, T4, T5, T6, T7, T8> singleClickListenerMs(
    windowMs: Long = DEFAULT_WINDOW_MS,
    listener: (T1, T2, T3, T4, T5, T6, T7, T8) -> Unit
): (T1, T2, T3, T4, T5, T6, T7, T8) -> Unit {
    val clicksHandler = singleClickHandlerMs(windowMs)
    return { t1, t2, t3, t4, t5, t6, t7, t8 ->
        clicksHandler { listener(t1, t2, t3, t4, t5, t6, t7, t8) }
    }
}

@Suppress("unused")
@Composable
fun <T1, T2, T3, T4, T5, T6, T7, T8> singleClickListener(
    window: Duration = DEFAULT_WINDOW_DURATION,
    listener: (T1, T2, T3, T4, T5, T6, T7, T8) -> Unit
): (T1, T2, T3, T4, T5, T6, T7, T8) -> Unit {
    return singleClickListenerMs(remember(window) { window.toDelayMillis() }, listener)
}

@Composable
fun <T1, T2, T3, T4, T5, T6, T7, T8, T9> singleClickListenerMs(
    windowMs: Long = DEFAULT_WINDOW_MS,
    listener: (T1, T2, T3, T4, T5, T6, T7, T8, T9) -> Unit
): (T1, T2, T3, T4, T5, T6, T7, T8, T9) -> Unit {
    val clicksHandler = singleClickHandlerMs(windowMs)
    return { t1, t2, t3, t4, t5, t6, t7, t8, t9 ->
        clicksHandler { listener(t1, t2, t3, t4, t5, t6, t7, t8, t9) }
    }
}

@Suppress("unused")
@Composable
fun <T1, T2, T3, T4, T5, T6, T7, T8, T9> singleClickListener(
    window: Duration = DEFAULT_WINDOW_DURATION,
    listener: (T1, T2, T3, T4, T5, T6, T7, T8, T9) -> Unit
): (T1, T2, T3, T4, T5, T6, T7, T8, T9) -> Unit {
    return singleClickListenerMs(remember(window) { window.toDelayMillis() }, listener)
}