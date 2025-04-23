package com.deltasf.createpropulsion.optical_sensors;

import java.util.List;

import com.deltasf.createpropulsion.Config;
import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.optical_sensors.optical_sensor.OpticalSensorDistanceScrollBehaviour;
import com.deltasf.createpropulsion.optical_sensors.optical_sensor.OpticalSensorFilterValueBox;
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

public class OpticalSensorBlockEntity extends InlineOpticalSensorBlockEntity {
    private FilteringBehaviour filtering;
    public ScrollValueBehaviour targetDistance;

    public OpticalSensorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state){
        super(CreatePropulsion.OPTICAL_SENSOR_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public float getZAxisOffset(){
        return 0.625f; //0.5 + 2/16
    }
    
    @Override
    protected float getMaxRaycastDistance(){
        return targetDistance.getValue();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(filtering = new FilteringBehaviour(this, new OpticalSensorFilterValueBox(true)));
        behaviours.add(targetDistance = new OpticalSensorDistanceScrollBehaviour(this).between(1, Config.OPTICAL_SENSOR_MAX_DISTANCE.get()));
        targetDistance.setValue(32);
    }

    //Filtering
    @Override
    protected void updateRedstoneSignal(Level level, BlockState state, BlockPos pos, int newPower, BlockPos hitBlockPos) {
        //Filter 
        if (!filterTestBlock(level, hitBlockPos)) {
            newPower = 0;
        }

        int oldPower = state.getValue(OpticalSensorBlock.POWER);
        if (oldPower != newPower) {
            //Update block
            BlockState updatedState = state.setValue(OpticalSensorBlock.POWER, newPower).setValue(OpticalSensorBlock.POWERED, newPower > 0);
            level.setBlock(pos, updatedState, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
            //Update block behind
            Direction facingDir = state.getValue(OpticalSensorBlock.FACING).getOpposite();
            level.updateNeighborsAt(pos.relative(facingDir), state.getBlock());
        }
    }

    private boolean filterTestBlock(Level level, BlockPos posToTest) {
        ItemStack filterStack = this.filtering.getFilter();
        if (filterStack.isEmpty()) return true;
        Block block = level.getBlockState(posToTest).getBlock();
        ItemStack blockAsStack = new ItemStack(block.asItem());
        if (blockAsStack.isEmpty()) return false; //Just in case everything is broken
        return this.filtering.test(blockAsStack);
    }
}
