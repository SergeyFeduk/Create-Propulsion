package com.deltasf.createpropulsion.magnet;

import org.joml.Vector3i;

import net.minecraft.core.BlockPos;

public final class MagnetPair {
    public final BlockPos localPos;
    public final Vector3i localDir;
    public final long otherShipId;
    public final BlockPos otherPos;
    public final Vector3i otherDir;

    public MagnetPair(BlockPos APos, Vector3i ADir, long BShipId, BlockPos BPos, Vector3i BDir) {
        this.localPos = APos;
        this.localDir = ADir;
        this.otherShipId = BShipId;
        this.otherPos = BPos;
        this.otherDir = BDir;
    }
}
