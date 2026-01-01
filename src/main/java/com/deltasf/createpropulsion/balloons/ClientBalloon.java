package com.deltasf.createpropulsion.balloons;

import java.util.HashSet;
import java.util.Set;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public class ClientBalloon {
    public final int id;
    public final LongOpenHashSet volume = new LongOpenHashSet();
    public final Set<BlockPos> holes = new HashSet<>();
    
    private AABB cachedBounds = null;

    public ClientBalloon(int id) {
        this.id = id;
    }

    public AABB getBounds() {
        if (cachedBounds == null) recalculateBounds();
        return cachedBounds;
    }

    public void applyDelta(long[] added, long[] removed, long[] addedHoles, long[] removedHoles) {
        // Volume
        for (long p : removed) volume.remove(p);
        for (long p : added) volume.add(p);
        
        // Holes
        for (long p : removedHoles) holes.remove(BlockPos.of(p));
        for (long p : addedHoles) holes.add(BlockPos.of(p));

        recalculateBounds();
    }
    
    public void setContent(long[] newVolume, long[] newHoles) {
        volume.clear();
        for(long p : newVolume) volume.add(p);
        
        holes.clear();
        for(long p : newHoles) holes.add(BlockPos.of(p));
        
        recalculateBounds();
    }

    private void recalculateBounds() {
        if (volume.isEmpty()) {
            cachedBounds = new AABB(0,0,0,0,0,0);
            return;
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        LongIterator it = volume.iterator();
        while(it.hasNext()) {
            long packed = it.nextLong();
            int x = BlockPos.getX(packed);
            int y = BlockPos.getY(packed);
            int z = BlockPos.getZ(packed);
            
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
            if (z < minZ) minZ = z;
            if (z > maxZ) maxZ = z;
        }
        cachedBounds = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }
}
