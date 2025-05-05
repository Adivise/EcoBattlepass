package com.exanthiax.xbattlepass.commands

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.util.toNiceString
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil
import com.exanthiax.xbattlepass.api.giveExactBPExperience
import com.exanthiax.xbattlepass.battlepass.BattlePasses
import com.exanthiax.xbattlepass.plugin

object GiveCommand: PluginCommand(
    plugin,
    "give",
    "xbattlepass.command.give",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        val playerString = args.firstOrNull() ?: run {
            sender.sendMessage(plugin.langYml.getMessage("player-required"))
            return
        }

        val player = Bukkit.getPlayer(playerString) ?: run {
            sender.sendMessage(plugin.langYml.getMessage("player-not-found"))
            return
        }

        val passString = args.getOrNull(1) ?: run {
            sender.sendMessage(plugin.langYml.getMessage("pass-required"))
            return
        }

        val pass = BattlePasses.getByID(passString) ?: run {
            sender.sendMessage(plugin.langYml.getMessage("pass-not-found"))
            return
        }

        val amountString = args.getOrNull(2) ?: run {
            sender.sendMessage(plugin.langYml.getMessage("amount-required"))
            return
        }

        val amount = amountString.toDoubleOrNull() ?: run {
            sender.sendMessage(plugin.langYml.getMessage("invalid-amount"))
            return
        }

        player.giveExactBPExperience(pass, amount)

        sender.sendMessage(plugin.langYml.getMessage("given-experience")
            .replace("%playername%", player.name)
            .replace("%amount%", amount.toNiceString())
            .replace("%pass%", pass.name)
        )
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return when(args.size) {
            1 -> StringUtil.copyPartialMatches(args.first(), Bukkit.getOnlinePlayers().map { it.name }, mutableListOf())
            2 -> StringUtil.copyPartialMatches(args.first(), BattlePasses.values().map { it.id }, mutableListOf())
            3 -> StringUtil.copyPartialMatches(args.first(), listOf("1", "10", "100", "1000"), mutableListOf())
            else -> emptyList()
        }
    }
}