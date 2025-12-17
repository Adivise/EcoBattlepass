package com.exanthiax.xbattlepass.commands.dynamic

import com.exanthiax.xbattlepass.battlepass.BattlePass
import com.exanthiax.xbattlepass.commands.helpers.Messages
import com.exanthiax.xbattlepass.gui.BattlePassGUI
import com.exanthiax.xbattlepass.gui.BattleTiersGUI
import com.exanthiax.xbattlepass.gui.QuestsGUI
import com.exanthiax.xbattlepass.plugin
import com.willfp.eco.core.command.impl.PluginCommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil

class DynamicPassCommand(
    private val pass: BattlePass,
    cmd: String
) : PluginCommand(
    plugin,
    cmd,
    "xbattlepass.command.$cmd",
    true  // player-only
) {
    override fun onExecute(sender: Player, args: MutableList<String>) {
        when (args.getOrNull(0)?.lowercase()) {
            null -> {
                BattlePassGUI.createAndOpen(sender, pass)
            }

            "tiers" -> {
                BattleTiersGUI.createAndOpen(sender, pass)
            }

            "quests" -> {
                val categoryId = args.getOrNull(1) ?: run {
                    Messages.sendCategoryRequired(sender)
                    return
                }

                val category = pass.categories.firstOrNull {
                    it.id.equals(categoryId, ignoreCase = true)
                } ?: run {
                    Messages.sendInvalidCategory(sender)
                    return
                }

                QuestsGUI(sender, category, wasBack = false).open()
            }

            else -> {
                Messages.sendDynamicPassUsage(sender)
            }
        }
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return when (args.size) {
            1 -> StringUtil.copyPartialMatches(
                args[0],
                listOf("tiers", "quests"),
                mutableListOf()
            )
            2 -> {
                if (args[0].equals("quests", ignoreCase = true)) {
                    StringUtil.copyPartialMatches(
                        args[1],
                        pass.categories.map { it.id },
                        mutableListOf()
                    )
                } else emptyList()
            }
            else -> emptyList()
        }
    }
}