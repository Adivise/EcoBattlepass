package com.exanthiax.xbattlepass.utils

import com.exanthiax.xbattlepass.plugin
import com.willfp.eco.util.SoundUtils as EcoSoundUtils
import org.bukkit.entity.Player

object SoundUtils {
    fun playIfEnabled(player: Player, configPath: String) {
        val sound = EcoSoundUtils.getSound(plugin.configYml.getFormattedString("$configPath.sound")) ?: return
        val pitch = plugin.configYml.getDoubleFromExpression("$configPath.pitch")
        val volume = plugin.configYml.getDoubleFromExpression("$configPath.volume")

        if (plugin.configYml.getBool("$configPath.enabled")) {
            player.playSound(
                player.location,
                sound,
                volume.toFloat(),
                pitch.toFloat()
            )
        }
    }
}