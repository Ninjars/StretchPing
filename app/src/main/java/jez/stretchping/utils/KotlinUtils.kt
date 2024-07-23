package jez.stretchping.utils

/**
 * Only executes the getter function if the condition is met, unlike with takeIf()
 */
fun <T> getIf(condition: Boolean, getter: () -> T): T? =
    if (condition) getter() else null