package me.odinmain.features.impl.nether

import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.features.settings.impl.BooleanSetting
import me.odinmain.utils.skyblock.*
import me.odinmain.utils.skyblock.KuudraUtils.PreSpot
import me.odinmain.utils.skyblock.KuudraUtils.giantZombies
import net.minecraft.util.Vec3

object NoPre : Module(
    name = "Pre-Spot Alert",
    description = "Alerts the party about the state of a pre spot.",
    category = Category.NETHER
) {
    private val showAlert by BooleanSetting("Show Alert", false, description = "Shows an alert when you miss a pre spot.")

    private var preLocation = PreSpot.None
    var missing = PreSpot.None

    private val partyChatRegex = Regex("^Party > (\\[[^]]*?])? ?(\\w{1,16}): No ?(Triangle|X|Equals|Slash)!\$")

    init {
        onMessage("[NPC] Elle: Head over to the main platform, I will join you when I get a bite!", false) {
            val playerLocation = mc.thePlayer?.positionVector ?: return@onMessage
            preLocation = when {
                PreSpot.Triangle.location.distanceTo(playerLocation) < 15 -> PreSpot.Triangle
                PreSpot.X.location.distanceTo(playerLocation) < 30 -> PreSpot.X
                PreSpot.Equals.location.distanceTo(playerLocation) < 15 -> PreSpot.Equals
                PreSpot.Slash.location.distanceTo(playerLocation) < 15 -> PreSpot.Slash
                else -> PreSpot.None
            }
            modMessage("Pre-spot: ${if (preLocation == PreSpot.None) "§cDidn't register your pre-spot because you didn't get there in time." else preLocation.name}")
        }

        onMessage("[NPC] Elle: Not again!", false) {
            var pre = false
            var second = false
            var msg = ""
            giantZombies.forEach { supply ->
                val supplyLoc = Vec3(supply.posX, 76.0, supply.posZ)
                when {
                    preLocation.location.distanceTo(supplyLoc) < 18 -> pre = true
                    preLocation == PreSpot.Triangle && PreSpot.Shop.location.distanceTo(supplyLoc) < 18 -> second = true
                    preLocation == PreSpot.X && PreSpot.xCannon.location.distanceTo(supplyLoc) < 16 -> second = true
                    preLocation == PreSpot.Slash && PreSpot.Square.location.distanceTo(supplyLoc) < 20 -> second = true
                }
            }
            if (second && pre) return@onMessage
            if (!pre && preLocation != PreSpot.None) msg = "No ${preLocation.name}!"
            else if (!second) {
                msg = when (preLocation) {
                    PreSpot.Triangle -> "No Shop!"
                    PreSpot.X -> "No X Cannon!"
                    PreSpot.Slash -> "No Square!"
                    else -> return@onMessage
                }
            }
            if (msg.isEmpty()) return@onMessage modMessage("§cYou didn't get to your pre spot in time")
            partyMessage(msg)
            if (showAlert) PlayerUtils.alert(msg, time = 10)
        }

        onMessage(partyChatRegex) {
            missing = PreSpot.valueOf(partyChatRegex.find(it)?.groupValues?.lastOrNull() ?: return@onMessage)
        }

        onWorldLoad { missing = PreSpot.None }
    }
}