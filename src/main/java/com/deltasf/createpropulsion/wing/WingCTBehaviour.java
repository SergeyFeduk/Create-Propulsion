package com.deltasf.createpropulsion.wing;

import javax.annotation.Nullable;

import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTType;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class WingCTBehaviour extends ConnectedTextureBehaviour.Base {
    protected CTSpriteShiftEntry shift;

    public WingCTBehaviour(CTSpriteShiftEntry shift) {
        this.shift = shift;
    }

    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos, BlockPos otherPos, Direction face) {
        if (state.getBlock() != other.getBlock()) {
            return false;
        }
        Direction myFacing = state.getValue(DirectionalBlock.FACING);
        Direction otherFacing = other.getValue(DirectionalBlock.FACING);
        return myFacing == otherFacing;
    }

    @Override
    protected Direction getUpDirection(BlockAndTintGetter reader, BlockPos pos, BlockState state, Direction face) {
        Direction facing = state.getValue(DirectionalBlock.FACING);
        if (facing.getAxis().isHorizontal()) return Direction.UP;
        return super.getUpDirection(reader, pos, state, face);
    }

    @Override
    protected boolean reverseUVs(BlockState state, Direction face) {
        Axis facingAxis = state.getValue(DirectionalBlock.FACING).getAxis();
        return face == state.getValue(DirectionalBlock.FACING) && facingAxis.isHorizontal();
    }

    @Override
	public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
		Direction facing = state.getValue(DirectionalBlock.FACING);
        if (direction.getAxis() == facing.getAxis()) return shift;
		return null;
	}

    @Override
    public CTType getDataType(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction direction) {
        return AllCTTypes.OMNIDIRECTIONAL;
    }
}
