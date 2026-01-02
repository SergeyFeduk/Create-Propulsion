package com.deltasf.createpropulsion.balloons.utils;

import java.util.Arrays;
import java.util.List;

import org.joml.primitives.AABBic;

import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry.HaiData;
import net.minecraft.world.phys.AABB;

public class RLEVolume {
    private static final int CHUNK_SHIFT = 5;
    private static final int CHUNK_SIZE = 1 << CHUNK_SHIFT;
    private static final int CHUNK_MASK = CHUNK_SIZE - 1;
    private static final int CHUNK_AREA = CHUNK_SIZE * CHUNK_SIZE;

    private short[][] chunkMap;
    private int chunkGridWidth;
    private int minChunkX;
    private int minChunkZ;

    public boolean isInside(int worldX, int worldY, int worldZ, AABB groupAABB, AABBic shipAABB) {
        if (worldX < shipAABB.minX() - 1 || worldX > shipAABB.maxX() + 1 ||
            worldY > shipAABB.maxY() + 1 || // We deliberately DO NOT check worldY < shipAABB.minY() as bottomY is handled by our bottomYMap
            worldZ < shipAABB.minZ() - 1 || worldZ > shipAABB.maxZ() + 1) {
            return false;
        }
        
        //Obtain chunk
        final int chunkX = (worldX >> CHUNK_SHIFT) - minChunkX;
        final int chunkZ = (worldZ >> CHUNK_SHIFT) - minChunkZ;
        final int chunkIndex = chunkZ * chunkGridWidth + chunkX;
        final short[] chunk = chunkMap[chunkIndex];
        if (chunk == null) return false;

        //Do the comparison
        final int innerX = worldX & CHUNK_MASK;
        final int innerZ = worldZ & CHUNK_MASK;
        final int innerIndex = innerZ * CHUNK_SIZE + innerX;
        return worldY >= chunk[innerIndex];
    }

    public void clear() {
        this.chunkMap = null;
        this.chunkGridWidth = 0;
    }
    
    public void regenerate(List<HaiData> hais, AABB groupAABB) {
        if (hais.isEmpty() || groupAABB == null) {
            this.chunkMap = null;
            return;
        }

        final int newMinChunkX = ((int) groupAABB.minX) >> CHUNK_SHIFT;
        final int newMaxChunkX = ((int) groupAABB.maxX) >> CHUNK_SHIFT;
        final int newMinChunkZ = ((int) groupAABB.minZ) >> CHUNK_SHIFT;
        final int newMaxChunkZ = ((int) groupAABB.maxZ) >> CHUNK_SHIFT;
        
        final int newChunkGridWidth = newMaxChunkX - newMinChunkX + 1;
        final int newChunkGridHeight = newMaxChunkZ - newMinChunkZ + 1;
        
        final short[][] newChunkMap = new short[newChunkGridWidth * newChunkGridHeight][];

        if (this.chunkMap != null && this.chunkGridWidth > 0) {
            final int oldChunkGridHeight = this.chunkMap.length / this.chunkGridWidth;
            for (int oldMapZ = 0; oldMapZ < oldChunkGridHeight; oldMapZ++) {
                for (int oldMapX = 0; oldMapX < this.chunkGridWidth; oldMapX++) {
                    short[] chunkToMove = this.chunkMap[oldMapZ * this.chunkGridWidth + oldMapX];
                    if (chunkToMove != null) {
                        final int globalChunkX = this.minChunkX + oldMapX;
                        final int globalChunkZ = this.minChunkZ + oldMapZ;
                        
                        if (globalChunkX >= newMinChunkX && globalChunkX <= newMaxChunkX &&
                            globalChunkZ >= newMinChunkZ && globalChunkZ <= newMaxChunkZ) {
                            
                            final int newMapX = globalChunkX - newMinChunkX;
                            final int newMapZ = globalChunkZ - newMinChunkZ;
                            final int newIndex = newMapZ * newChunkGridWidth + newMapX;
                            
                            newChunkMap[newIndex] = chunkToMove;
                            Arrays.fill(chunkToMove, Short.MAX_VALUE);
                        }
                    }
                }
            }
        }
        
        this.chunkMap = newChunkMap;
        this.chunkGridWidth = newChunkGridWidth;
        this.minChunkX = newMinChunkX;
        this.minChunkZ = newMinChunkZ;

        for (HaiData hai : hais) {
            final AABB haiAABB = hai.aabb();
            final short haiBottomY = (short) haiAABB.minY;

            final int minX = (int) haiAABB.minX;
            final int maxX = (int) haiAABB.maxX;
            final int minZ = (int) haiAABB.minZ;
            final int maxZ = (int) haiAABB.maxZ;

            final int haiMinChunkX = minX >> CHUNK_SHIFT;
            final int haiMaxChunkX = maxX >> CHUNK_SHIFT;
            final int haiMinChunkZ = minZ >> CHUNK_SHIFT;
            final int haiMaxChunkZ = maxZ >> CHUNK_SHIFT;
            
            for (int cz = haiMinChunkZ; cz <= haiMaxChunkZ; cz++) {
                for (int cx = haiMinChunkX; cx <= haiMaxChunkX; cx++) {
                    final int mapX = cx - this.minChunkX;
                    final int mapZ = cz - this.minChunkZ;

                    if (mapX < 0 || mapX >= this.chunkGridWidth || mapZ < 0 || mapZ >= newChunkGridHeight) {
                        continue;
                    }
                    
                    final int chunkIndex = mapZ * this.chunkGridWidth + mapX;
                    
                    short[] chunk = this.chunkMap[chunkIndex];
                    if (chunk == null) {
                        chunk = new short[CHUNK_AREA];
                        Arrays.fill(chunk, Short.MAX_VALUE);
                        this.chunkMap[chunkIndex] = chunk;
                    }

                    final int chunkWorldMinX = cx << CHUNK_SHIFT;
                    final int chunkWorldMinZ = cz << CHUNK_SHIFT;
                    final int startX = Math.max(minX, chunkWorldMinX);
                    final int endX = Math.min(maxX, chunkWorldMinX + CHUNK_MASK);
                    final int startZ = Math.max(minZ, chunkWorldMinZ);
                    final int endZ = Math.min(maxZ, chunkWorldMinZ + CHUNK_MASK);

                    for (int z = startZ; z <= endZ; z++) {
                        for (int x = startX; x <= endX; x++) {
                            final int innerIndex = (z & CHUNK_MASK) * CHUNK_SIZE + (x & CHUNK_MASK);
                            if (haiBottomY < chunk[innerIndex]) {
                                chunk[innerIndex] = haiBottomY;
                            }
                        }
                    }
                }
            }
        }
    }
}