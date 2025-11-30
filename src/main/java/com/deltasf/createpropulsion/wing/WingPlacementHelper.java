package com.deltasf.createpropulsion.wing;

import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.tterrag.registrate.util.entry.BlockEntry;

import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;


public class WingPlacementHelper implements IPlacementHelper {
    private List<BlockEntry<?>> blockEntries;

    public WingPlacementHelper(List<BlockEntry<?>> blockEntries) {
        this.blockEntries = blockEntries;
    }

    @Override
    public Predicate<ItemStack> getItemPredicate() {
        return (stack) -> blockEntries.stream().anyMatch(be -> be.isIn(stack));
    }

    @Override
    public Predicate<BlockState> getStatePredicate() {
        return (state) -> blockEntries.stream().anyMatch(be -> be.has(state));
    }

    @Override
    public PlacementOffset getOffset(@Nonnull Player player, @Nonnull Level world, @Nonnull BlockState state, @Nonnull BlockPos pos, @Nonnull BlockHitResult ray) {
        Vec3 result = ray.getLocation();
        Ship ship = VSGameUtilsKt.getShipManagingPos(world, ray.getBlockPos());
        if (ship != null && !VSGameUtilsKt.isBlockInShipyard(world,result.x,result.y,result.z)) {
            Vector3d tempVec = VectorConversionsMCKt.toJOML(result);
            ship.getWorldToShip().transformPosition(tempVec, tempVec);
            result = VectorConversionsMCKt.toMinecraft(tempVec);
        }

        List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(pos, result,
            state.getValue(BlockStateProperties.FACING).getAxis(),
            dir -> world.getBlockState(pos.relative(dir)).canBeReplaced());

        if (directions.isEmpty()) {
            return PlacementOffset.fail();
        }

        return PlacementOffset.success(pos.relative(directions.get(0)),
            s -> s.setValue(BlockStateProperties.FACING, state.getValue(BlockStateProperties.FACING)));
    }
}