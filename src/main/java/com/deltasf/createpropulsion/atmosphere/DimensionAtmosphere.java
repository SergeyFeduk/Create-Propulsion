package com.deltasf.createpropulsion.atmosphere;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record DimensionAtmosphere(
    Optional<Double> scaleHeight,
    Optional<Double> gravity,
    Optional<Double> pressureAtSeaLevel,
    Optional<String> requiredMod,
    Optional<Boolean> isAirless
) {
    public static final Codec<DimensionAtmosphere> CODEC = RecordCodecBuilder.create(instance -> 
        instance.group(
            Codec.DOUBLE.optionalFieldOf("height").forGetter(DimensionAtmosphere::scaleHeight),
            Codec.DOUBLE.optionalFieldOf("gravity").forGetter(DimensionAtmosphere::gravity),
            Codec.DOUBLE.optionalFieldOf("sea_level_pressure").forGetter(DimensionAtmosphere::pressureAtSeaLevel),
            Codec.STRING.optionalFieldOf("required_mod").forGetter(DimensionAtmosphere::requiredMod),
            Codec.BOOL.optionalFieldOf("is_airless").forGetter(DimensionAtmosphere::isAirless)
        ).apply(instance, DimensionAtmosphere::new));
}