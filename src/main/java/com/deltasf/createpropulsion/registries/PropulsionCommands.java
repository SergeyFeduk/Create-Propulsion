package com.deltasf.createpropulsion.registries;

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

        dispatcher.register(propulsionCommand);
    }

    private static int clearMagnetRegistry(CommandContext<CommandSourceStack> context) {
        MagnetRegistry.get().reset();
        return 1;
    }
}
