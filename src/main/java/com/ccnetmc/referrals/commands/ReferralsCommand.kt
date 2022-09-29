package com.ccnetmc.referrals.commands

import com.ccnetmc.referrals.*
import com.ccnetmc.referrals.requirements.FirstJoinRequirement
import com.ccnetmc.referrals.requirements.PlaytimeRequirement
import com.ccnetmc.referrals.requirements.RankRequirement
import com.ccnetmc.referrals.requirements.StayedInTownRequirement
import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import com.palmergames.bukkit.towny.TownyAPI
import com.palmergames.bukkit.towny.TownyMessaging
import com.palmergames.bukkit.towny.`object`.Resident
import com.palmergames.bukkit.towny.`object`.Town
import com.palmergames.bukkit.towny.utils.MetaDataUtil
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*


class ReferralsCommand(val plugin: Referrals) : TabExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendRichMessage("<red>This command can only be used by players.")
            return true
        }

        val resident = TownyAPI.getInstance().getResident(sender)
        val town = TownyAPI.getInstance().getTown(sender)
        if (resident == null || town == null) {
            sender.sendRichMessage("<red>Only town mayors can use this command.")
            return true
        }

        if (args.isEmpty()) {
            sender.sendRichMessage("<red>Usage: /recruitbonus <player name>")
            return true
        }

        val recruitName = args[0]
        if (recruitName.equals(sender.name, false)) {
            sender.sendRichMessage("<red>Nice try, but you can't claim a recruitment bonus for yourself!")
            return true
        }

        val recruitResident = TownyAPI.getInstance().getResident(recruitName)
        if (recruitResident == null || !town.residents.contains(recruitResident)) {
            sender.sendRichMessage("<red>Your town does not have a resident called $recruitName.")
            return true
        }

        if (MetaDataUtil.hasMeta(recruitResident, recruitBonusClaimedField)) {
            sender.sendRichMessage("<red>A recruitment bonus has already been claimed for this resident!")
            return true
        }

        // Open GUI
        CoroutineScope((Dispatchers.IO)).launch {
            openGui(sender, resident, town, recruitResident)
        }
        return true
    }

    private suspend fun openGui(recruiterPlayer: Player, recruiterResident: Resident, town: Town, recruitedResident: Resident) = coroutineScope {
        val recruitedPlayer = Bukkit.getOfflinePlayer(recruitedResident.name)

        val gui = ChestGui(1, "Recruitment Bonus Reward")
        val requirementsPane = StaticPane(3, 0, 1, 1)
        val requirementsItem = ItemStack(Material.PAPER)

        val requirements = listOf(
            StayedInTownRequirement(recruitedResident),
            FirstJoinRequirement(recruitedPlayer),
            PlaytimeRequirement(recruitedPlayer),
            RankRequirement(recruitedPlayer)
        )
        var requirementsSatisfied = true
        val requirementLoreLines = mutableListOf<Component>()

        launch {
            requirements.forEach {
                launch {
                    val success = it.evaluate()
                    if (!success) requirementsSatisfied = false
                    requirementLoreLines.add(it.message)
                }
            }
        }.invokeOnCompletion {
            requirementsItem.editMeta {
                it.lore(requirementLoreLines)
                if (requirementsSatisfied) {
                    it.displayName(miniMessage.deserialize("<#49de23><!i><b>✓</b> Requirements met for ${recruitedResident.name}"))
                } else {
                    it.displayName(miniMessage.deserialize("<#d67273><!i><b>❌</b> Requirements not met for ${recruitedResident.name}"))
                }
            }
            requirementsPane.addItem(GuiItem(requirementsItem), 0, 0)
            requirementsPane.setOnClick {
                it.isCancelled = true
            }
            gui.addPane(requirementsPane)

            // Add redemption item
            val redeemRewardPane = StaticPane(5, 0, 1, 1)
            val redeemRewardItem = if (requirementsSatisfied) ItemStack(Material.LIME_STAINED_GLASS_PANE) else ItemStack(Material.BARRIER)
            redeemRewardItem.editMeta {
                it.lore(listOf(miniMessage.deserialize("<!i><gray>Reward: <yellow>$${Config.baseRewardPayment.toInt()}")))
                if (requirementsSatisfied) {
                    it.displayName(miniMessage.deserialize("<#49de23><!i><b>✓</b> Click to redeem reward"))
                }
                else {
                    it.displayName(miniMessage.deserialize("<#d67273><!i><b>❌</b> Cannot redeem reward"))
                }
            }
            redeemRewardPane.addItem(GuiItem(redeemRewardItem), 0, 0)
            redeemRewardPane.setOnClick {
                it.isCancelled = true
                // Give rewards
                if (requirementsSatisfied) {
                    town.account.deposit(Config.baseRewardPayment, "Recruitment bonus.")
                    TownyMessaging.sendPrefixedTownMessage(town, "Earned $${Config.baseRewardPayment} in recruitment bonuses from ${recruitedResident.name}!")
                    plugin.logger.info("${recruiterPlayer.name} of the town of ${town.name} earned a recruitment bonus for ${recruitedResident.name}")

                    MetaDataUtil.setBoolean(recruitedResident, recruitBonusClaimedField, true, true)
                    recruiterResident.incrementNumRecruited()
                    recruiterPlayer.playSound(recruiterPlayer.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f)

                    it.inventory.close()
                }
            }
            gui.addPane(redeemRewardPane)

            // Show GUI synchronously
            Bukkit.getServer().scheduler.runTask(plugin, Runnable {
                gui.show(recruiterPlayer)
            })
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): MutableList<String>? {
        // Return list of online players
        if (args.size == 1 && sender is Player) {
            val town = TownyAPI.getInstance().getResident(sender)?.townOrNull ?: return Collections.emptyList()
            return TownyAPI.getResidentsOfTownStartingWith(town.name, args[0])
        }
        return Collections.emptyList()
    }
}