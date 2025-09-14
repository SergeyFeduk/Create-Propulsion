package com.deltasf.createpropulsion.balloons.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.joml.primitives.AABBic;

import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry.HaiData;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class RLEVolume {
    private short[] bottomYMap;
    private int sizeX;

    public boolean isInside(int worldX, int worldY, int worldZ, AABB groupAABB, AABBic shipAABB) {
        if (worldX < shipAABB.minX() - 1 || worldX > shipAABB.maxX() + 1 ||
            worldY > shipAABB.maxY() + 1 || // We deliberately DO NOT check worldY < shipAABB.minY() as bottomY is handled by our bottomYMap
            worldZ < shipAABB.minZ() - 1 || worldZ > shipAABB.maxZ() + 1) {
            return false;
        }
        int localX = worldX - (int) groupAABB.minX;
        int localZ = worldZ - (int) groupAABB.minZ;
        int index = localZ * sizeX + localX;
        if (index < 0 || index >= bottomYMap.length) {
            return false;
        }
    
        return worldY >= bottomYMap[index];
    }

    public void regenerate(List<HaiData> hais, AABB groupAABB) {
        if (hais.isEmpty() || groupAABB == null) {
            this.bottomYMap = null;
            return;
        }

        this.sizeX = (int) (groupAABB.maxX - groupAABB.minX) + 1;
        int sizeZ = (int) (groupAABB.maxZ - groupAABB.minZ) + 1;

        this.bottomYMap = new short[sizeX * sizeZ];
        Arrays.fill(this.bottomYMap, Short.MAX_VALUE);

        for (HaiData hai : hais) {
            AABB haiAABB = hai.aabb();
            short haiBottomY = (short) haiAABB.minY;

            int minX = (int) haiAABB.minX - (int) groupAABB.minX;
            int maxX = (int) haiAABB.maxX - (int) groupAABB.minX;
            int minZ = (int) haiAABB.minZ - (int) groupAABB.minZ;
            int maxZ = (int) haiAABB.maxZ - (int) groupAABB.minZ;

            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    int index = z * sizeX + x;
                    if (index >= 0 && index < bottomYMap.length) {
                        if (haiBottomY < bottomYMap[index]) {
                            bottomYMap[index] = haiBottomY;
                        }
                    }
                }
            }
        }
    }

}
