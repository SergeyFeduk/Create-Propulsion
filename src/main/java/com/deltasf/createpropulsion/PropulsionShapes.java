package com.deltasf.createpropulsion;

import com.deltasf.createpropulsion.utility.ShapeBuilder;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

public class PropulsionShapes {
    public static final VoxelShaper
        THRUSTER = ShapeBuilder.shape()
            .add(Block.box(2, 2, 0, 14, 14, 2))
            .add(Block.box(1, 1, 2, 15, 15, 14))
            .add(Block.box(3, 3, 14, 13, 13, 16))
            .forDirectional(Direction.NORTH),

        INLINE_OPTICAL_SENSOR = ShapeBuilder.shape()
            .add(Block.box(4, 4, 10, 12, 12, 16))
            .forDirectional(Direction.NORTH),

        OPTICAL_SENSOR = ShapeBuilder.shape()
            .add(Block.box(0, 0, 4, 16, 16, 16))
            .add(Block.box(4, 4, 0, 12, 12, 4))
            .forDirectional(Direction.NORTH),

        LODESTONE_TRACKER = ShapeBuilder.shape()
            .add(Block.box(1, 0, 1, 15, 2, 15))
            .add(Block.box(2, 2, 2, 14, 9, 14))
            .add(Block.box(0, 9, 0, 16, 14, 16))
            .forDirectional(Direction.NORTH);
    
}
