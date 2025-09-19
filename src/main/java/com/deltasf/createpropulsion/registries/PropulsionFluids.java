package com.deltasf.createpropulsion.registries;

import java.util.function.Supplier;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.FluidBuilder.FluidTypeFactory;
import com.tterrag.registrate.util.entry.FluidEntry;

import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fml.DistExecutor;

public class PropulsionFluids {
    public static final CreateRegistrate REGISTRATE = CreatePropulsion.registrate();
    public static void register() {} //Loads this class

    private static final Supplier<FluidTypeFactory> TURPENTINE_TYPE_FACTORY = DistExecutor.unsafeRunForDist(
        () -> PropulsionFluidsClient::getTurpentineTypeFactory,
        () -> PropulsionFluids::createGenericFactory
    );

    public static final FluidEntry<ForgeFlowingFluid.Flowing> TURPENTINE = REGISTRATE.standardFluid("turpentine", TURPENTINE_TYPE_FACTORY.get())
        .renderType(getSidedRenderType())
        .lang("Turpentine")
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
