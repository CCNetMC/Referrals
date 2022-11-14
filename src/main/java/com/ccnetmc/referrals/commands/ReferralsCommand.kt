package com.ccnetmc.referrals.commands

import com.ccnetmc.referrals.*
import com.ccnetmc.referrals.requirements.FirstJoinRequirement
import com.ccnetmc.referrals.requirements.PlaytimeRequirement
import com.ccnetmc.referrals.requirements.RankRequirement
import com.ccnetmc.referrals.requirements.StayedInTownRequirement
import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.pane.Pane
import com.github.stefvanschie.inventoryframework.pane.PatternPane
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import com.github.stefvanschie.inventoryframework.pane.util.Pattern
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

        val fillerPattern = Pattern("111010111")
        val fillerPane = PatternPane(0, 0, 9, 1, Pane.Priority.LOWEST, fillerPattern)
        fillerPane.setOnClick { it.isCancelled = true }

        val fillerPaneItem = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        fillerPaneItem.editMeta { it.displayName(Component.empty()) }
        fillerPane.bindItem('1', GuiItem(fillerPaneItem))
        gui.addPane(fillerPane)

        val requirementsPane = StaticPane(3, 0, 1, 1)
        val requirementsItem = ItemStack(Material.WRITABLE_BOOK)

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
                it.lore(listOf(
                    miniMessage.deserialize("<!i><gray>•</gray> <white>The <#edc187>town bank</#edc187> will receive: <#eded87>$${Config.baseRewardPayment.toInt()}"),
                    miniMessage.deserialize("<!i><gray>•</gray> <white><#edc187>${recruitedPlayer.name}</#edc187> will receive: <#eded87>$${Config.recruiteeRewardPayment.toInt()}"))
                )
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
                    town.account.deposit(Config.baseRewardPayment, "Recruitment bonus (mayor)")
                    recruitedResident.accountOrNull?.deposit(Config.recruiteeRewardPayment, "Recruitment bonus (recruitee)")

                    TownyMessaging.sendPrefixedTownMessage(town, "Earned $${Config.baseRewardPayment} in recruitment bonuses from ${recruitedResident.name}!")
                    if (recruitedPlayer.isOnline) {
                        (recruitedPlayer as Player).playSound(recruiterPlayer.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 3f, 2f)
                        recruitedPlayer.sendRichMessage("<green><b>✓</b></green> <#b7f0b6>You have earned <#b6f0ef>$${Config.recruiteeRewardPayment}</#b6f0ef> from the recruitment bonus reward!")
                    }
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