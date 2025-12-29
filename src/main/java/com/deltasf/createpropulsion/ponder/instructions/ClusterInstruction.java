package com.deltasf.createpropulsion.ponder.instructions;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.core.BlockPos;
import net.createmod.ponder.foundation.instruction.TickingInstruction;

public class ClusterInstruction extends TickingInstruction {
    private final PonderPalette color;
    private final Iterable<BlockPos> cluster;
    private final String key;

    public ClusterInstruction(String key, PonderPalette color, int animationTicks, Iterable<BlockPos> cluster) {
        super(false, animationTicks);
        this.color = color;
        this.cluster = cluster;
        this.key = key;
    }

    @Override
    public void tick(PonderScene scene) {
        super.tick(scene);
        scene.getOutliner().showCluster(key, this.cluster).colored(color.getColor()).lineWidth(0.125f / 2.0f);
    }
}
