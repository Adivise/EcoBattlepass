package com.exanthiax.xbattlepass.commands

import com.willfp.eco.core.command.impl.PluginCommand
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil
import com.exanthiax.xbattlepass.battlepass.BattlePasses
import com.exanthiax.xbattlepass.categories.Categories
import com.exanthiax.xbattlepass.plugin

object ResetCommand : PluginCommand(
    plugin,
    "reset",
    "xbattlepass.command.reset",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        val typeArg = args.getOrNull(0)?.lowercase() ?: run {
            sender.sendMessage(plugin.langYml.getMessage("type-required"))
            return
        }

        when (typeArg) {
            "battlepass", "pass" -> {
                val playerArg = args.getOrNull(1) ?: run {
                    sender.sendMessage(plugin.langYml.getMessage("player-required"))
                    return
                }

                val passArg = args.getOrNull(2) ?: run {
                    sender.sendMessage(plugin.langYml.getMessage("pass-required"))
                    return
                }

                val pass = BattlePasses.getByID(passArg) ?: run {
                    sender.sendMessage(plugin.langYml.getMessage("pass-not-found"))
                    return
                }

                val isAll = playerArg.equals("all", ignoreCase = true)
                val players = if (isAll) {
                    Bukkit.getOnlinePlayers().toList()
                } else {
                    val p = Bukkit.getPlayer(playerArg) ?: run {
                        sender.sendMessage(plugin.langYml.getMessage("player-not-found"))
                        return
                    }
                    listOf(p)
                }

                for (player in players) {
                    pass.reset(player)
                }

                sender.sendMessage(
                    plugin.langYml.getMessage("reset-player")
                        .replace("%playername%", if (isAll) "all players" else players.first().name)
                        .replace("%pass%", pass.name)
                )
            }

            "task" -> {
                if (args.size < 6) {
                    sender.sendMessage(plugin.langYml.getMessage("task-required"))
                    return
                }

                val playerArg = args[1]
                val passArg = args[2]
                val categoryId = args[3]
                val questId = args[4]
                val taskId = args[5]

                val pass = BattlePasses.getByID(passArg) ?: run {
                    sender.sendMessage(plugin.langYml.getMessage("pass-not-found"))
                    return
                }

                val category = Categories.getByID(categoryId) ?: run {
                    sender.sendMessage(plugin.langYml.getMessage("invalid-category"))
                    return
                }

                if (category.battlepass != pass) {
                    sender.sendMessage(plugin.langYml.getMessage("invalid-category"))
                    return
                }

                val activeQuest = category.quests.find { it.parent.id.equals(questId, true) } ?: run {
                    sender.sendMessage(plugin.langYml.getMessage("invalid-quest"))
                    return
                }

                val activeTask = activeQuest.tasks.find { it.parent.id.equals(taskId, true) } ?: run {
                    sender.sendMessage(plugin.langYml.getMessage("invalid-task"))
                    return
                }

                val isAll = playerArg.equals("all", ignoreCase = true)
                val players = if (isAll) {
                    Bukkit.getOnlinePlayers().toList()
                } else {
                    val p = Bukkit.getPlayer(playerArg) ?: run {
                        sender.sendMessage(plugin.langYml.getMessage("player-not-found"))
                        return
                    }
                    listOf(p)
                }

                for (player in players) {
                    activeTask.reset(player)
                }

                val taskName = activeTask.parent.name
                sender.sendMessage(
                    plugin.langYml.getMessage("reset-task")
                        .replace("%playername%", if (isAll) "all players" else players.first().name)
                        .replace("%task%", taskName)
                        .replace("%pass%", pass.name)
                )
            }

            else -> {
                sender.sendMessage(plugin.langYml.getMessage("invalid-type"))
                return
            }
        }
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        return when (args.size) {
            1 -> StringUtil.copyPartialMatches(args[0], listOf("battlepass", "task"), mutableListOf())
            2 -> when (args[0].lowercase()) {
                "battlepass", "pass" -> StringUtil.copyPartialMatches(
                    args[1],
                    listOf("all") + Bukkit.getOnlinePlayers().map { it.name },
                    mutableListOf()
                )
                "task" -> StringUtil.copyPartialMatches(
                    args[1],
                    listOf("all") + Bukkit.getOnlinePlayers().map { it.name },
                    mutableListOf()
                )
                else -> emptyList()
            }
            3 -> when (args[0].lowercase()) {
                "battlepass", "pass", "task" -> StringUtil.copyPartialMatches(
                    args[2],
                    BattlePasses.values().map { it.id },
                    mutableListOf()
                )
                else -> emptyList()
            }
            4 -> if (args[0].lowercase() == "task") Categories.values().map { it.id } else emptyList()
            5 -> if (args[0].lowercase() == "task") {
                val cat = Categories.getByID(args[3]) ?: return emptyList()
                cat.quests.map { it.parent.id }
            } else emptyList()
            6 -> if (args[0].lowercase() == "task") {
                val cat = Categories.getByID(args[3]) ?: return emptyList()
                val quest = cat.quests.find { it.parent.id.equals(args[4], true) } ?: return emptyList()
                quest.tasks.map { it.parent.id }
            } else emptyList()
            else -> emptyList()
        }.let { StringUtil.copyPartialMatches(args.last(), it, mutableListOf()) }
    }
}