package com.ccnetmc.referrals.listeners

import com.ccnetmc.referrals.hasLeftTownField
import com.palmergames.bukkit.towny.event.town.TownKickEvent
import com.palmergames.bukkit.towny.event.town.TownLeaveEvent
import com.palmergames.bukkit.towny.utils.MetaDataUtil
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class ReferralJoinListener : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onTownKickResident(event: TownKickEvent) {
        MetaDataUtil.setBoolean(event.kickedResident, hasLeftTownField, true, true)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onResidentLeaveTown(event: TownLeaveEvent) {
        MetaDataUtil.setBoolean(event.resident, hasLeftTownField, true, true)
    }

}