package com.deltasf.createpropulsion.utility;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

public class FlickerAwareTicker extends BlockEntityBehaviour {
    public static final BehaviourType<FlickerAwareTicker> TYPE = new BehaviourType<>();

    private final int flickerThreshold;
    private Runnable pendingUpdate;
    private int internalTally = 0;
    
    public FlickerAwareTicker(SmartBlockEntity be, int flickerThreshold) {
        super(be);
        this.flickerThreshold = flickerThreshold;
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    public int getThreshold() {
        return flickerThreshold;
    }

    @Override
    public void tick() {
        super.tick();
        
        if (internalTally > 0) internalTally--;

        if (getWorld().isClientSide) return;
        
        if (pendingUpdate != null) {
            if (canUpdate()) {
                execute(pendingUpdate);
            }
        }
    }

    public void scheduleUpdate(Runnable updateAction) {
        if (canUpdate()) {
            execute(updateAction);
        } else {
            this.pendingUpdate = updateAction;
        }
    }
    
    private void execute(Runnable action) {
        action.run();
        pendingUpdate = null;
        //Speed change costs 10 flicker tally coins :P
        internalTally += 10;
    }

    private boolean canUpdate() {
        if (internalTally > flickerThreshold) return false;
        //Shouldn't trigger like ever given that KBE detaches and reattaches
        if (blockEntity instanceof KineticBlockEntity kbe) {
            return kbe.getFlickerScore() <= flickerThreshold;
        }
        return true;
    }
}
