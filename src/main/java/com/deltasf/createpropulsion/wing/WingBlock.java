package com.deltasf.createpropulsion.wing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.core.api.ships.Wing;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.deltasf.createpropulsion.registries.PropulsionShapes;
import com.deltasf.createpropulsion.utility.MathUtility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WingBlock extends DirectionalBlock implements org.valkyrienskies.mod.common.block.WingBlock {

    public WingBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        //TODO: Surrounding-aware placement (skip if shifting)
        //If there is a wing (or copycat wing) block nearby 

        //Oooor, as another potentially better option - use Create's ghosts. For copycat wings also use them and allow width modification when shift clicking it
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public Wing getWing(@Nullable Level level, @Nullable BlockPos pos, @NotNull BlockState state) {
        Vec3i normal = state.getValue(FACING).getNormal();
        //Should I set wingBreakingForce to 10, like CW does?
        return new Wing(VectorConversionsMCKt.toJOMLD(MathUtility.AbsComponents(normal)),150, 30, null, 0);
    }

    @Override
    public VoxelShape getShape(@Nullable BlockState pState, @Nullable BlockGetter pLevel, @Nullable BlockPos pPos, @Nullable CollisionContext pContext) {
        if (pState == null) {
            return PropulsionShapes.WING.get(Direction.UP);
        }
        return PropulsionShapes.WING.get(pState.getValue(FACING));
    }
}
