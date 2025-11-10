package com.deltasf.createpropulsion.atmosphere.data;

import java.util.List;

import com.deltasf.createpropulsion.utility.math.NoiseOctave;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record VarianceNoiseProperties(
    double driftSpeedX,
    double driftSpeedZ,
    double evolutionSpeed,
    List<NoiseOctave> octaves
) {
    public static final Codec<VarianceNoiseProperties> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.DOUBLE.fieldOf("drift_speed_x").forGetter(VarianceNoiseProperties::driftSpeedX),
            Codec.DOUBLE.fieldOf("drift_speed_z").forGetter(VarianceNoiseProperties::driftSpeedZ),
            Codec.DOUBLE.fieldOf("evolution_speed").forGetter(VarianceNoiseProperties::evolutionSpeed),
            Codec.list(NoiseOctave.CODEC).fieldOf("octaves").forGetter(VarianceNoiseProperties::octaves)
        ).apply(instance, VarianceNoiseProperties::new));
}