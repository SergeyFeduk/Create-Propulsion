package com.deltasf.createpropulsion.registries;

import com.simibubi.create.api.stress.BlockStressValues;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.createmod.catnip.config.ConfigBase;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleSupplier;

import javax.annotation.Nonnull;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class PropulsionDefaultStress extends ConfigBase {
    private static final int VERSION = 1;
    public static final PropulsionDefaultStress INSTANCE = new PropulsionDefaultStress();

    private static final Map<ResourceLocation, Double> DEFAULT_IMPACTS = new HashMap<>();
    private static final Map<ResourceLocation, Double> INTERNAL_IMPACTS = new HashMap<>();

    protected final Map<ResourceLocation, ForgeConfigSpec.ConfigValue<Double>> impacts = new HashMap<>();

    public static void init(ForgeConfigSpec spec) {
        INSTANCE.specification = spec;
        BlockStressValues.IMPACTS.registerProvider(INSTANCE::getImpact);
    }

    @Override
    public String getName() {
        return "stress-values.v" + VERSION;
    }

    @Override
    public void registerAll(@Nonnull ForgeConfigSpec.Builder builder) {
        builder.push("Stress impacts");

        //Internal impacts are not added into spec as they must not be modified!
        DEFAULT_IMPACTS.forEach((id, value) ->
            impacts.put(id, builder.define(id.getPath(), value))
        );

        builder.pop();
    }

    @Nullable
    public DoubleSupplier getImpact(Block block) {
        ResourceLocation id = CatnipServices.REGISTRIES.getKeyOrThrow(block);

        //Check config
        ForgeConfigSpec.ConfigValue<Double> configValue = this.impacts.get(id);
        if (configValue != null) return configValue::get;

        //Check internal
        Double internalValue = INTERNAL_IMPACTS.get(id);
        if (internalValue != null) return () -> internalValue;

        return null;
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {
        if (INSTANCE.specification == event.getConfig().getSpec())
            INSTANCE.onLoad();
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        if (INSTANCE.specification == event.getConfig().getSpec())
            INSTANCE.onReload();
    }

    public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> setImpact(double impact, boolean addToConfig) {
        return builder -> {
            ResourceLocation id = ResourceLocation.tryParse(builder.getOwner().getModid() + ":" + builder.getName());
            if (id != null) {
                if (addToConfig) {
                    DEFAULT_IMPACTS.put(id, impact);
                } else {
                    INTERNAL_IMPACTS.put(id, impact);
                }
            }
            return builder;
        };
    }
}