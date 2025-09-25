package com.deltasf.createpropulsion.optical_sensors;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.compat.PropulsionCompatibility;
import com.deltasf.createpropulsion.compat.computercraft.ComputerBehaviour;
import com.deltasf.createpropulsion.optical_sensors.optical_sensor.OpticalSensorDistanceScrollBehaviour;
import com.deltasf.createpropulsion.optical_sensors.optical_sensor.OpticalSensorFilterValueBox;
import com.deltasf.createpropulsion.registries.PropulsionItems;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public class OpticalSensorBlockEntity extends AbstractOpticalSensorBlockEntity {
    private static final int LENS_LIMIT = 2;
    private FilteringBehaviour filtering;
    private boolean wasFocused;
    public ScrollValueBehaviour targetDistance;
    //CC
    public AbstractComputerBehaviour computerBehaviour;

    public OpticalSensorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state){
        super(typeIn, pos, state);
    }
    
    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(filtering = new FilteringBehaviour(this, new OpticalSensorFilterValueBox(true)));
        targetDistance = new OpticalSensorDistanceScrollBehaviour(this).between(1, PropulsionConfig.OPTICAL_SENSOR_MAX_DISTANCE.get() * 2);
        behaviours.add(targetDistance);
        targetDistance.setValue(32);

        if (PropulsionCompatibility.CC_ACTIVE) {
            behaviours.add(computerBehaviour = new ComputerBehaviour(this));
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (PropulsionCompatibility.CC_ACTIVE && computerBehaviour.isPeripheralCap(cap)) {
            return computerBehaviour.getPeripheralCapability();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public float getZAxisOffset(){
        return 0.625f; // 0.5 + 2/16
    }

    @Override
    public int getLensLimit() { return LENS_LIMIT; }

    public void setMaxDistance(int distance) {
        targetDistance.setValue(distance);
    }

    public int getMaxPossibleRaycastDistance() {
        boolean isFocused = hasLens(PropulsionItems.FOCUS_LENS.get());
        int maxDistance = PropulsionConfig.OPTICAL_SENSOR_MAX_DISTANCE.get() * (isFocused ? 2 : 1);
        return maxDistance;
    }

    @Override
    protected float getMaxRaycastDistance() {
        boolean isFocused = hasLens(PropulsionItems.FOCUS_LENS.get());
        if (isFocused != wasFocused) {
            wasFocused = isFocused;
            int maxDistance = PropulsionConfig.OPTICAL_SENSOR_MAX_DISTANCE.get() * (isFocused ? 2 : 1);
            int clampedValue = Math.min(targetDistance.value, maxDistance);
            targetDistance.setValue(clampedValue);
        }
        return targetDistance.getValue();
    }

    @Override
    protected void updateRedstoneSignal(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockPos pos, int rawNewPower, @Nullable BlockPos hitBlockPos) {
        int finalPower = rawNewPower;
        if (hitBlockPos == null || !filterTestBlock(level, hitBlockPos)) {
             finalPower = 0;
        }

        int oldPower = state.getValue(AbstractOpticalSensorBlock.POWER);
        if (oldPower != finalPower) {
            BlockState updatedState = state.setValue(AbstractOpticalSensorBlock.POWER, finalPower).setValue(AbstractOpticalSensorBlock.POWERED, finalPower > 0);
            level.setBlock(pos, updatedState, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);

            Direction facingDir = state.getValue(AbstractOpticalSensorBlock.FACING);
            level.updateNeighborsAt(pos.relative(facingDir.getOpposite()), state.getBlock());
            level.updateNeighborsAt(pos, state.getBlock());
        }
    }

    private boolean filterTestBlock(Level level, BlockPos posToTest) {
        ItemStack filterStack = this.filtering.getFilter();
        if (filterStack.isEmpty()) {
            return true;
        }

        Block block = level.getBlockState(posToTest).getBlock();
        ItemStack blockAsStack = new ItemStack(block.asItem());

        if (blockAsStack.isEmpty()) {
            return false;
        }

        return this.filtering.test(blockAsStack);
    }
}
