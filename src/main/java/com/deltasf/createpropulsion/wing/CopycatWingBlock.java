package com.deltasf.createpropulsion.wing;

import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.core.api.ships.Wing;
import org.valkyrienskies.mod.common.block.WingBlock;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.deltasf.createpropulsion.registries.PropulsionShapes;
import com.deltasf.createpropulsion.utility.MathUtility;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CopycatWingBlock extends CopycatBlock implements WingBlock{
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private final int width;
    private final Supplier<Item> baseItemSupplier;

    private static final Map<Integer, VoxelShaper> wingShapers = Map.of(
        4, PropulsionShapes.WING,
        8, PropulsionShapes.WING_8,
        12, PropulsionShapes.WING_12
    );

    public CopycatWingBlock(Properties properties, int width, Supplier<Item> baseItemSupplier) {
        super(properties);
        this.width = width;
        this.baseItemSupplier = baseItemSupplier;
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(FACING));
    }

    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return wingShapers.get(this.width).get(state.getValue(FACING));
    }
    
    @Override
    public Wing getWing(@Nullable Level level, @Nullable BlockPos pos, @NotNull BlockState state) {
        Vec3i normal = state.getValue(FACING).getNormal();
        CopycatWingProperties properties = CopycatWingProperties.PROPERTIES_BY_WIDTH.getOrDefault(this.width, new CopycatWingProperties(0, 0));

        return new Wing(
            VectorConversionsMCKt.toJOMLD(MathUtility.AbsComponents(normal)),
            properties.lift(),
            properties.drag(),
            null,
            0
        );
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        BlockState material = getMaterial(level, pos);
        if (AllBlocks.COPYCAT_BASE.has(material) || (player != null && player.isShiftKeyDown())) {
            return new ItemStack(baseItemSupplier.get());
        }
        
        return material.getCloneItemStack(target, level, pos, player);
    }

    @Override
    public boolean canConnectTexturesToward(BlockAndTintGetter reader, BlockPos fromPos, BlockPos toPos, BlockState state) {
        BlockState toState = reader.getBlockState(toPos);
        if (!toState.is(this)) {
            return false;
        }

        return state.getValue(FACING) == toState.getValue(FACING);
    }

    @Override
    public boolean isIgnoredConnectivitySide(BlockAndTintGetter reader, BlockState state, Direction face, @Nullable BlockPos fromPos, @Nullable BlockPos toPos) {
        Direction facing = state.getValue(FACING);
        return face == facing || face == facing.getOpposite();
    }

    //TODO: perhaps remove?
    @Override
    public boolean canFaceBeOccluded(BlockState state, Direction face) {
        return false;
    }

    @Override
    public boolean shouldFaceAlwaysRender(BlockState state, Direction face)  {
        return false;
    }
}
