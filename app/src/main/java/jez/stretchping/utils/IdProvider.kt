package jez.stretchping.utils

import java.util.UUID
import javax.inject.Inject

class IdProvider @Inject constructor() {
    fun getId() = UUID.randomUUID().toString()
}
