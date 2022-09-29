package com.ccnetmc.referrals

import com.palmergames.bukkit.towny.`object`.Resident
import com.palmergames.bukkit.towny.`object`.metadata.BooleanDataField
import com.palmergames.bukkit.towny.`object`.metadata.IntegerDataField
import com.palmergames.bukkit.towny.utils.MetaDataUtil

val hasLeftTownField = BooleanDataField("has_left_town")
val recruitBonusClaimedField = BooleanDataField("recruit_bonus_claimed")
val numberOfResidentsRecruitedField = IntegerDataField("number_of_residents_recruited")

fun Resident.hasLeftTown(): Boolean {
    return MetaDataUtil.hasMeta(this, hasLeftTownField)
}

fun Resident.incrementNumRecruited() {
    val numRecruited = MetaDataUtil.getInt(this, numberOfResidentsRecruitedField).plus(1)
    MetaDataUtil.setInt(this, numberOfResidentsRecruitedField, numRecruited, true)
}