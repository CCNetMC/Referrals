package com.ccnetmc.referrals

import com.ccnetmc.referrals.commands.ReferralsCommand
import com.ccnetmc.referrals.listeners.ReferralJoinListener
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.plugin.java.JavaPlugin

val miniMessage = MiniMessage.miniMessage()

class Referrals : JavaPlugin() {
    companion object {
        private lateinit var plugin: Referrals
        val instance: Referrals
            get() = plugin
    }

    override fun onEnable() {
        saveDefaultConfig()
        reloadConfig()

        server.pluginManager.registerEvents(ReferralJoinListener(), this)

        getCommand("recruitbonus")?.setExecutor(ReferralsCommand(this))
    }

    override fun reloadConfig() {
        super.reloadConfig()
        Config.loadValues(this)
    }
}