package com.deltasf.createpropulsion.balloons.network;

import net.minecraft.network.FriendlyByteBuf;

public abstract class BalloonPacket {
    public final long shipId;
    public final int balloonId;

    public BalloonPacket(long shipId, int balloonId) {
        this.shipId = shipId;
        this.balloonId = balloonId;
    }
    
    protected void writeHeader(FriendlyByteBuf buffer) {
        buffer.writeLong(shipId);
        buffer.writeInt(balloonId);
    }
}
