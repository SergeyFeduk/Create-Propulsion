package com.deltasf.createpropulsion.registries;

import com.simibubi.create.api.stress.BlockStressValues;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;

public class PropulsionDefaultStress {
    public static final Map<ResourceLocation, Double> DEFAULT_IMPACTS = new HashMap<>();

    public static void register() {
        //TODO: make stress values configurable
        BlockStressValues.IMPACTS.registerProvider(PropulsionDefaultStress::getImpact);
    }

    public static DoubleSupplier getImpact(Block block) {
        ResourceLocation id = CatnipServices.REGISTRIES.getKeyOrThrow(block);
        Double value = DEFAULT_IMPACTS.getOrDefault(id, 0.0);
        if (value == null) return null;
        return () -> value;
    }

    public static void setDefaultImpact(ResourceLocation blockId, double impact) {
        DEFAULT_IMPACTS.put(blockId, impact);
    }

    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> setImpact(double impact) {
        return b -> {
            setDefaultImpact(ResourceLocation.fromNamespaceAndPath(b.getOwner().getModid(), b.getName()), impact);
            return b;
        };
    }

    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> setNoImpact() {
        return setImpact(0.0);
    }
}
