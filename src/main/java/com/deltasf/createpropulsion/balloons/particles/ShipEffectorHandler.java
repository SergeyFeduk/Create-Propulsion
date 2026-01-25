package com.deltasf.createpropulsion.balloons.particles;

import org.joml.Vector3f;

import com.deltasf.createpropulsion.balloons.ClientBalloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.balloons.particles.effectors.EffectorBucket;
import com.deltasf.createpropulsion.balloons.particles.effectors.HapEffector;
import com.deltasf.createpropulsion.balloons.particles.effectors.HoleEffector;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public class ShipEffectorHandler {
    private final Long2ObjectOpenHashMap<EffectorBucket> effectors = new Long2ObjectOpenHashMap<>();
    private final ShipParticleHandler parent;
    
    // Config constants
    private static final float HOLE_EJECT_STRENGTH = 2.8f;
    private static final int EFFECTOR_RADIUS = 2; // 5x5x5

    public ShipEffectorHandler(ShipParticleHandler parent) {
        this.parent = parent;
    }

    public EffectorBucket getBucket(long key) {
        return effectors.get(key);
    }
    
    public void clear() {
        effectors.clear();
    }

    public EffectorBucket getOrCreateBucket(long key) {
        return effectors.computeIfAbsent(key, k -> new EffectorBucket());
    }

    public void onStructureUpdate(Level level, ClientBalloon balloon) {
        removeEffectorsForBalloon(balloon.id);

        if (balloon.holes.isEmpty()) return;
        
        for (BlockPos hole : balloon.holes) {
            addHoleEffectorInternal(level, balloon, hole);
        }
    }

    public void onDeltaUpdate(Level level, ClientBalloon balloon, long[] addedHoles, long[] removedHoles) {
        if (removedHoles.length > 0) {
            for (long packed : removedHoles) {
                removeHoleEffectorInternal(balloon.id, BlockPos.of(packed));
            }
        }

        if (addedHoles.length > 0) {
            for (long packed : addedHoles) {
                addHoleEffectorInternal(level, balloon, BlockPos.of(packed));
            }
        }
    }

    private void addHoleEffectorInternal(Level level, ClientBalloon balloon, BlockPos hole) {
        Vector3f dirAccumulator = calculateHoleDirection(level, balloon, hole);

        // Only register if we have a valid flow direction
        if (dirAccumulator.lengthSquared() > 0.001f) {
            HapEffector holeEffector = new HoleEffector(
                balloon.id,
                parent.getAnchorX(), parent.getAnchorY(), parent.getAnchorZ(),
                hole.getX(), hole.getY(), hole.getZ(),
                dirAccumulator, 
                HOLE_EJECT_STRENGTH
            );

            // Register in 5x5 area
            BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
            for (int x = -EFFECTOR_RADIUS; x <= EFFECTOR_RADIUS; x++) {
                for (int y = -EFFECTOR_RADIUS; y <= EFFECTOR_RADIUS; y++) {
                    for (int z = -EFFECTOR_RADIUS; z <= EFFECTOR_RADIUS; z++) {
                        mut.set(hole.getX() + x, hole.getY() + y, hole.getZ() + z);
                        addEffectorToBucket(mut, holeEffector);
                    }
                }
            }
        }
    }

    private void removeHoleEffectorInternal(int balloonId, BlockPos hole) {
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        for (int x = -EFFECTOR_RADIUS; x <= EFFECTOR_RADIUS; x++) {
            for (int y = -EFFECTOR_RADIUS; y <= EFFECTOR_RADIUS; y++) {
                for (int z = -EFFECTOR_RADIUS; z <= EFFECTOR_RADIUS; z++) {
                    mut.set(hole.getX() + x, hole.getY() + y, hole.getZ() + z);
                    
                    long key = mut.asLong();
                    EffectorBucket bucket = effectors.get(key);
                    if (bucket != null) {
                        bucket.removeSpecific(balloonId, hole);
                    }
                }
            }
        }
    }
    
    private void removeEffectorsForBalloon(int balloonId) {
        var iter = effectors.values().iterator();
        while (iter.hasNext()) {
            EffectorBucket bucket = iter.next();
            bucket.removeByBalloonId(balloonId);
            if (bucket.isEmpty()) {
                iter.remove();
            }
        }
    }

    private void addEffectorToBucket(BlockPos pos, HapEffector effector) {
        long key = pos.asLong();
        EffectorBucket bucket = effectors.get(key);
        if (bucket == null) {
            bucket = new EffectorBucket();
            effectors.put(key, bucket);
        }
        bucket.add(effector);
    }

    private Vector3f calculateHoleDirection(Level level, ClientBalloon balloon, BlockPos hole) {
        Vector3f dirAccumulator = new Vector3f();
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();

        for (Direction d : Direction.values()) {
            neighborPos.setWithOffset(hole, d);
            long neighborPacked = neighborPos.asLong();
            
            if (balloon.volume.contains(neighborPacked)) {
                dirAccumulator.add(-d.getStepX() * 1.0f, -d.getStepY() * 1.0f, -d.getStepZ() * 1.0f);
            } 
            else if (balloon.holes.contains(neighborPos)) {
                dirAccumulator.add(d.getStepX() * 0.1f, d.getStepY() * 0.1f, d.getStepZ() * 0.1f);
            } 
            else {
                boolean isEnvelope = HaiGroup.isHab(neighborPos, level);
                
                if (!isEnvelope && balloon.getBounds().contains(neighborPos.getCenter())) {
                    dirAccumulator.add(d.getStepX() * 1.0f, d.getStepY() * 1.0f, d.getStepZ() * 1.0f);
                }
            }
        }
        return dirAccumulator;
    }
}
