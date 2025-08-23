package com.deltasf.createpropulsion.balloons;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;

public class Balloon {
    public final Set<BlockPos> interiorAir;
    public final Map<BlockPos, BalloonSegment> segments;
    public final List<BalloonSegment> bottomLayerSegments;

    public Balloon(Set<BlockPos> interiorAir, Map<BlockPos, BalloonSegment> segments, List<BalloonSegment> bottomLayerSegments) {
        this.interiorAir = interiorAir;
        this.segments = segments;
        this.bottomLayerSegments = bottomLayerSegments;
    }

    public static class BalloonSegment {
        public final BlockPos pos;
        public final int y;
        public final List<BalloonSegment> parents = new ArrayList<>();
        public final List<BalloonSegment> children = new ArrayList<>();

        public BalloonSegment(BlockPos pos) {
            this.pos = pos;
            this.y = pos.getY();
        }
    }
}
