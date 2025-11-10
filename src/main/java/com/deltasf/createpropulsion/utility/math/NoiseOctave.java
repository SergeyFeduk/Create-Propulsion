package com.deltasf.createpropulsion.utility.math;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record NoiseOctave(double scale, double magnitude) {
    public static final Codec<NoiseOctave> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.DOUBLE.fieldOf("scale").forGetter(NoiseOctave::scale),
            Codec.DOUBLE.fieldOf("magnitude").forGetter(NoiseOctave::magnitude)
        ).apply(instance, NoiseOctave::new)
    );
}