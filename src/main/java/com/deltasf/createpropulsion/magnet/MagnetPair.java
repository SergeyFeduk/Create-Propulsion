package com.deltasf.createpropulsion.magnet;

import org.joml.Vector3i;

import net.minecraft.core.BlockPos;

public final class MagnetPair {
    public final BlockPos localPos;
    public final Vector3i localDir;
    public final int localPower;

    public final long otherShipId;
    public final BlockPos otherPos;
    public final Vector3i otherDir;
    public final int otherPower;

    public MagnetPair(BlockPos APos, Vector3i ADir, int APower, long BShipId, BlockPos BPos, Vector3i BDir, int BPower) {
        this.localPos = APos;
        this.localDir = ADir;
        this.localPower = APower;

        this.otherShipId = BShipId;
        this.otherPos = BPos;
        this.otherDir = BDir;
        this.otherPower = BPower;
    }
}
