package com.deltasf.createpropulsion.balloons;

public class BalloonForceChunk {
    public final int blockCount;
    public final int sumX, sumY, sumZ; // sums relative to chunk center

    public BalloonForceChunk(int blockCount, int sumX, int sumY, int sumZ) {
        this.blockCount = blockCount;
        this.sumX = sumX;
        this.sumY = sumY;
        this.sumZ = sumZ;
    }

    public double getCentroidRelX() { return ((double) sumX) / blockCount; }
    public double getCentroidRelY() { return ((double) sumY) / blockCount; }
    public double getCentroidRelZ() { return ((double) sumZ) / blockCount; }
}
