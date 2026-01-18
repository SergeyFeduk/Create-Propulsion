package com.deltasf.createpropulsion.registries;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.deltasf.createpropulsion.debug.PropulsionDebug;
import com.deltasf.createpropulsion.magnet.MagnetRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class PropulsionCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> propulsionCommand = Commands.literal("propulsion")
            .requires(source -> source.hasPermission(2));
        //All debug commands
        LiteralArgumentBuilder<CommandSourceStack> debugNode = PropulsionDebug.registerCommands();
        propulsionCommand.then(debugNode);

        Set<String> floatKeys = PropulsionDebug.getFloatKeys();
        if (floatKeys.size() > 0) {
            LiteralArgumentBuilder<CommandSourceStack> varNode = Commands.literal("var");
            for (String key : floatKeys) {
                varNode.then(Commands.literal(key)
                    .then(Commands.argument("val", FloatArgumentType.floatArg())
                    .executes(ctx -> setVar(key, FloatArgumentType.getFloat(ctx, "val")))));
            }
            propulsionCommand.then(varNode);
        }

        //Magnet registry cleanup command
        propulsionCommand
            .then(Commands.literal("clearMagnetRegistry")
            .executes(PropulsionCommands::clearMagnetRegistry));

        propulsionCommand
            .then(Commands.literal("fill-balloons")
            .executes(PropulsionCommands::fillBalloons));

        propulsionCommand
            .then(Commands.literal("kill-orphans")
            .executes(PropulsionCommands::killOrphans));

        dispatcher.register(propulsionCommand);
    }

    private static int setVar(String key, float val) {
        PropulsionDebug.registerFloat(key, val);
        return 1;
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

    private static int killOrphans(CommandContext<CommandSourceStack> context) {
        int deadOrphans = 0;

        for(BalloonRegistry registry : BalloonShipRegistry.get().getRegistries()) {
            for(HaiGroup group : registry.getHaiGroups()) {
                final List<Balloon> balloonsToKill = new ArrayList<>();
                for(Balloon balloon : group.balloons) {
                    if (balloon.isSupportHaisEmpty()) { 
                        balloonsToKill.add(balloon);
                        deadOrphans++;
                    }
                }
                synchronized(group.balloons) {
                    for(Balloon balloon : balloonsToKill) {
                        group.killBalloon(balloon, registry);
                    }
                }
            }
        }
        if (deadOrphans > 0) {
            String orphanCount = String.valueOf(deadOrphans);
            String postfix = deadOrphans <= 1 ? "!" : "s!";
            context.getSource().sendSuccess(() -> Component.literal("Killed " + orphanCount + " orphan" + postfix), false);
        } else {
            context.getSource().sendFailure(Component.literal("No orphans to kill :("));
        }
        return 1;
    }
}
