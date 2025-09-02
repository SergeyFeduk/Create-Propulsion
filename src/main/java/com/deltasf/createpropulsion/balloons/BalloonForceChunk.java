package com.deltasf.createpropulsion.balloons;

public class BalloonForceChunk {
    public final int blockCount;
    public final float centroidX, centroidY, centroidZ;

    public BalloonForceChunk(int blockCount, float centroidX, float centroidY, float centroidZ) {
        this.blockCount = blockCount;
        this.centroidX = centroidX;
        this.centroidY = centroidY;
        this.centroidZ = centroidZ;
    }
}
