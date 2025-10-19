package com.deltasf.createpropulsion.registries;

import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.BiFunction;

import static net.minecraft.core.Direction.UP;

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
            .forDirectional(Direction.NORTH),

        PHYSICS_ASSEMBLER = ShapeBuilder.shape()
            .add(Block.box(0, 0, 0, 16, 14, 16))
            .add(Block.box(1, 14, 1, 15, 16, 15))
            .forDirectional(Direction.NORTH),
        
        PROPELLER = ShapeBuilder.shape()
            .add(Block.box(0, 0, 4, 16, 16, 16))
            .forDirectional(Direction.NORTH),

        HOT_AIR_BURNER = ShapeBuilder.shape()
            .add(Block.box(0, 0, 0, 16, 4, 16))
            .add(Block.box(2, 4, 0, 14, 9, 16))
            .forDirectional(Direction.NORTH);
    
    public static class ShapeBuilder {
        private VoxelShape shape;
    
        public static ShapeBuilder shapeBuilder(VoxelShape shape) {
            return new ShapeBuilder(shape);
        }
    
        public static ShapeBuilder shape() {
            return new ShapeBuilder(Shapes.empty());
        }
    
        private static VoxelShape cuboid(double x1, double y1, double z1, double x2, double y2, double z2) {
            return Block.box(x1, y1, z1, x2, y2, z2);
        }
    
        public ShapeBuilder(VoxelShape shape) {
            this.shape = shape;
        }
    
        public ShapeBuilder add(VoxelShape shape) {
            this.shape = Shapes.or(this.shape, shape);
            return this;
        }
    
        public ShapeBuilder add(double x1, double y1, double z1, double x2, double y2, double z2) {
            return add(cuboid(x1, y1, z1, x2, y2, z2));
        }
    
        public ShapeBuilder erase(double x1, double y1, double z1, double x2, double y2, double z2) {
            this.shape = Shapes.join(shape, cuboid(x1, y1, z1, x2, y2, z2), BooleanOp.ONLY_FIRST);
            return this;
        }
    
        public VoxelShape build() {
            return shape;
        }
    
        public VoxelShaper build(BiFunction<VoxelShape, Direction, VoxelShaper> factory, Direction direction) {
            return factory.apply(shape, direction);
        }
    
        public VoxelShaper build(BiFunction<VoxelShape, Axis, VoxelShaper> factory, Axis axis) {
            return factory.apply(shape, axis);
        }
    
        public VoxelShaper forDirectional(Direction direction) {
            return build(VoxelShaper::forDirectional, direction);
        }
    
        public VoxelShaper forAxis() {
            return build(VoxelShaper::forAxis, Axis.Y);
        }
    
        public VoxelShaper forHorizontalAxis() {
            return build(VoxelShaper::forHorizontalAxis, Axis.Z);
        }
    
        public VoxelShaper forHorizontal(Direction direction) {
            return build(VoxelShaper::forHorizontal, direction);
        }
    
        public VoxelShaper forDirectional() {
            return forDirectional(UP);
        }
    }
}
