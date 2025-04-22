package com.deltasf.createpropulsion.physics_assembler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.world.IPlayer;
import org.valkyrienskies.core.impl.game.ships.ShipDataCommon;
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl;
import org.valkyrienskies.core.impl.networking.simple.SimplePackets;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import com.deltasf.createpropulsion.CreatePropulsion;

import kotlin.jvm.functions.Function0;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;

import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import org.valkyrienskies.mod.util.RelocationUtilKt;

import org.valkyrienskies.mod.common.assembly.ShipAssemblyKt;
import org.valkyrienskies.mod.common.networking.PacketRestartChunkUpdates;
import org.valkyrienskies.mod.common.networking.PacketStopChunkUpdates;

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
        Level world = getLevel();
        if (!(world instanceof ServerLevel)) return;
        ServerLevel level = (ServerLevel) world;

        if (posA == null || posB == null) {
            calculateBounds(this.worldPosition, this.getBlockState());
            if (posA == null || posB == null) return;
        }


        int minX = Math.min(posA.getX(), posB.getX());
        int minY = Math.min(posA.getY(), posB.getY());
        int minZ = Math.min(posA.getZ(), posB.getZ());

        int maxX = Math.max(posA.getX(), posB.getX());
        int maxY = Math.max(posA.getY(), posB.getY());
        int maxZ = Math.max(posA.getZ(), posB.getZ());
        
        SelectedRegion region = getGeometricCenterOfBlocksInRegion(minX, maxX, minY, maxY, minZ, maxZ);
        if (!region.hasBlocks) return; //No blocks -> Nothing to convert into ship
        //Convert selected region to ship
        var parentShip = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
        BlockPos centerOfRegion = new BlockPos((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
        String dimensionId = VSGameUtilsKt.getDimensionId(level);
        Vector3d rp = region.geometricCenter;
        BlockPos creationAnchorPos = new BlockPos(
            (int) Math.floor(rp.x),
            (int) Math.floor(rp.y),
            (int) Math.floor(rp.z)
        );
        ServerShip ship = VSGameUtilsKt.getShipObjectWorld(level).createNewShipAtBlock(VectorConversionsMCKt.toJOML(creationAnchorPos), false, 1, dimensionId);

        BlockPos newShipInternalCenterPos = VectorConversionsMCKt.toBlockPos(
            ship.getChunkClaim().getCenterBlockCoordinates(VSGameUtilsKt.getYRange(level), new Vector3i()));

        //Client sync (Stop updates)
        /*Set<ChunkPos> involvedChunkPositions = new HashSet<>();
        // Determine all involved source and destination chunks
        for (BlockPos currentPos : region.actualBlockPositions) {
            // Calculate offset relative to the integer anchor pos
           int offsetX = currentPos.getX() - creationAnchorPos.getX();
           int offsetY = currentPos.getY() - creationAnchorPos.getY();
           int offsetZ = currentPos.getZ() - creationAnchorPos.getZ();
           // Calculate target pos based on ship's internal center
           BlockPos targetPos = newShipInternalCenterPos.offset(offsetX, offsetY, offsetZ);

           involvedChunkPositions.add(level.getChunkAt(currentPos).getPos());
           involvedChunkPositions.add(level.getChunkAt(targetPos).getPos());
           // sourceToTargetMap.put(currentPos, targetPos); // Store if needed, but recalculating might be simpler
        }

        List<Vector2i> chunkPositionsJOML = involvedChunkPositions.stream()
            .map((c) -> VectorConversionsMCKt.toJOML(c)) // Convert ChunkPos to Vector3i
            .collect(Collectors.toList());
        List<ServerPlayer> nearbyPlayers = level.players(); 
        PacketStopChunkUpdates stopPacket = new PacketStopChunkUpdates(chunkPositionsJOML);
        for (ServerPlayer player : nearbyPlayers) {
            // TODO: Add distance check if desired?
            IPlayer playerWrapper = VSGameUtilsKt.getPlayerWrapper((Player) player);
            if (playerWrapper != null) {
                SimplePackets.sendToClient(stopPacket, playerWrapper);
            }
        }*/

        //Relocation
        for (BlockPos currentPos : region.actualBlockPositions) {
            int offsetX = currentPos.getX() - creationAnchorPos.getX();
            int offsetY = currentPos.getY() - creationAnchorPos.getY();
            int offsetZ = currentPos.getZ() - creationAnchorPos.getZ();
            BlockPos targetPos = newShipInternalCenterPos.offset(offsetX, offsetY, offsetZ);

            LevelChunk fromChunk = level.getChunkAt(currentPos);
            LevelChunk toChunk = level.getChunkAt(targetPos);
            RelocationUtilKt.relocateBlock(
                fromChunk,         // Source Chunk
                currentPos,        // Source Position
                toChunk,           // Destination Chunk (correct one for targetPos)
                targetPos,         // Destination Position (calculated)
                true,              // doUpdate (NOTE: Kotlin version used false here, check implications)
                ship,              // Target ship
                Rotation.NONE      // Rotation
            );
        }
        //Update blocks after relocation
        //CreatePropulsion.LOGGER.debug("Starting block update pass...");
        for (BlockPos originalPos : region.actualBlockPositions) {
            // Recalculate target position (consistent with relocation pass)
            int offsetX = originalPos.getX() - creationAnchorPos.getX();
            int offsetY = originalPos.getY() - creationAnchorPos.getY();
            int offsetZ = originalPos.getZ() - creationAnchorPos.getZ();
            BlockPos targetPos = newShipInternalCenterPos.offset(offsetX, offsetY, offsetZ);

            try {
                LevelChunk destChunk = level.getChunkAt(targetPos); // Get chunk at final destination
                BlockState stateAtDest = destChunk.getBlockState(targetPos); // Get state *after* relocation

                // Call updateBlock using the signature: updateBlock(Level, BlockPos originalSourcePos, BlockPos finalDestPos, BlockState stateAtFinalDest)
                RelocationUtilKt.updateBlock(level, originalPos, targetPos, stateAtDest);
            } catch (Exception e) {
                 // Catch potential errors like chunk not being loaded, though unlikely immediately after relocation
                    //CreatePropulsion.LOGGER.error("Error updating block at target position {}: {}", targetPos, e.getMessage());
            }
        }

        
        /*for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos currentPos = new BlockPos(x, y, z);
                    BlockState blockState = level.getBlockState(currentPos);

                    if (blockState.isAir()) continue;

                    int offsetX = currentPos.getX() - centerOfRegion.getX();
                    int offsetY = currentPos.getY() - centerOfRegion.getY();
                    int offsetZ = currentPos.getZ() - centerOfRegion.getZ();
                    BlockPos targetPos = newShipInternalCenterPos.offset(offsetX, offsetY, offsetZ);

                    LevelChunk fromChunk = level.getChunkAt(currentPos);
                    LevelChunk toChunk = level.getChunkAt(targetPos);
                    RelocationUtilKt.relocateBlock(
                        fromChunk,         // Source Chunk
                        currentPos,        // Source Position
                        toChunk,           // Destination Chunk (in ship space)
                        targetPos,  // Destination Position (in ship space)
                        true,              // doUpdate - update neighbors/lighting
                        ship,           // Target ship
                        Rotation.NONE      // Keep original rotation
                    );
                }
            }
        }*/

        Vector3d finalShipPosInWorld;
        Vector3d finalShipPosInShipyard;
        Quaterniondc finalShipRotation = new Quaterniond(); // Identity rotation initially
        Vector3d finalShipScale = new Vector3d(1, 1, 1);

        if (parentShip != null) {
            // If created on a parent ship, the preciseCenter is relative to the parent's space
            finalShipPosInShipyard = rp; // The desired location in parent's shipyard
            finalShipPosInWorld = parentShip.getShipToWorld().transformPosition(rp.add(0.5, 0.5, 0.5)); // Calculate world pos
            finalShipRotation = parentShip.getTransform().getShipToWorldRotation(); // Inherit rotation
            finalShipScale = parentShip.getTransform().getShipToWorldScaling().mul(1, new Vector3d()); // Inherit and multiply scale
        } else {
            // If created in the world, shipyard and world positions are the same precise center
            finalShipPosInShipyard = rp;
            finalShipPosInWorld = rp.add(0.5, 0.5, 0.5);
        }

        ShipTransformImpl newShipTransform = new ShipTransformImpl(
            finalShipPosInWorld,
            finalShipPosInShipyard,
            finalShipRotation,
            finalShipScale
        );

        // Apply the final transform
        if (ship instanceof ShipDataCommon) {
            ((ShipDataCommon) ship).setTransform(newShipTransform);
        }

        //Client sync (resume chunk updates)
        //PacketRestartChunkUpdates resumePacket = new PacketRestartChunkUpdates(chunkPositionsJOML);
        /*ServerChunkCache chunkSource = level.getChunkSource();

        Function0<Boolean> condition = () -> {
            if (level.getServer().isStopped()) { // Check if server is shutting down
                return false;
            }
            // Check if all involved chunks are loaded and ticking
            for (ChunkPos pos : involvedChunkPositions) { // Use the final Set<ChunkPos>
                // Check if chunk exists and is at least TICKING status
                // getChunk returns null if not loaded to the required status or doesn't exist
                if (chunkSource.getChunk(pos.x, pos.z, ChunkStatus.FULL, false) == null) {
                    // If *any* relevant chunk isn't ticking yet, the condition is false (wait)
                    return false;
                }
            }
            // If the loop completes, all involved chunks are loaded and ticking
            return true;
        };

        Runnable resumeTask = () -> {
            PacketRestartChunkUpdates resumePacket = new PacketRestartChunkUpdates(chunkPositionsJOML);
            // Re-fetch player list within the Runnable's context
            List<ServerPlayer> currentPlayers = level.players();
            for (ServerPlayer player : currentPlayers) {
                IPlayer playerWrapper = VSGameUtilsKt.getPlayerWrapper((Player) player);
                if (playerWrapper != null) {
                    SimplePackets.sendToClient(resumePacket, playerWrapper);
                }
            }
        };

        VSGameUtilsKt.executeIf(level.getServer(), condition, resumeTask);*/

        //level.chunk
        //involvedChunkPositions.toArray()[0].
        /*level.getServer().execute(() -> { // Schedule for main server thread execution
            for (ServerPlayer player : nearbyPlayers) {
                IPlayer playerWrapper = VSGameUtilsKt.getPlayerWrapper((Player) player);
                if (playerWrapper != null) {
                    SimplePackets.sendToClient(resumePacket, playerWrapper);
                }
            }
            //CreatePropulsion.LOGGER.debug("Sent PacketResumeChunkUpdates for {} chunks.", chunkPositionsJOML.size());
        });*/

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
        /*double actualCenterX = (minActualX + maxActualX + 1.0) / 2.0;
        double actualCenterY = (minActualY + maxActualY + 1.0) / 2.0;
        double actualCenterZ = (minActualZ + maxActualZ + 1.0) / 2.0;*/
        return new SelectedRegion(new Vector3d(actualCenterX, actualCenterY, actualCenterZ), blocksFound, blocksList);
    }

    private class SelectedRegion {
        public Vector3d geometricCenter;
        public boolean hasBlocks; 
        public List<BlockPos> actualBlockPositions;
        public SelectedRegion(Vector3d geometricalCenter, boolean hasBlocks, List<BlockPos> blocks) {
            this.geometricCenter = geometricalCenter;
            this.hasBlocks = hasBlocks;
            this.actualBlockPositions = blocks;
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
