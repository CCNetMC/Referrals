package com.ccnetmc.referrals

object Config {
    // Rewards
    var baseRewardPayment = 0.0
    var recruiteeRewardPayment = 0.0

    // Requirements
    var daysSinceFirstJoinLimit = 1000
    var minimumPlaytimeInHours = 1000

    fun loadValues(plugin: Referrals) {
        val config = plugin.config

        baseRewardPayment = config.getDouble("rewards.base-payment")
        recruiteeRewardPayment = config.getDouble("rewards.recruitee-payment")

        daysSinceFirstJoinLimit = config.getInt("requirements.days-since-first-join-limit")
        minimumPlaytimeInHours = config.getInt("requirements.playtime-in-hours")
    }
}