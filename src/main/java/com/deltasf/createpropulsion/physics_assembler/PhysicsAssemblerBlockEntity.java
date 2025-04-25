package com.deltasf.createpropulsion.physics_assembler;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet;
import com.deltasf.createpropulsion.CreatePropulsion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.mod.common.assembly.ShipAssemblyKt;

@SuppressWarnings("null")
public class PhysicsAssemblerBlockEntity extends BlockEntity {
    public PhysicsAssemblerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(CreatePropulsion.PHYSICAL_ASSEMBLER_BLOCK_ENTITY.get(), pos, state);
        //Set poses here
        calculateBounds(pos, state);
    }

    private BlockPos posA;
    private BlockPos posB;

    private void calculateBounds(BlockPos origin, BlockState state) {
        Direction facing = state.getValue(DirectionalBlock.FACING);
        calculateBoundsForDirection(origin, facing);
    }

    private void calculateBoundsForDirection(BlockPos origin, Direction facing) {
        // Using offsets relative to the *assembler block's* position (origin)
        int minXOff = (facing.getAxis() == Direction.Axis.X) ? (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -3) : -1;
        int minYOff = (facing.getAxis() == Direction.Axis.Y) ? (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -3) : -1;
        int minZOff = (facing.getAxis() == Direction.Axis.Z) ? (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -3) : -1;

        int maxXOff = (facing.getAxis() == Direction.Axis.X) ? (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 3 : -1) : 1;
        int maxYOff = (facing.getAxis() == Direction.Axis.Y) ? (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 3 : 1) : 1;
        int maxZOff = (facing.getAxis() == Direction.Axis.Z) ? (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 3 : -1) : 1;

        // The calculated bounds are relative to the assembler's worldPosition
        this.posA = origin.offset(minXOff, minYOff, minZOff);
        this.posB = origin.offset(maxXOff, maxYOff, maxZOff);
    }

    //Super secret logic that converts objects spatially restricted to world grid into independent ship entities
    public void shipify() {
        //Check if everything is valid
        Level world = getLevel();
        if (!(world instanceof ServerLevel)) return;
        ServerLevel level = (ServerLevel) world;

        if (posA == null || posB == null) {
            calculateBounds(this.worldPosition, this.getBlockState());
            if (posA == null || posB == null) return;
        }

        //Get region
        int minX = Math.min(posA.getX(), posB.getX());
        int minY = Math.min(posA.getY(), posB.getY());
        int minZ = Math.min(posA.getZ(), posB.getZ());

        int maxX = Math.max(posA.getX(), posB.getX());
        int maxY = Math.max(posA.getY(), posB.getY());
        int maxZ = Math.max(posA.getZ(), posB.getZ());
        
        SelectedRegion region = getGeometricCenterOfBlocksInRegion(minX, maxX, minY, maxY, minZ, maxZ);
        
        if (!region.hasBlocks) return; //No blocks -> Nothing to convert into ship
        Vector3d rp = region.geometricCenter;
        BlockPos creationAnchorPos = new BlockPos(
            (int) Math.floor(rp.x),
            (int) Math.floor(rp.y),
            (int) Math.floor(rp.z)
        );
        var blockset = new DenseBlockPosSet();
        for (BlockPos bp : region.blockPositions) {
            blockset.add(bp.getX(),bp.getY(),bp.getZ());
        }

        ShipAssemblyKt.createNewShipWithBlocks(creationAnchorPos, blockset, level);
    }

    private SelectedRegion getGeometricCenterOfBlocksInRegion(int regionMinX, int regionMaxX, int regionMinY, int regionMaxY, int regionMinZ, int regionMaxZ) {
        int minActualX = Integer.MAX_VALUE, minActualY = Integer.MAX_VALUE, minActualZ = Integer.MAX_VALUE;
        int maxActualX = Integer.MIN_VALUE, maxActualY = Integer.MIN_VALUE, maxActualZ = Integer.MIN_VALUE;
        boolean blocksFound = false;
        List<BlockPos> blocksList = new ArrayList<>();

        for (int x = regionMinX; x <= regionMaxX; x++) {
            for (int y = regionMinY; y <= regionMaxY; y++) {
                for (int z = regionMinZ; z <= regionMaxZ; z++) {
                    BlockPos currentPos = new BlockPos(x, y, z);
                    BlockState blockState = level.getBlockState(currentPos);

                    if (!blockState.isAir()) {
                        blocksFound = true;
                        blocksList.add(currentPos);
                        minActualX = Math.min(minActualX, x);
                        minActualY = Math.min(minActualY, y);
                        minActualZ = Math.min(minActualZ, z);
                        maxActualX = Math.max(maxActualX, x);
                        maxActualY = Math.max(maxActualY, y);
                        maxActualZ = Math.max(maxActualZ, z);
                    }
                }
            }
        }
        double actualCenterX = (minActualX + maxActualX) / 2.0;
        double actualCenterY = (minActualY + maxActualY) / 2.0;
        double actualCenterZ = (minActualZ + maxActualZ) / 2.0;
        return new SelectedRegion(new Vector3d(actualCenterX, actualCenterY, actualCenterZ), blocksFound, blocksList);
    }

    private class SelectedRegion {
        public Vector3d geometricCenter;
        public boolean hasBlocks; 
        public List<BlockPos> blockPositions;
        public SelectedRegion(Vector3d geometricalCenter, boolean hasBlocks, List<BlockPos> blocks) {
            this.geometricCenter = geometricalCenter;
            this.hasBlocks = hasBlocks;
            this.blockPositions = blocks;
        }
    }

    //Serialization, temp as will be replaced with some item storing corner positions

    @Override
    public void saveAdditional(@Nonnull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("posA", NbtUtils.writeBlockPos(posA));
        tag.put("posB", NbtUtils.writeBlockPos(posB));
    }

    @Override
    public void load(@Nonnull CompoundTag tag) {
        super.load(tag);
        posA = NbtUtils.readBlockPos(tag.getCompound("posA"));
        posB = NbtUtils.readBlockPos(tag.getCompound("posB"));
    }

    @Override
    public CompoundTag getUpdateTag(){
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
