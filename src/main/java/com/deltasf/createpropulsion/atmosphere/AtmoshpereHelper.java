package com.deltasf.createpropulsion.atmosphere;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.atmosphere.data.VarianceNoiseProperties;
import com.deltasf.createpropulsion.utility.math.PerlinNoise;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;

public class AtmoshpereHelper {
    private static final double NOISE_SEED = 196418;

    public static int determineSeaLevel(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            ChunkGenerator generator = serverLevel.getChunkSource().getGenerator();
            if (generator instanceof FlatLevelSource) {
                return -60;
            }
        }

        return level.getSeaLevel();
    }

    public static double calculateExternalAirDensity(AtmosphereData atmosphere, double worldY, boolean clampAtSea) {
        if (atmosphere.isAirless() || atmosphere.pressureAtSea() <= 0) return 0.0;

        double altitude = worldY - atmosphere.seaLevel();
        if (clampAtSea) {
            altitude = Math.max(0, altitude);
        }
        
        double height = PropulsionConfig.ATMOSPHERE_HEIGHT_FACTOR.get() * atmosphere.scaleHeight();
        return atmosphere.pressureAtSea() * Math.exp(-altitude / height);
    }

    public static double calculateVariableExternalAirDensity(AtmosphereData atmosphere, double worldX, double worldY, double worldZ, long gameTime, boolean clampAtSea) {
        double baseDensity = calculateExternalAirDensity(atmosphere, worldY, clampAtSea);
        if (baseDensity <= 0) return 0.0;

        VarianceNoiseProperties noiseProperties = atmosphere.varianceNoise();
        if (noiseProperties == null || noiseProperties.octaves().isEmpty()) return baseDensity;

        double time = gameTime * PropulsionConfig.ATMOSPHERE_NOISE_TIME_FACTOR.get();
        double effectiveX = worldX + time * noiseProperties.driftSpeedX();
        double effectiveZ = worldZ + time * noiseProperties.driftSpeedZ();
        double effectiveT = time * noiseProperties.evolutionSpeed();

        double totalNoise = PerlinNoise.get(effectiveX, effectiveT, effectiveZ, NOISE_SEED, noiseProperties.octaves());

        double modifier = 1.0 + totalNoise * PropulsionConfig.ATMOSPHERE_NOISE_MAGNITUDE.get();
        modifier = Math.max(0.1, modifier);
        return baseDensity * modifier;
    }
}
