package com.deltasf.createpropulsion.registries;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.FluidBuilder.FluidTypeFactory;
import com.tterrag.registrate.util.entry.FluidEntry;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fml.DistExecutor;

import com.simibubi.create.foundation.utility.Color;

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
