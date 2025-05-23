package com.deltasf.createpropulsion.thruster;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;

public record ThrusterFuelDefinition (
    ResourceLocation fluidId,
    float thrustMultiplier,
    float consumptionMultiplier,
    Optional<String> requiredMod
) {
    public static final Codec<ThrusterFuelDefinition> CODEC = RecordCodecBuilder.create(instance -> 
        instance.group(
            ResourceLocation.CODEC.fieldOf("fluid").forGetter(ThrusterFuelDefinition::fluidId),
            Codec.FLOAT.fieldOf("thrust_multiplier").forGetter(ThrusterFuelDefinition::thrustMultiplier),
            Codec.FLOAT.fieldOf("consumption_multiplier").forGetter(ThrusterFuelDefinition::consumptionMultiplier),
            Codec.STRING.optionalFieldOf("required_mod").forGetter(ThrusterFuelDefinition::requiredMod)
        ).apply(instance, ThrusterFuelDefinition::new));

    
    public Fluid getFluid() {
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(this.fluidId);
        return fluid == null ? Fluids.EMPTY : fluid;
    }
}
