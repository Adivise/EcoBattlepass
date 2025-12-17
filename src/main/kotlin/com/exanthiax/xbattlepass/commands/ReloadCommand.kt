package com.exanthiax.xbattlepass.commands

import com.exanthiax.xbattlepass.api.giveExactBPExperience
import com.exanthiax.xbattlepass.api.giveExactBPTiers
import com.exanthiax.xbattlepass.api.giveTaskExperience
import com.exanthiax.xbattlepass.battlepass.BattlePasses
import com.exanthiax.xbattlepass.categories.Categories
import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.util.NumberUtils
import com.willfp.eco.util.StringUtils
import org.bukkit.command.CommandSender
import com.exanthiax.xbattlepass.plugin
import com.willfp.eco.util.toNiceString
import org.bukkit.Bukkit
import org.bukkit.util.StringUtil

object ReloadCommand: PluginCommand(
    plugin,
    "reload",
    "xbattlepass.command.reload",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        sender.sendMessage(
            plugin.langYml.getMessage("reloaded", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                .replace("%time%", NumberUtils.format(plugin.reloadWithTime().toDouble()))
        )
    }
}