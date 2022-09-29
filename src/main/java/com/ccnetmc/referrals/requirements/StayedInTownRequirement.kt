package com.ccnetmc.referrals.requirements

import com.ccnetmc.referrals.hasLeftTown
import com.palmergames.bukkit.towny.`object`.Resident
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

class StayedInTownRequirement(private val recruitedResident: Resident) : Requirement() {
    override fun evaluate(): Boolean {
        val hasLeft = recruitedResident.hasLeftTown()
        message = miniMessage.deserialize("<white><!i>• Has <aqua>only</aqua> been in your town? <b><value>",
            TagResolver.resolver(
                Placeholder.parsed("value", if (hasLeft) "<red>❌" else "<green>✓")
            )
        )
        return !hasLeft
    }
}