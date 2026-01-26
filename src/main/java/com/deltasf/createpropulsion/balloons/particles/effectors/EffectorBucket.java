package com.deltasf.createpropulsion.balloons.particles.effectors;

import net.minecraft.core.BlockPos;

public class EffectorBucket {
    public static final int CAPACITY = 16; 
    public final HapEffector[] effectors = new HapEffector[CAPACITY];
    public int count = 0;
    
    private int holeCount = 0;
    public boolean hasHole = false;

    public EffectorBucket() {}

    public void add(HapEffector e) {
        if (count < CAPACITY) {
            effectors[count++] = e;
            if (e instanceof HoleEffector) {
                holeCount++;
                hasHole = true;
            }
        }
    }

    public void remove(HapEffector e) {
        for (int i = 0; i < count; i++) {
            if (effectors[i] == e) {
                if (e instanceof HoleEffector) {
                    holeCount--;
                    hasHole = holeCount > 0;
                }
                
                int last = count - 1;
                if (i != last) {
                    effectors[i] = effectors[last];
                }
                effectors[last] = null;
                count--;
                return;
            }
        }
    }

    public void removeByBalloonId(int id) {
        for (int i = 0; i < count; i++) {
            if (effectors[i].getBalloonId() == id) {
                if (effectors[i] instanceof HoleEffector) {
                    holeCount--;
                }
                
                int last = count - 1;
                if (i != last) {
                    effectors[i] = effectors[last];
                }
                effectors[last] = null;
                count--;
                i--;
            }
        }
        hasHole = holeCount > 0;
    }

    public void removeSpecific(int balloonId, BlockPos origin) {
        for (int i = 0; i < count; i++) {
            if (effectors[i].getBalloonId() == balloonId && effectors[i].isOrigin(origin)) {
                if (effectors[i] instanceof HoleEffector) {
                    holeCount--;
                }
                
                removeAt(i);
                i--;
            }
        }
        hasHole = holeCount > 0;
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