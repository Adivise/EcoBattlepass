package com.exanthiax.xbattlepass.utils

import com.exanthiax.xbattlepass.XBattlePass
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import com.exanthiax.xbattlepass.api.events.PlayerBPExpGainEvent
import com.exanthiax.xbattlepass.api.events.PlayerQuestCompleteEvent
import com.exanthiax.xbattlepass.api.events.PlayerPostRewardEvent
import com.exanthiax.xbattlepass.api.events.PlayerTierLevelUpEvent
import com.exanthiax.xbattlepass.api.getTier
import com.exanthiax.xbattlepass.api.giveBPExperience

class BattlePassListener(
    private val plugin: XBattlePass
): Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    fun handleBPLevelUp(event: PlayerTierLevelUpEvent) {
        val player = event.player

        if (event.player.getTier(event.battlepass) >= event.battlepass.maxLevel) {
            event.isCancelled = true
            return
        }

        event.player.sendMessage(
            plugin.langYml.getMessage("tier-up").replace(
                "%tier%", event.level.toString()
            )
        )

        SoundUtils.playIfEnabled(player, "sound.tier-up")
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun handleBPExp(event: PlayerBPExpGainEvent) {
        if (event.player.getTier(event.battlepass) >= event.battlepass.maxLevel) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun handleQuest(event: PlayerQuestCompleteEvent) {
        val player = event.player

        event.player.sendMessage(
            plugin.langYml.getMessage("quest-complete").replace(
                "%quest%", event.quest.getFormattedName(event.player)
            )
        )

        SoundUtils.playIfEnabled(player, "sound.quest-complete")

        event.player.giveBPExperience(
            event.quest.category.battlepass,
            event.quest.parent.tierPoints.toDouble(),
            true
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun handleReward(event: PlayerPostRewardEvent) {
        val player = event.player

        event.player.sendMessage(
            plugin.langYml.getMessage("reward-claim").replace(
                "%reward%", event.reward.getDisplayName(event.player)
            )
        )

        SoundUtils.playIfEnabled(player, "sound.reward-claim")
    }
}