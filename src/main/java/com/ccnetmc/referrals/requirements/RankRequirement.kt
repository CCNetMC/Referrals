package com.ccnetmc.referrals.requirements

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.luckperms.api.LuckPermsProvider
import org.bukkit.OfflinePlayer

class RankRequirement(private val recruitedPlayer: OfflinePlayer) : Requirement() {
    override fun evaluate(): Boolean {
        val luckPerms = LuckPermsProvider.get()

        // In the plugin, Requirement#evaluate is only run from a coroutine, so we are not blocking the main thread:
        val luckPermsUser = luckPerms.userManager.loadUser(recruitedPlayer.uniqueId).join()

        val satisfiesRequirement: Boolean =
            luckPermsUser?.getInheritedGroups(luckPermsUser.queryOptions)
                ?.asSequence()
                ?.map { group -> group.name }
                ?.toSet()
                ?.contains("voyager")
                ?: false

        message = miniMessage.deserialize(
            "<white><!i>• Has <aqua>Voyager</aqua> rank or above? <b><value>",
            TagResolver.resolver(
                Placeholder.parsed("value", if (!satisfiesRequirement) "<red>❌" else "<green>✓")
            )
        )
        return satisfiesRequirement
    }
}