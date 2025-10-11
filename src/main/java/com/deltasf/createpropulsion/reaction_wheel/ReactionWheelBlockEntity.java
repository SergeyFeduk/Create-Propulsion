package com.deltasf.createpropulsion.reaction_wheel;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ReactionWheelBlockEntity extends KineticBlockEntity {

    protected ReactionWheelData reactionWheelData;

    public ReactionWheelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        reactionWheelData = new ReactionWheelData();
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        super.initialize();
        if (!level.isClientSide) {
            ReactionWheelAttachment ship = ReactionWheelAttachment.get(level, worldPosition);
            if (ship != null) {
                // Future logic might set data properties here, like rotation axis.
                ReactionWheelForceApplier applier = new ReactionWheelForceApplier(reactionWheelData);
                ship.addApplier(worldPosition, applier);
            }
        }
    }

    @Override
    public void onSpeedChanged(float prevSpeed) {
        super.onSpeedChanged(prevSpeed);
        if (getSpeed() != prevSpeed) {
            // Logic to update torque based on speed will go here.
        }
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
    }

    @Override
    public void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
    }
}
