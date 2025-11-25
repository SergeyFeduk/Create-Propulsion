package com.deltasf.createpropulsion.atmosphere;

import com.deltasf.createpropulsion.atmosphere.data.VarianceNoiseProperties;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY
)
public record AtmosphereData(double pressureAtSea, double scaleHeight, double gravity, int seaLevel, boolean isAirless, VarianceNoiseProperties varianceNoise) {}
