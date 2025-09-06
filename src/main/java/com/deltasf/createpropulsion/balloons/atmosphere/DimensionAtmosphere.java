package com.deltasf.createpropulsion.balloons.atmosphere;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record DimensionAtmosphere(
    double scaleHeight,
    Optional<Double> gravity,
    Optional<Double> pressureAtSeaLevel,
    Optional<String> requiredMod
) {
    public static final Codec<DimensionAtmosphere> CODEC = RecordCodecBuilder.create(instance -> 
        instance.group(
            Codec.DOUBLE.fieldOf("height").forGetter(DimensionAtmosphere::scaleHeight),
            Codec.DOUBLE.optionalFieldOf("gravity").forGetter(DimensionAtmosphere::gravity),
            Codec.DOUBLE.optionalFieldOf("sea_level_pressure").forGetter(DimensionAtmosphere::pressureAtSeaLevel),
            Codec.STRING.optionalFieldOf("required_mod").forGetter(DimensionAtmosphere::requiredMod)
        ).apply(instance, DimensionAtmosphere::new));
}