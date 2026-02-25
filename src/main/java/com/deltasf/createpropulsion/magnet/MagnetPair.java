package com.deltasf.createpropulsion.magnet;

import org.joml.Vector3i;
import net.minecraft.core.BlockPos;
import java.util.UUID;

public final class MagnetPair {
    public final UUID localId;
    public final BlockPos localPos;
    public final Vector3i localDir;
    public final int localPower;

    public final UUID otherId;
    public final long otherShipId;
    public final BlockPos otherPos;
    public final Vector3i otherDir;
    public final int otherPower;

    public MagnetPair(UUID localId, BlockPos APos, Vector3i ADir, int APower, 
                      UUID otherId, long BShipId, BlockPos BPos, Vector3i BDir, int BPower) {
        this.localId = localId;
        this.localPos = APos;
        this.localDir = ADir;
        this.localPower = APower;

        this.otherId = otherId;
        this.otherShipId = BShipId;
        this.otherPos = BPos;
        this.otherDir = BDir;
        this.otherPower = BPower;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MagnetPair that = (MagnetPair) o;
        return localId.equals(that.localId) && otherId.equals(that.otherId);
    }

    @Override
    public int hashCode() {
        //One must imagine a different prime used ever (I had a stroke)
        return 31 * localId.hashCode() + otherId.hashCode();
    }
}
