package com.ccnetmc.referrals.requirements

import com.ccnetmc.referrals.Config
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.OfflinePlayer
import org.bukkit.Statistic
import kotlin.math.floor

class PlaytimeRequirement(private val recruitedPlayer: OfflinePlayer) : Requirement() {
    override fun evaluate(): Boolean {
        val hoursPlayed = recruitedPlayer.getStatistic(Statistic.PLAY_ONE_MINUTE) / 72000.0
        val hasPlaytime = (hoursPlayed >= Config.minimumPlaytimeInHours)
        message = miniMessage.deserialize("<white><!i>• Has played more than <aqua>${Config.minimumPlaytimeInHours}</aqua> hours? <b><value>",
            TagResolver.resolver(
                Placeholder.parsed("value", if (!hasPlaytime) "<red>❌ <!b><#d67273>(${floor(hoursPlayed).toInt()} hours)" else "<green>✓")
            )
        )
        return hasPlaytime
    }
}