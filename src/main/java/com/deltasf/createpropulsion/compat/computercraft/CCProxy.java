package com.deltasf.createpropulsion.compat.computercraft;

import java.util.function.Function;

import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.FallbackComputerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

public class CCProxy {
    public static void register() {
        secondaryFactory = FallbackComputerBehaviour::new;
        Mods.COMPUTERCRAFT.executeIfInstalled(() -> CCProxy::registerWithDependency);
    }

    private static void registerWithDependency() {
        primaryFactory = ComputerBehaviour::new;
    }

    private static Function<SmartBlockEntity, ? extends AbstractComputerBehaviour> secondaryFactory;
    private static Function<SmartBlockEntity, ? extends AbstractComputerBehaviour> primaryFactory;

    public static AbstractComputerBehaviour behaviour(SmartBlockEntity blockEntity) {
        if (primaryFactory == null) {
            secondaryFactory = FallbackComputerBehaviour::new;
            return secondaryFactory.apply(blockEntity);
        }
        return primaryFactory.apply(blockEntity);
    }
}
