package com.deltasf.createpropulsion.balloons;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;

public class Balloon {
    public final Set<BlockPos> interiorAir;
    public final List<BalloonSegment> allSegments;
    public final Map<BlockPos, BalloonSegment> blockToSegmentMap;
    public final List<BalloonSegment> bottomLayerSegments;

    public Balloon(Set<BlockPos> interiorAir, List<BalloonSegment> allSegments, Map<BlockPos, BalloonSegment> blockToSegmentMap, List<BalloonSegment> bottomLayerSegments) {
        this.interiorAir = interiorAir;
        this.allSegments = allSegments;
        this.blockToSegmentMap = blockToSegmentMap;
        this.bottomLayerSegments = bottomLayerSegments;
    }

    public static class BalloonSegment {
        public final Set<BlockPos> volume;
        public final int y;
        public final List<BalloonSegment> parents = new ArrayList<>();
        public final List<BalloonSegment> children = new ArrayList<>();


        public BalloonSegment(Set<BlockPos> volume) {
            this.volume = volume;
            // All blocks in a segment are at the same Y level
            this.y = volume.iterator().next().getY();
        }
    }
}