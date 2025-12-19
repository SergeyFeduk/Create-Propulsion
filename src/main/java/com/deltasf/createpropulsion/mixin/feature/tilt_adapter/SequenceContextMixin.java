package com.deltasf.createpropulsion.mixin.feature.tilt_adapter;

import com.deltasf.createpropulsion.tilt_adapter.ISnappingSequenceContext;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SequencedGearshiftBlockEntity.SequenceContext.class)
public class SequenceContextMixin implements ISnappingSequenceContext {

    /// This mixin allows to snap specific sequence contexts back to zero. ISnappingSequenceContext should only be emitted by tilt adapter and derivative blocks

    @Unique private boolean shouldSnap = false;

    @Override
    public void setSnapToZero(boolean snap) {
        shouldSnap = snap;
    }

    @Override
    public boolean shouldSnapToZero() {
        return shouldSnap;
    }
}
