package com.ccnetmc.referrals.requirements

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

abstract class Requirement() {
    val miniMessage = MiniMessage.miniMessage()
    lateinit var message: Component

    /**
     * @return true if the requirement is met, false if not.
     */
    abstract fun evaluate(): Boolean
}