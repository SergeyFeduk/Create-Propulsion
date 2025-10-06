package com.deltasf.createpropulsion.registries;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.deltasf.createpropulsion.debug.PropulsionDebug;
import com.deltasf.createpropulsion.magnet.MagnetRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class PropulsionCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> propulsionCommand = Commands.literal("propulsion")
            .requires(source -> source.hasPermission(2));
        //All debug commands
        LiteralArgumentBuilder<CommandSourceStack> debugNode = PropulsionDebug.registerCommands();
        propulsionCommand.then(debugNode);

        //Magnet registry cleanup command
        propulsionCommand
            .then(Commands.literal("clearMagnetRegistry")
            .executes(PropulsionCommands::clearMagnetRegistry));

        propulsionCommand
            .then(Commands.literal("fill-balloons")
            .executes(PropulsionCommands::fillBalloons));

        dispatcher.register(propulsionCommand);
    }

    private static int clearMagnetRegistry(CommandContext<CommandSourceStack> context) {
        MagnetRegistry.get().reset();
        return 1;
    }

    private static int fillBalloons(CommandContext<CommandSourceStack> context) {
        for(BalloonRegistry registry : BalloonShipRegistry.get().getRegistries()) {
            for (Balloon balloon : registry.getBalloons()) {
                balloon.hotAir = balloon.getVolumeSize();
            }
        }
        return 1;
    }
}
