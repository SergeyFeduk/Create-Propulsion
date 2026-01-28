package com.deltasf.createpropulsion.balloons.particles;

import com.deltasf.createpropulsion.balloons.particles.effectors.EffectorBucket;

//Data-Oriented ?!?!?
public class HapData {
    public final int capacity;
    public int count = 0;

    //Everything is in ship space
    //Position
    public final float[] x, y, z;
    public final float[] px, py, pz;
    //Velocity
    public final float[] vx, vy, vz;
    //Cached force
    public final float[] cfx, cfy, cfz;
    
    //State
    public final float[] life;
    public final float[] maxLife;
    public final byte[] state; //0 = Volume, 1 = Stream, 2 = Leak
    public final float[] scale;

    //Acceleration
    public final int[] balloonId;
    public final long[] lastBlockPosKey;
    public final EffectorBucket[] cachedBucket;

    public static final byte STATE_VOLUME = 0;
    public static final byte STATE_STREAM = 1;
    public static final byte STATE_LEAK = 2;

    public HapData(int capacity) {
        this.capacity = capacity;
        this.x = new float[capacity]; this.y = new float[capacity]; this.z = new float[capacity];
        this.px = new float[capacity]; this.py = new float[capacity]; this.pz = new float[capacity];
        this.vx = new float[capacity]; this.vy = new float[capacity]; this.vz = new float[capacity];
        this.cfx = new float[capacity]; this.cfy = new float[capacity]; this.cfz = new float[capacity];
        this.life = new float[capacity];
        this.maxLife = new float[capacity];
        this.state = new byte[capacity];
        this.scale = new float[capacity];
        this.balloonId = new int[capacity];
        this.lastBlockPosKey = new long[capacity];
        this.cachedBucket = new EffectorBucket[capacity];
    }
    
    public int spawn(float startX, float startY, float startZ, byte startState, int bId) {
        if (count >= capacity) return -1;
        int i = count++;
        x[i] = startX; y[i] = startY; z[i] = startZ;
        px[i] = startX; py[i] = startY; pz[i] = startZ;
        vx[i] = 0; vy[i] = 0; vz[i] = 0;
        cfx[i] = 0; cfy[i] = 0; cfz[i] = 0;
        life[i] = 1.0f;
        maxLife[i] = 1.0f;
        state[i] = startState;
        scale[i] = 1.0f; 
        balloonId[i] = bId;
        lastBlockPosKey[i] = Long.MIN_VALUE;
        cachedBucket[i] = null;
        return i;
    }
    
    public void remove(int index) {
        if (count <= 0) return;
        int last = count - 1;
        
        if (index != last) {
            x[index] = x[last]; y[index] = y[last]; z[index] = z[last];
            px[index] = px[last]; py[index] = py[last]; pz[index] = pz[last];
            vx[index] = vx[last]; vy[index] = vy[last]; vz[index] = vz[last];
            cfx[index] = cfx[last]; cfy[index] = cfy[last]; cfz[index] = cfz[last];
            life[index] = life[last];
            maxLife[index] = maxLife[last];
            state[index] = state[last];
            scale[index] = scale[last];
            balloonId[index] = balloonId[last];
            lastBlockPosKey[index] = lastBlockPosKey[last];
            cachedBucket[index] = cachedBucket[last];
        }
        cachedBucket[last] = null;

        count--;
    }
    
    public void clear() {
        for(int i = 0; i < count; i++) cachedBucket[i] = null;
        count = 0;
    }
}
