package com.deltasf.createpropulsion.balloons;

import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class Balloon {
    //Scan-state
    public Set<BlockPos> volume;
    public AABB bounds; 
    public Set<UUID> supportHais;
    //Dynamic state
    public Set<BlockPos> holes;

    public Balloon(Set<BlockPos> volume, AABB bounds, Set<UUID> supportHais) {
        this.volume = volume;
        this.bounds = bounds;
        this.supportHais = supportHais;
    }
}
