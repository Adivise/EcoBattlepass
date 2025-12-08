package com.exanthiax.xbattlepass.commands

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.util.toNiceString
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil
import com.exanthiax.xbattlepass.api.giveExactBPExperience
import com.exanthiax.xbattlepass.api.giveExactBPTiers
import com.exanthiax.xbattlepass.api.setTaskProgress
import com.exanthiax.xbattlepass.api.taskProgress
import com.exanthiax.xbattlepass.battlepass.BattlePasses
import com.exanthiax.xbattlepass.categories.Categories
import com.exanthiax.xbattlepass.plugin

object GiveCommand: PluginCommand(
    plugin,
    "give",
    "xbattlepass.command.give",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        val playerString = args.getOrNull(0) ?: run {
            sender.sendMessage(plugin.langYml.getMessage("player-required"))
            return
        }

        val players = if (playerString.equals("all", ignoreCase = true)) {
            Bukkit.getOnlinePlayers().toList()
        } else {
            val player = Bukkit.getPlayer(playerString)
            if (player == null) {
                sender.sendMessage(plugin.langYml.getMessage("player-not-found"))
                return
            }
            listOf(player)
        }

        val passString = args.getOrNull(1) ?: run {
            sender.sendMessage(plugin.langYml.getMessage("pass-required"))
            return
        }

        val pass = BattlePasses.getByID(passString) ?: run {
            sender.sendMessage(plugin.langYml.getMessage("pass-not-found"))
            return
        }

        val mode = args.getOrNull(2)?.lowercase() ?: run {
            sender.sendMessage(plugin.langYml.getMessage("type-required"))
            return
        }

        when (mode) {
            "xp", "experience" -> {
                val amountString = args.getOrNull(3) ?: run {
                    sender.sendMessage(plugin.langYml.getMessage("amount-required"))
                    return
                }
                val amount = amountString.toDoubleOrNull() ?: run {
                    sender.sendMessage(plugin.langYml.getMessage("invalid-amount"))
                    return
                }

                for (player in players) {
                    player.giveExactBPExperience(pass, amount)

                    sender.sendMessage(plugin.langYml.getMessage("given-experience")
                        .replace("%playername%", player.name)
                        .replace("%amount%", amount.toNiceString())
                        .replace("%pass%", pass.name)
                    )

                    player.sendMessage(plugin.langYml.getMessage("received-experience")
                        .replace("%amount%", amount.toNiceString())
                        .replace("%pass%", pass.name)
                    )
                }
            }

            "tier", "tiers" -> {
                val amountString = args.getOrNull(3) ?: run {
                    sender.sendMessage(plugin.langYml.getMessage("amount-required"))
                    return
                }
                val amount = amountString.toIntOrNull() ?: run {
                    sender.sendMessage(plugin.langYml.getMessage("invalid-amount"))
                    return
                }

                for (player in players) {
                    player.giveExactBPTiers(pass, amount)

                    sender.sendMessage(plugin.langYml.getMessage("given-tiers")
                        .replace("%playername%", player.name)
                        .replace("%amount%", amount.toString())
                        .replace("%pass%", pass.name)
                    )

                    player.sendMessage(plugin.langYml.getMessage("received-tiers")
                        .replace("%amount%", amount.toString())
                        .replace("%pass%", pass.name)
                    )
                }
            }

            "task-xp", "taskxp" -> {
                val categoryString = args.getOrNull(3) ?: run {
                    sender.sendMessage(plugin.langYml.getMessage("category-required"))
                    return
                }
                val questString = args.getOrNull(4) ?: run {
                    sender.sendMessage(plugin.langYml.getMessage("quest-required"))
                    return
                }
                val taskString = args.getOrNull(5) ?: run {
                    sender.sendMessage(plugin.langYml.getMessage("task-required"))
                    return
                }
                val amountString = args.getOrNull(6) ?: run {
                    sender.sendMessage(plugin.langYml.getMessage("amount-required"))
                    return
                }

                val category = Categories.getByID(categoryString) ?: run {
                    sender.sendMessage(plugin.langYml.getMessage("invalid-category"))
                    return
                }

                if (category.battlepass != pass) {
                    sender.sendMessage(plugin.langYml.getMessage("invalid-category"))
                    return
                }

                val activeQuest = category.quests.find { it.parent.id.equals(questString, true) } ?: run {
                    sender.sendMessage(plugin.langYml.getMessage("invalid-quest"))
                    return
                }

                val activeTask = activeQuest.tasks.find { it.parent.id.equals(taskString, true) } ?: run {
                    sender.sendMessage(plugin.langYml.getMessage("invalid-task"))
                    return
                }

                val amount = amountString.toDoubleOrNull() ?: run {
                    sender.sendMessage(plugin.langYml.getMessage("invalid-amount"))
                    return
                }

                for (player in players) {
                    val current = player.taskProgress(activeTask)
                    player.setTaskProgress(activeTask, current + amount)

                    sender.sendMessage(plugin.langYml.getMessage("given-task-progress")
                        .replace("%playername%", player.name)
                        .replace("%amount%", amount.toNiceString())
                        .replace("%task%", activeTask.parent.name)
                        .replace("%pass%", pass.name)
                    )

                    player.sendMessage(plugin.langYml.getMessage("received-task-progress")
                        .replace("%amount%", amount.toNiceString())
                        .replace("%task%", activeTask.parent.name)
                        .replace("%pass%", pass.name)
                    )
                }
            }

            else -> {
                sender.sendMessage(plugin.langYml.getMessage("invalid-type"))
                return
            }
        }
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return when (args.size) {
            1 -> StringUtil.copyPartialMatches(args[0], Bukkit.getOnlinePlayers().map { it.name } + "all", mutableListOf())
            2 -> StringUtil.copyPartialMatches(args[1], BattlePasses.values().map { it.id }, mutableListOf())
            3 -> StringUtil.copyPartialMatches(args[2], listOf("xp", "tier", "task-xp"), mutableListOf())
            4 -> when (args.getOrNull(2)?.lowercase()) {
                "task-xp", "taskxp" -> Categories.values().map { it.id }
                else -> listOf("1", "10", "100", "1000")
            }
            5 -> if (args.getOrNull(2)?.lowercase() in listOf("task-xp", "taskxp")) {
                val cat = Categories.getByID(args[3]) ?: return emptyList()
                cat.quests.map { it.parent.id }
            } else emptyList()
            6 -> if (args.getOrNull(2)?.lowercase() in listOf("task-xp", "taskxp")) {
                val cat = Categories.getByID(args[3]) ?: return emptyList()
                val quest = cat.quests.find { it.parent.id.equals(args[4], true) } ?: return emptyList()
                quest.tasks.map { it.parent.id }
            } else emptyList()
            7 -> if (args.getOrNull(2)?.lowercase() in listOf("task-xp", "taskxp"))
                listOf("1", "10", "100", "1000") else emptyList()
            else -> emptyList()
        }.let { StringUtil.copyPartialMatches(args.last(), it, mutableListOf()) }
    }
}