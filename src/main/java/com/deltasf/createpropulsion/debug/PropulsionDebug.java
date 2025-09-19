package com.deltasf.createpropulsion.debug;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.deltasf.createpropulsion.debug.routes.MainDebugRoute;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class PropulsionDebug {
    private static final Map<IDebugRoute, Boolean> activeDebugStates = new ConcurrentHashMap<>();

    public static boolean isDebug(IDebugRoute route) {
        return activeDebugStates.getOrDefault(route, false);
    }

    //State registration for commands

    public static LiteralArgumentBuilder<CommandSourceStack> registerCommands() {
        LiteralArgumentBuilder<CommandSourceStack> debugCommand = Commands.literal("debug");
        buildDebugBranch(debugCommand, MainDebugRoute.values());

        return debugCommand;
    }

    //Updating debug state

    private static void buildDebugBranch(ArgumentBuilder<CommandSourceStack, ?> parent, IDebugRoute[] children) {
        for (final IDebugRoute route : children) {
            LiteralArgumentBuilder<CommandSourceStack> currentNode = Commands.literal(route.name().toLowerCase());

            if (route.getChildren().length > 0) {
                buildDebugBranch(currentNode, route.getChildren());
            } else {
                currentNode.then(Commands.argument("value", BoolArgumentType.bool())
                    .executes(context -> setDebugLeafState(context, route))
                );
            }
            parent.then(currentNode);
        }
    }

    private static int setDebugLeafState(CommandContext<CommandSourceStack> context, IDebugRoute leafNode) {
        boolean value = BoolArgumentType.getBool(context, "value");
        activeDebugStates.put(leafNode, value);
        return 1;
    }

    //Resolve static debug routes
    static {
        initializeLeafStates(MainDebugRoute.values());
    }

    private static void initializeLeafStates(IDebugRoute[] routes) {
        for (IDebugRoute route : routes) {
            if (route.getChildren().length == 0) {
                activeDebugStates.put(route, false);
            } else {
                initializeLeafStates(route.getChildren());
            }
        }
    }
}
