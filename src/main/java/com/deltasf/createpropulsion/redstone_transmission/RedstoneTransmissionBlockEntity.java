package com.deltasf.createpropulsion.redstone_transmission;

import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import static com.deltasf.createpropulsion.redstone_transmission.RedstoneTransmissionBlock.SHIFT_LEVEL;

public class RedstoneTransmissionBlockEntity extends SplitShaftBlockEntity {

    public final int MAX_VALUE = 128;
    public RedstoneTransmissionBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);
    }

    public void updateShift(int shift_by){
        int value = getBlockState().getValue(SHIFT_LEVEL);
        int newValue = Mth.clamp(value + shift_by, 1, MAX_VALUE);
        if (value != newValue) {
            detachKinetics();
            level.setBlock(getBlockPos(), getBlockState().setValue(SHIFT_LEVEL, newValue), 3);
            attachKinetics();
        }
    }

    @Override
    public void lazyTick() {
        if(level == null) {
            return;
        }
        Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        int shift_up = level.getSignal(getBlockPos().relative(facing.getClockWise()), facing.getClockWise());
        int shift_down = level.getSignal(getBlockPos().relative(facing.getCounterClockWise()), facing.getCounterClockWise());
        updateShift(shift_up - shift_down);
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (hasSource() && getSourceFacing() == face) return (float) MAX_VALUE / getBlockState().getValue(SHIFT_LEVEL);
        else return 1;
    }
}
