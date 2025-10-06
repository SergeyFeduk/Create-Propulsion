package com.deltasf.createpropulsion.atmosphere;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;

public class AtmoshpereHelper {
    public static int determineSeaLevel(ServerLevel serverLevel) {
        ChunkGenerator generator = serverLevel.getChunkSource().getGenerator();
        if (generator instanceof FlatLevelSource) {
            return -60;
        }

        return serverLevel.getSeaLevel();
    }

    public static double calculateExternalAirDensity(AtmosphereData atmosphere, double worldY, boolean clampAtSea) {
        double altitude = worldY - atmosphere.seaLevel();
        if (clampAtSea) {
            altitude = Math.max(0, altitude);
        }
        return atmosphere.pressureAtSea() * Math.exp(-altitude / atmosphere.scaleHeight());
    }
}
