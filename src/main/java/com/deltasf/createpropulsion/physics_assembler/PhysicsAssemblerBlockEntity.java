package com.deltasf.createpropulsion.physics_assembler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.properties.ChunkClaim;
import org.valkyrienskies.core.impl.game.ships.ShipDataCommon;
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl;
import org.valkyrienskies.core.impl.networking.simple.SimplePackets;
import com.deltasf.createpropulsion.CreatePropulsion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ticks.ScheduledTick;

import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.networking.PacketRestartChunkUpdates;
import org.valkyrienskies.mod.common.networking.PacketStopChunkUpdates;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

@SuppressWarnings("null")
public class PhysicsAssemblerBlockEntity extends BlockEntity {
    private final BlockState AIR = Blocks.AIR.defaultBlockState();
    public PhysicsAssemblerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(CreatePropulsion.PHYSICAL_ASSEMBLER_BLOCK_ENTITY.get(), pos, state);
        //Set poses here
        calculateBounds(pos, state);
    }

    private BlockPos posA;
    private BlockPos posB;

    private List<Vector2i> chunkPoses = new ArrayList<Vector2i>();
    private List<Vector2i> destchunkPoses = new ArrayList<Vector2i>();

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
    //I spent like two evenings on this
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
        SelectedRegion region = getGeometricCenterOfBlocksInRegion(level, posA, posB);
        if (!region.hasBlocks) return; //No blocks -> Nothing to convert into ship

        //Create ship
        var parentShip = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
        //BlockPos centerOfRegion = new BlockPos((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
        String dimensionId = VSGameUtilsKt.getDimensionId(level);
        Vector3d gc = region.geometricCenter;
        BlockPos creationAnchorPos = new BlockPos(
            (int) Math.floor(gc.x),
            (int) Math.floor(gc.y),
            (int) Math.floor(gc.z)
        );
        ServerShip ship = VSGameUtilsKt.getShipObjectWorld(level).createNewShipAtBlock(
            VectorConversionsMCKt.toJOML(creationAnchorPos), false, 1, dimensionId);
        ChunkClaim claim = ship.getChunkClaim();
        BlockPos newShipInternalCenterPos = VectorConversionsMCKt.toBlockPos(
                claim.getCenterBlockCoordinates(VSGameUtilsKt.getYRange(level), new Vector3i()));
        //Get and store affected FROM chunks
        chunkPoses.clear();
        var it = region.chunkPosSet.iterator();
        while (it.hasNext()) {
            ChunkPos pos = it.next();
            chunkPoses.add(new Vector2i(pos.x, pos.z));
        }
        //Get and store affected TO chunks
        destchunkPoses.clear();
        Set<ChunkPos> destSet = new HashSet<>();
        for (BlockPos pos : region.blockPositions) {
            BlockPos relative = pos.subtract(creationAnchorPos);
            BlockPos shipPos = newShipInternalCenterPos.offset(relative);
            destSet.add(level.getChunk(shipPos).getPos());
        }
        var dit = destSet.iterator();
        while (dit.hasNext()) {
            ChunkPos pos = dit.next();
            destchunkPoses.add(new Vector2i(pos.x, pos.z));
        }
        //Send packets to stop updating chunks 
        SimplePackets.sendToAllClients(new PacketStopChunkUpdates(chunkPoses));
        //Copy blocks from region to shipyard
        for (BlockPos pos : region.blockPositions) {
            BlockPos relative = pos.subtract(creationAnchorPos);
            BlockPos shipPos = newShipInternalCenterPos.offset(relative);
            copyBlock(level, pos, shipPos);
        }
        //Remove original blocks
        for (BlockPos pos : region.blockPositions) {
            removeBlock(level, pos);
        }
        //Trigger updates 
        for (BlockPos pos : region.blockPositions) {
            BlockPos relative = pos.subtract(creationAnchorPos);
            BlockPos shipPos = newShipInternalCenterPos.offset(relative);
            updateBlock(level, pos, shipPos, level.getBlockState(shipPos));
        }

        //Teleport ship to actual location
        Vector3d finalShipPosInWorld;
        Vector3d finalShipPosInShipyard;
        Quaterniondc finalShipRotation = new Quaterniond(); // Identity rotation initially
        Vector3d finalShipScale = new Vector3d(1, 1, 1);
        if (parentShip != null) {
            finalShipPosInShipyard = gc;
            finalShipPosInWorld = parentShip.getShipToWorld().transformPosition(gc.add(0.5, 0.5, 0.5));
            finalShipRotation = parentShip.getTransform().getShipToWorldRotation();
            finalShipScale = parentShip.getTransform().getShipToWorldScaling().mul(1, new Vector3d());
        } else {
            finalShipPosInShipyard = gc;
            finalShipPosInWorld = gc.add(0.5, 0.5, 0.5);
        }

        ShipTransformImpl newShipTransform = new ShipTransformImpl(
            finalShipPosInWorld,
            finalShipPosInShipyard,
            finalShipRotation,
            finalShipScale
        );

        if (ship instanceof ShipDataCommon) {
            ((ShipDataCommon) ship).setTransform(newShipTransform);
        }

        //Sync FROM chunks to resume updated when TO chunks start to tick
        VSGameUtilsKt.executeIf(level.getServer(), 
            () -> destchunkPoses.stream().allMatch(chunkPos -> VSGameUtilsKt.isTickingChunk(level, chunkPos.x, chunkPos.y)), 
            () -> {
                SimplePackets.sendToAllClients(new PacketRestartChunkUpdates(chunkPoses));
                chunkPoses.clear();
            }
        );
    }

    private void copyBlock(Level level, BlockPos from, BlockPos to){
        BlockState state = level.getBlockState(from);
        BlockEntity blockEntity = level.getBlockEntity(from);
        level.getChunk(to).setBlockState(to, state, false);
        //Transfer scheduled tick from original to a copy. This rises a lot of philosophical concerns but we have a job to do
        if (level.getBlockTicks().hasScheduledTick(from, state.getBlock())) {
            level.getBlockTicks().schedule(new ScheduledTick<Block>(state.getBlock(), to, 0, 0));
        }
        //Transfer block entity and its data. This is even more concerning as neurocorellate of consciousness is probably located somewhere in blockEntity
        if (state.hasBlockEntity() && blockEntity != null) {
            CompoundTag tdata = blockEntity.saveWithFullMetadata();
            level.setBlockEntity(blockEntity);
            BlockEntity newBlockEntity = level.getBlockEntity(to);
            newBlockEntity.load(tdata);
        }

        
    }

    private void removeBlock(Level level, BlockPos pos){
        level.removeBlockEntity(pos);
        level.getChunk(pos).setBlockState(pos, AIR, false);
    }

    private void updateBlock(Level level, BlockPos from, BlockPos to, BlockState toState){
        int flags = 11 | Block.UPDATE_MOVE_BY_PISTON | Block.UPDATE_SUPPRESS_DROPS;
        int recursionLeft = 511;

        //FROM
        level.setBlocksDirty(from, toState, AIR);
        level.sendBlockUpdated(from, toState, AIR, flags);
        level.blockUpdated(from, AIR.getBlock());
        //Update neighboring blocks
        AIR.updateIndirectNeighbourShapes(level, from, flags, recursionLeft - 1);
        AIR.updateNeighbourShapes(level, from, flags, recursionLeft);
        AIR.updateIndirectNeighbourShapes(level, from, flags, recursionLeft);
        //And lighting too
        level.getChunkSource().getLightEngine().checkBlock(from);

        //TO
        level.setBlocksDirty(to, AIR, toState);
        level.sendBlockUpdated(to, AIR, toState, flags);
        level.blockUpdated(to, toState.getBlock());
        //Redstone
        if (!level.isClientSide && toState.hasAnalogOutputSignal()) {
            level.updateNeighbourForOutputSignal(to, toState.getBlock());
        }

        //And lighting too
        level.getChunkSource().getLightEngine().checkBlock(to);

    }

    private SelectedRegion getGeometricCenterOfBlocksInRegion(Level level, BlockPos posA, BlockPos posB) {
        int minActualX = Integer.MAX_VALUE, minActualY = Integer.MAX_VALUE, minActualZ = Integer.MAX_VALUE;
        int maxActualX = Integer.MIN_VALUE, maxActualY = Integer.MIN_VALUE, maxActualZ = Integer.MIN_VALUE;

        int regionMinX = Math.min(posA.getX(), posB.getX());
        int regionMinY = Math.min(posA.getY(), posB.getY());
        int regionMinZ = Math.min(posA.getZ(), posB.getZ());

        int regionMaxX = Math.max(posA.getX(), posB.getX());
        int regionMaxY = Math.max(posA.getY(), posB.getY());
        int regionMaxZ = Math.max(posA.getZ(), posB.getZ());

        boolean blocksFound = false;
        List<BlockPos> blocksList = new ArrayList<>();
        Set<ChunkPos> chunkPosSet = new HashSet<ChunkPos>();

        //Iterate through all of the region coordinates
        for (int x = regionMinX; x <= regionMaxX; x++) {
            for (int y = regionMinY; y <= regionMaxY; y++) {
                for (int z = regionMinZ; z <= regionMaxZ; z++) {
                    BlockPos currentPos = new BlockPos(x, y, z);
                    BlockState blockState = level.getBlockState(currentPos);
                    //If block is valid
                    if (!blockState.isAir()) {
                        //Update lists
                        blocksFound = true;
                        blocksList.add(currentPos);
                        chunkPosSet.add(level.getChunk(currentPos).getPos());
                        //Update region bounds
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
        return new SelectedRegion(new Vector3d(actualCenterX, actualCenterY, actualCenterZ), blocksFound, blocksList, chunkPosSet);
    }

    private class SelectedRegion {
        public Vector3d geometricCenter;
        public boolean hasBlocks; 
        public List<BlockPos> blockPositions;
        public Set<ChunkPos> chunkPosSet;

        public SelectedRegion(Vector3d geometricalCenter, boolean hasBlocks, List<BlockPos> blocks, Set<ChunkPos> chunkPosSet) {
            this.geometricCenter = geometricalCenter;
            this.hasBlocks = hasBlocks;
            this.blockPositions = blocks;
            this.chunkPosSet = chunkPosSet;
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
