package com.exanthiax.xbattlepass.commands

import com.exanthiax.xbattlepass.battlepass.BattlePasses
import com.exanthiax.xbattlepass.commands.helpers.Messages
import com.exanthiax.xbattlepass.commands.helpers.resolveBattlePass
import com.exanthiax.xbattlepass.gui.BattleTiersGUI
import com.exanthiax.xbattlepass.plugin
import com.willfp.eco.core.command.impl.PluginCommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil

object TiersCommand : PluginCommand(
    plugin,
    "tiers",
    "xbattlepass.command.tier",
    true
) {
    override fun onExecute(sender: Player, args: MutableList<String>) {
        if (args.isEmpty()) {
            Messages.sendTiersUsage(sender)
            return
        }

        val pass = (sender as CommandSender).resolveBattlePass(args.getOrNull(0)) ?: run {
            Messages.sendTiersUsage(sender)
            return
        }

        BattleTiersGUI.createAndOpen(sender, pass)
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return when (args.size) {
            1 -> StringUtil.copyPartialMatches(
                args[0],
                BattlePasses.values().map { it.id },
                mutableListOf()
            )
            else -> emptyList()
        }
    }
}