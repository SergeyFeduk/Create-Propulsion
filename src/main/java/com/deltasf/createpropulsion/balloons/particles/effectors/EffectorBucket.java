package com.deltasf.createpropulsion.balloons.particles.effectors;

import net.minecraft.core.BlockPos;

public class EffectorBucket {
    public static final int CAPACITY = 16; 
    public final HapEffector[] effectors = new HapEffector[CAPACITY];
    public int count = 0;

    public EffectorBucket() {}

    public void add(HapEffector e) {
        if (count < CAPACITY) {
            effectors[count++] = e;
        }
    }

    public void removeByBalloonId(int id) {
        for (int i = 0; i < count; i++) {
            if (effectors[i].getBalloonId() == id) {
                int last = count - 1;
                if (i != last) {
                    effectors[i] = effectors[last];
                }
                effectors[last] = null;
                count--;
                i--;
            }
        }
    }

    public void removeSpecific(int balloonId, BlockPos origin) {
        for (int i = 0; i < count; i++) {
            if (effectors[i].getBalloonId() == balloonId && effectors[i].isOrigin(origin)) {
                removeAt(i);
                i--;
            }
        }
    }

    private void removeAt(int i) {
        int last = count - 1;
        if (i != last) {
            effectors[i] = effectors[last];
        }
        effectors[last] = null;
        count--;
    }

    public boolean isEmpty() {
        return count == 0;
    }
}