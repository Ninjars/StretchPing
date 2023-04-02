package jez.stretchping.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.util.Consumer

@Composable
fun <T> rememberEventConsumer(consumer: Consumer<T>) =
    remember<(T) -> Unit>(consumer) { { consumer.accept(it) } }