package com.ccnetmc.referrals.commands

import com.ccnetmc.referrals.*
import com.ccnetmc.referrals.requirements.FirstJoinRequirement
import com.ccnetmc.referrals.requirements.PlaytimeRequirement
import com.ccnetmc.referrals.requirements.RankRequirement
import com.ccnetmc.referrals.requirements.StayedInTownRequirement
import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui
import com.github.stefvanschie.inventoryframework.gui.type.util.Gui
import com.github.stefvanschie.inventoryframework.pane.*
import com.github.stefvanschie.inventoryframework.pane.Pane.Priority
import com.github.stefvanschie.inventoryframework.pane.util.Pattern
import com.google.common.collect.Iterables
import com.palmergames.bukkit.towny.TownyAPI
import com.palmergames.bukkit.towny.TownyMessaging
import com.palmergames.bukkit.towny.`object`.Resident
import com.palmergames.bukkit.towny.`object`.Town
import com.palmergames.bukkit.towny.utils.MetaDataUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.math.ceil


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
            CoroutineScope((Dispatchers.IO)).launch {
                openOverviewGui(sender, resident, town)
            }
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

    private fun openOverviewGui(recruiterPlayer: Player, recruiterResident: Resident, town: Town) {
        val gui = ChestGui(6, "Recruitment Bonuses: Page 1")

        val paginatorPane = PaginatedPane(0, 0, 9, 5, Priority.HIGH)
        paginatorPane.setOnClick { it.isCancelled = true }
        gui.addPane(paginatorPane)

        var page = 0
        val pageResidents = Iterables.partition<Resident>(town.residents, paginatorPane.height * paginatorPane.length)
        for (residentsList in pageResidents) {
            val residentsPane = OutlinePane(0, 0, 9, 5)
            for (resident in residentsList) {
                if (resident === recruiterResident) {
                    continue
                }

                val residentPlayer = Bukkit.getOfflinePlayer(resident.name)
                residentsPane.addItem(GuiItem(getRequirementsItem(residentPlayer, resident)) {
                    it.isCancelled = true
                    openGui(recruiterPlayer, recruiterResident, town, resident)
                })
            }
            residentsPane.setOnClick { it.isCancelled = true }
            paginatorPane.addPane(page, residentsPane)
            gui.addPane(residentsPane)
            page += 1
        }

        paginatorPane.page = 0

        val fillerPaneItem = ItemStack(Material.WHITE_STAINED_GLASS_PANE)
        fillerPaneItem.editMeta { meta -> meta.displayName(Component.empty()) }
        val fillerPane = PatternPane(0, 0, paginatorPane.length, paginatorPane.height, Pattern("111111111", "111111111", "111111111", "111111111", "111111111"))
        fillerPane.bindItem('1', GuiItem(fillerPaneItem))
        fillerPane.setOnClick { it.isCancelled = true }
        gui.addPane(fillerPane)

        val bottomFillerPaneItem = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        bottomFillerPaneItem.editMeta { meta -> meta.displayName(Component.empty()) }
        val bottomFillerPane = PatternPane(0, 5, 9, 1, Priority.HIGH, Pattern("111010111"))
        bottomFillerPane.bindItem('1', GuiItem(bottomFillerPaneItem))
        bottomFillerPane.setOnClick { it.isCancelled = true }
        gui.addPane(bottomFillerPane)

        val nextPagePane = StaticPane(5, 5, 1, 1, Priority.HIGHEST)
        val previousPagePane = StaticPane(3, 5, 1, 1, Priority.HIGHEST)

        previousPagePane.addItem(createOverviewPrevPageButton(paginatorPane), 0, 0)
        previousPagePane.setOnClick {
            it.isCancelled = true
            if (paginatorPane.page > 0) {
                paginatorPane.page = paginatorPane.page - 1
                gui.title = "Recruitment Bonuses: Page ${paginatorPane.page + 1}"

                previousPagePane.addItem(createOverviewPrevPageButton(paginatorPane), 0, 0)
                nextPagePane.addItem(createOverviewNextPageButton(paginatorPane), 0, 0)
                recruiterPlayer.playSound(recruiterPlayer.location, Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f)
                gui.update()
            }
        }
        gui.addPane(previousPagePane)

        nextPagePane.addItem(createOverviewNextPageButton(paginatorPane), 0, 0)
        nextPagePane.setOnClick {
            if (paginatorPane.page < paginatorPane.pages - 1) {
                paginatorPane.page = paginatorPane.page + 1
                gui.title = "Recruitment Bonuses: Page ${paginatorPane.page + 1}"

                previousPagePane.addItem(createOverviewPrevPageButton(paginatorPane), 0, 0)
                nextPagePane.addItem(createOverviewNextPageButton(paginatorPane), 0, 0)
                recruiterPlayer.playSound(recruiterPlayer.location, Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f)
                gui.update()
            }
            it.isCancelled = true
        }
        gui.addPane(nextPagePane)

        // Show GUI synchronously
        Bukkit.getServer().scheduler.runTask(plugin, Runnable {
            gui.show(recruiterPlayer)
        })
    }

    private fun createOverviewNextPageButton(paginatedPane: PaginatedPane): GuiItem {
        val nextPageItemStack = if (paginatedPane.page < paginatedPane.pages - 1) {
            ItemStack(Material.LIME_CONCRETE_POWDER)
        } else {
            ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        }

        if (paginatedPane.page < paginatedPane.pages - 1) {
            nextPageItemStack.editMeta { meta -> meta.displayName(miniMessage.deserialize("<!i><green>Next Page")) }
        } else {
            nextPageItemStack.editMeta { meta -> meta.displayName(Component.empty()) }
        }

        return GuiItem(nextPageItemStack)
    }


    private fun createOverviewPrevPageButton(paginatedPane: PaginatedPane): GuiItem {
        val previousPageItemStack = if (paginatedPane.page > 0) {
            ItemStack(Material.RED_CONCRETE_POWDER)
        } else {
            ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        }

        if (paginatedPane.page > 0) {
            previousPageItemStack.editMeta { meta -> meta.displayName(miniMessage.deserialize("<!i><red>Previous Page")) }
        } else {
            previousPageItemStack.editMeta { meta -> meta.displayName(Component.empty()) }
        }

        return GuiItem(previousPageItemStack)
    }

    private fun openGui(recruiterPlayer: Player, recruiterResident: Resident, town: Town, recruitedResident: Resident) {
        val recruitedPlayer = Bukkit.getOfflinePlayer(recruitedResident.name)

        val gui = ChestGui(1, "Recruitment Bonus Reward")

        val fillerPattern = Pattern("111010111")
        val fillerPane = PatternPane(0, 0, 9, 1, Priority.LOWEST, fillerPattern)
        fillerPane.setOnClick { it.isCancelled = true }

        val fillerPaneItem = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        fillerPaneItem.editMeta { it.displayName(Component.empty()) }
        fillerPane.bindItem('1', GuiItem(fillerPaneItem))
        gui.addPane(fillerPane)

        val requirementsPane = StaticPane(3, 0, 1, 1)
        val requirementsItem = getRequirementsItem(recruitedPlayer, recruitedResident)
        val requirementsSatisfied = (requirementsItem.type === Material.LIME_STAINED_GLASS_PANE)

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

    private fun getRequirementsItem(player: OfflinePlayer, resident: Resident): ItemStack {
        val requirementsItem = ItemStack(Material.LIME_STAINED_GLASS_PANE)

        val requirements = listOf(
            StayedInTownRequirement(resident),
            FirstJoinRequirement(player),
            PlaytimeRequirement(player),
            RankRequirement(player)
        )

        var requirementsSatisfied = true
        val requirementLoreLines = mutableListOf<Component>()

        requirements.forEach {
            val success = it.evaluate()
            if (!success) requirementsSatisfied = false
            requirementLoreLines.add(it.message)
        }

        if (!requirementsSatisfied) {
            requirementsItem.type = Material.RED_STAINED_GLASS_PANE
        }

        requirementsItem.editMeta {
            it.lore(requirementLoreLines)
            if (MetaDataUtil.hasMeta(resident, recruitBonusClaimedField)) {
                requirementsItem.type = Material.GREEN_STAINED_GLASS_PANE
                it.displayName(miniMessage.deserialize("<dark_green><!i><b>✓</b> Already claimed bonus for ${resident.name}"))
                it.lore(listOf(miniMessage.deserialize("<!i><green>You have claimed the recruitment bonus"), miniMessage.deserialize("<!i><green>for this player!")))
            } else if (requirementsSatisfied) {
                it.displayName(miniMessage.deserialize("<#49de23><!i><b>✓</b> Requirements met for ${resident.name}"))
            } else {
                it.displayName(miniMessage.deserialize("<#d67273><!i><b>❌</b> Requirements not met for ${resident.name}"))
            }
        }

        return requirementsItem
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