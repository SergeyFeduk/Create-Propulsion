package com.deltasf.createpropulsion.mixin;

import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;

import com.deltasf.createpropulsion.balloons.envelopes.IEnvelope;

@Mixin(Block.class)
public class BlockMixin implements IEnvelope {
    @Override
    public boolean isEnvelope() {
        return false;
    }
}