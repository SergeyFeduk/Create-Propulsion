package com.deltasf.createpropulsion.mixin.feature.envelopes;

import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;

import com.deltasf.createpropulsion.balloons.envelopes.IEnvelope;

@Mixin(Block.class)
public class BlockMixin implements IEnvelope {

    /// This mixin implements IEnvelope class for all blocks. Blocks are not evenlopes by design
    /// If specific block type is an envelope - it should override isEnvelope method (e.g. AbstractEnvelopeBlock)

    @Override
    public boolean isEnvelope() {
        return false;
    }
}