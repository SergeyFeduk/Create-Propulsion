package com.deltasf.createpropulsion.heat.burners.solid;

import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SolidBurnerBlockEntity extends AbstractBurnerBlockEntity {
    public SolidBurnerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    protected Direction getHeatCapSide() { return Direction.UP; }

    @Override
    protected int getBaseHeatCapacity() {
        return 400;
    }
}
