package org.valkyrienskies.mod.common.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.minecraft.commands.CommandRuntimeException
import net.minecraft.commands.CommandSourceStack
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.world.ServerShipWorld
import org.valkyrienskies.core.api.world.ShipWorld
import org.valkyrienskies.core.util.x
import org.valkyrienskies.core.util.y
import org.valkyrienskies.core.util.z
import org.valkyrienskies.mod.common.vsCore
import org.valkyrienskies.mod.mixinducks.feature.command.VSCommandSource
import org.valkyrienskies.mod.util.logger

object VSCommands {
    private val LOGGER by logger()

    private fun literal(name: String) =
        LiteralArgumentBuilder.literal<VSCommandSource>(name)

    private fun <T> argument(name: String, type: ArgumentType<T>) =
        RequiredArgumentBuilder.argument<VSCommandSource, T>(name, type)

    fun registerServerCommands(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher as CommandDispatcher<VSCommandSource>

        dispatcher.register(
            literal("vs")
                .then(literal("delete").then(argument("ships", ShipArgument.ships()).executes {
                    try {
                        val r = ShipArgument.getShips(it, "ships").toList() as List<ServerShip>
                        vsCore.deleteShips(it.source.shipWorld as ServerShipWorld, r)

                        r.size
                    } catch (e: Exception) {
                        if (e !is CommandRuntimeException) LOGGER.throwing(e)
                        throw e
                    }
                }))

                // Single ship commands
                .then(
                    literal("ship").then(
                        argument("ship", ShipArgument.ships())

                            // Delete a ship
                            .then(literal("delete").executes {

                                try {
                                    vsCore.deleteShips(
                                        it.source.shipWorld as ServerShipWorld,
                                        listOf(ShipArgument.getShip(it, "ship") as ServerShip)
                                    )
                                } catch (e: Exception) {
                                    if (e !is CommandRuntimeException) LOGGER.throwing(e)
                                    throw e
                                }

                                1
                            })

                            // Rename a ship
                            .then(
                                literal("rename")
                                    .then(argument("newName", StringArgumentType.string())
                                        .executes {
                                            vsCore.renameShip(
                                                ShipArgument.getShip(it, "ship") as ServerShip,
                                                StringArgumentType.getString(it, "newName")
                                            )

                                            1
                                        })
                            )

                            // Scale a ship
                            .then(
                                literal("scale")
                                    .then(argument("newScale", FloatArgumentType.floatArg(0.001f))
                                        .executes {
                                            try {
                                                vsCore.scaleShip(
                                                    ShipArgument.getShip(it, "ship") as ServerShip,
                                                    FloatArgumentType.getFloat(it, "newScale")
                                                )
                                            } catch (e: Exception) {
                                                if (e !is CommandRuntimeException) LOGGER.throwing(e)
                                                throw e
                                            }

                                            1
                                        })
                            )
                    )
                )
        )

        dispatcher.root.children.first { it.name == "teleport" }.addChild(
            argument("ship", ShipArgument.selectorOnly()).executes {
                val ship = ShipArgument.getShip(it, "ship")
                val source = it.source as CommandSourceStack
                val shipPos = ship.transform.positionInWorld

                source.entity?.let { it.teleportTo(shipPos.x, shipPos.y, shipPos.z); 1 } ?: 0
            }.build()
        )
    }

    fun registerClientCommands(dispatcher: CommandDispatcher<CommandSourceStack>) {
        // TODO implement client commands
    }
}

val CommandSourceStack.shipWorld: ShipWorld
    get() = (this as VSCommandSource).shipWorld
