package com.deltasf.createpropulsion.registries;

import com.deltasf.createpropulsion.magnet.MagnetRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class PropulsionCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("propulsion")
            .then(Commands.literal("debug")
                .then(Commands.argument("value", BoolArgumentType.bool())
                    .executes(PropulsionCommands::setDebugMode)))
            .requires(source -> source.hasPermission(2))
        );
    }

    private static int setDebugMode(CommandContext<CommandSourceStack> context) {
        boolean value = BoolArgumentType.getBool(context, "value");
        MagnetRegistry.get().debug = value;
        return 1;
    }

    /*private static int clearMagnetRegistry(CommandContext<CommandSourceStack> context) {
        MagnetRegistry.get().reset();
        return 1;
    }*/
}
