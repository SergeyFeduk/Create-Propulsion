package com.deltasf.createpropulsion.atmosphere;

import com.deltasf.createpropulsion.atmosphere.data.VarianceNoiseProperties;

public record AtmosphereData(double pressureAtSea, double scaleHeight, double gravity, int seaLevel, boolean isAirless, VarianceNoiseProperties varianceNoise) {}