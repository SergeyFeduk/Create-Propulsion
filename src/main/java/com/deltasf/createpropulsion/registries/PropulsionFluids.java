package com.deltasf.createpropulsion.registries;

import java.util.function.Supplier;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.FluidBuilder.FluidTypeFactory;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateLangProvider;
import com.tterrag.registrate.util.entry.FluidEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fml.DistExecutor;

public class PropulsionFluids {
    public static final CreateRegistrate REGISTRATE = CreatePropulsion.registrate();
    public static void register() {} //Loads this class

    private static <R, T extends R> NonNullBiConsumer<DataGenContext<R, T>, RegistrateLangProvider> FUCK_OFF_LANG() {
        return (ctx, prov) -> {};
    }

    private static final Supplier<FluidTypeFactory> TURPENTINE_TYPE_FACTORY = DistExecutor.unsafeRunForDist(
        () -> PropulsionFluidsClient::getTurpentineTypeFactory,
        () -> PropulsionFluids::createGenericFactory
    );

    public static final FluidEntry<ForgeFlowingFluid.Flowing> TURPENTINE = REGISTRATE.fluid("turpentine", 
            new ResourceLocation("minecraft", "block/water_still"), 
            new ResourceLocation("minecraft", "block/water_flow"),
            TURPENTINE_TYPE_FACTORY.get())
        .renderType(getSidedRenderType())
        .source(ForgeFlowingFluid.Source::new) 
        .setData(ProviderType.LANG, FUCK_OFF_LANG())
        .block().setData(ProviderType.LANG, FUCK_OFF_LANG()).build()
        .bucket().setData(ProviderType.LANG, FUCK_OFF_LANG()).build()
        .properties(p -> p.viscosity(1000).density(500))
        .fluidProperties(p -> p.levelDecreasePerBlock(1)
            .tickRate(7)
            .slopeFindDistance(3)
            .explosionResistance(100f))
        .register();

    //Helpers
    
    private static Supplier<RenderType> getSidedRenderType() {
        return DistExecutor.unsafeRunForDist(
                () -> PropulsionFluidsClient::getTurpentineRenderType,
                () -> () -> null
        );
    }
    
    private static Supplier<FluidTypeFactory> createGenericFactory() {
        return () -> (properties, stillTexture, flowingTexture) -> new FluidType(properties);
    }
}
