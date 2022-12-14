package com.ccnetmc.referrals.requirements

import com.ccnetmc.referrals.Config
import com.palmergames.bukkit.towny.`object`.Resident
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.OfflinePlayer
import kotlin.math.round

class FirstJoinRequirement(private val recruitedResident: Resident) : Requirement() {
    override fun evaluate(): Boolean {
        val joinedTimestamp = recruitedResident.registered
        val daysSinceJoin = (System.currentTimeMillis() - joinedTimestamp) / 86400000.0
        val isNewPlayer = (daysSinceJoin <= Config.daysSinceFirstJoinLimit)
        message = miniMessage.deserialize("<white><!i>• Joined in the last <aqua>${Config.daysSinceFirstJoinLimit}</aqua> days? <b><value>",
            TagResolver.resolver(
                Placeholder.parsed("value", if (!isNewPlayer) "<red>❌ <!b><#d67273>(Joined ${round(daysSinceJoin).toInt()} days ago)" else "<green>✓")
            )
        )
        return isNewPlayer
    }
}