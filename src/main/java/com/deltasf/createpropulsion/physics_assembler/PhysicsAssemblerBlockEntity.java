package com.deltasf.createpropulsion.physics_assembler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3i;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.properties.ChunkClaim;
import org.valkyrienskies.core.impl.game.ships.ShipDataCommon;
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl;
import org.valkyrienskies.core.impl.networking.simple.SimplePackets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.networking.PacketRestartChunkUpdates;
import org.valkyrienskies.mod.common.networking.PacketStopChunkUpdates;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.compat.PropulsionCompatibility;
import com.deltasf.createpropulsion.compat.computercraft.ComputerBehaviour;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

@SuppressWarnings("null")
public class PhysicsAssemblerBlockEntity extends SmartBlockEntity {
    private final BlockState AIR = Blocks.AIR.defaultBlockState();

    public PhysicsAssemblerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }
    //CC
    public AbstractComputerBehaviour computerBehaviour;

    private List<Vector2i> chunkPoses = new ArrayList<Vector2i>();
    private List<Vector2i> destchunkPoses = new ArrayList<Vector2i>();

    private final ItemStackHandler itemHandler = createItemHandler();
    private final LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.of(() -> itemHandler);

    //Super secret logic that converts objects spatially restricted to world grid into independent ship entities
    //I spent like two evenings on this
    public void shipify() {
        //Check if everything is valid
        Level world = getLevel();
        if (!(world instanceof ServerLevel)) return;
        ServerLevel level = (ServerLevel) world;

        //Obtain pos from stack
        ItemStack gaugeStack = itemHandler.getStackInSlot(0);
        if (gaugeStack == null) return;
        if (!(gaugeStack.getItem() instanceof AssemblyGaugeItem)) return;

        BlockPos posA = AssemblyGaugeItem.getPosA(gaugeStack);
        BlockPos posB = AssemblyGaugeItem.getPosB(gaugeStack);

        if (posA == null || posB == null) {
            return;
        }

        //Get region
        SelectedRegion region = getGeometricCenterOfBlocksInRegion(level, posA, posB);
        if (!region.hasBlocks) return; //No blocks -> Nothing to convert into ship

        //Create ship
        var parentShip = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
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
        Vector3dc comInShip = ship.getInertiaData().getCenterOfMassInShip();
        Vector3d finalShipPosInShipyard = new Vector3d(comInShip).add(0.5,0.5,0.5);

        Vector3d newShipInternalCenterPosVec = VectorConversionsMCKt.toJOMLD(newShipInternalCenterPos);
        Vector3d creationAnchorPosVec = VectorConversionsMCKt.toJOMLD(creationAnchorPos);

        Vector3d shipComInWorld = new Vector3d(comInShip)
            .sub(newShipInternalCenterPosVec)
            .add(creationAnchorPosVec);

        Vector3d finalShipPosInWorld = shipComInWorld.add(0.5,0.5,0.5);
        Quaterniondc finalShipRotation = new Quaterniond();
        Vector3d finalShipScale = new Vector3d(1, 1, 1);

        if (parentShip != null) {
            finalShipPosInWorld = parentShip.getShipToWorld().transformPosition(shipComInWorld, new Vector3d());
            finalShipRotation = parentShip.getTransform().getShipToWorldRotation();
            finalShipScale.mul(parentShip.getTransform().getShipToWorldScaling());
        }

        ShipTransformImpl newShipTransform = new ShipTransformImpl(
            finalShipPosInWorld,
            finalShipPosInShipyard,
            finalShipRotation,
            finalShipScale
        );

        if (ship instanceof ShipDataCommon shipData) {
            shipData.setTransform(newShipTransform);
            //TODO: Inherit linear velocity and angular momentum
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
        //Transfer block entity and its data. This is even more concerning as neurocorrelate of consciousness is probably located somewhere in blockEntity
        //Also, why the fuck do I write this?

        //This approach should be better
        if (state.hasBlockEntity() && blockEntity != null) {
            CompoundTag tdata = blockEntity.saveWithFullMetadata();
            level.setBlockEntity(BlockEntity.loadStatic(to, state, tdata));
        }
        //But RelocationUtil.kt does something like that. Not sure why
        /*if (state.hasBlockEntity() && blockEntity != null) {
            CompoundTag tdata = blockEntity.saveWithFullMetadata();
            level.setBlockEntity(blockEntity);
            BlockEntity newBlockEntity = level.getBlockEntity(to);
            newBlockEntity.load(tdata);
        }*/
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

    //Gauge

    public GaugeValidationResult canInsertGauge(ItemStack gauge) {
        BlockPos posA = AssemblyGaugeItem.getPosA(gauge);
        BlockPos posB = AssemblyGaugeItem.getPosB(gauge);

        // Check gauge positions
        if (posA == null || posB == null) {
            Component reason = Component.literal("Gauge is not fully configured").withStyle(s -> s.withColor(AssemblyUtility.CANCEL_COLOR));
            return new GaugeValidationResult(false, Optional.of(reason));
        }

        // Check if the assembler is inside the region
        BlockPos selfPos = this.worldPosition;
        int minX = Math.min(posA.getX(), posB.getX());
        int maxX = Math.max(posA.getX(), posB.getX());
        int minY = Math.min(posA.getY(), posB.getY());
        int maxY = Math.max(posA.getY(), posB.getY());
        int minZ = Math.min(posA.getZ(), posB.getZ());
        int maxZ = Math.max(posA.getZ(), posB.getZ());

        if (selfPos.getX() >= minX && selfPos.getX() <= maxX &&
            selfPos.getY() >= minY && selfPos.getY() <= maxY &&
            selfPos.getZ() >= minZ && selfPos.getZ() <= maxZ) {
            Component reason = Component.translatable("createpropulsion.assembler.selection.cannot_be_inside").withStyle(s -> s.withColor(AssemblyUtility.CANCEL_COLOR));
            return new GaugeValidationResult(false, Optional.of(reason));
        }

        // Check distance between assembler and region
        int manhattanDistance = getManhattanDistanceToRegion(this.worldPosition, posA, posB);
        if (manhattanDistance > PropulsionConfig.PHYSICS_ASSEMBLER_MAX_MINK_DISTANCE.get()) {
             Component reason = Component.translatable("createpropulsion.assembler.selection.too_far").withStyle(s -> s.withColor(AssemblyUtility.CANCEL_COLOR));
            return new GaugeValidationResult(false, Optional.of(reason));
        }

        return new GaugeValidationResult(true, Optional.empty());
    }

    public boolean hasGauge() {
        return !itemHandler.getStackInSlot(0).isEmpty();
    }

    public void insertGauge(Player player, InteractionHand hand) {
        if (level == null || level.isClientSide()) {
            return;
        }

        ItemStack gaugeInHand = player.getItemInHand(hand);
        
        if (!(gaugeInHand.getItem() instanceof AssemblyGaugeItem) || !itemHandler.getStackInSlot(0).isEmpty()) {
            return;
        }
        
        ItemStack gaugeToInsert = gaugeInHand.copy();
        gaugeToInsert.setCount(1);
        itemHandler.setStackInSlot(0, gaugeToInsert);

        gaugeInHand.shrink(1);
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    public ItemStack removeGauge() {
        if (level == null || level.isClientSide()) {
            return ItemStack.EMPTY;
        }

        ItemStack extracted = itemHandler.getStackInSlot(0).copy();
        itemHandler.setStackInSlot(0, ItemStack.EMPTY);

        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        return extracted;
    }

    private int getManhattanDistanceToRegion(BlockPos point, BlockPos corner1, BlockPos corner2) {
        int minX = Math.min(corner1.getX(), corner2.getX());
        int maxX = Math.max(corner1.getX(), corner2.getX());
        int minY = Math.min(corner1.getY(), corner2.getY());
        int maxY = Math.max(corner1.getY(), corner2.getY());
        int minZ = Math.min(corner1.getZ(), corner2.getZ());
        int maxZ = Math.max(corner1.getZ(), corner2.getZ());

        int distX = Math.max(0, minX - point.getX()) + Math.max(0, point.getX() - maxX);
        int distY = Math.max(0, minY - point.getY()) + Math.max(0, point.getY() - maxY);
        int distZ = Math.max(0, minZ - point.getZ()) + Math.max(0, point.getZ() - maxZ);

        return distX + distY + distZ;
    }

    public ItemStack getGaugeStack() {
        return this.itemHandler.getStackInSlot(0);
    }

    public float getGaugeRotation() {
        ItemStack gauge = getGaugeStack();
        if (gauge.isEmpty()) {
            return 0;
        }

        BlockPos posA = AssemblyGaugeItem.getPosA(gauge);
        BlockPos posB = AssemblyGaugeItem.getPosB(gauge);

        if (posA == null || posB == null) {
            return 0;
        }

        int minX = Math.min(posA.getX(), posB.getX());
        int maxX = Math.max(posA.getX(), posB.getX());
        int minZ = Math.min(posA.getZ(), posB.getZ());
        int maxZ = Math.max(posA.getZ(), posB.getZ());

        BlockPos point = this.worldPosition;
        int distX = Math.max(0, minX - point.getX()) + Math.max(0, point.getX() - maxX);
        int distZ = Math.max(0, minZ - point.getZ()) + Math.max(0, point.getZ() - maxZ);
        
        if (distZ >= distX) {
            return 0.0f;
        }

        return 90.0f;
    }

    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        if (PropulsionCompatibility.CC_ACTIVE) {
            behaviours.add(computerBehaviour = new ComputerBehaviour(this));
        }
    }

    //Serialization

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.put("inventory", itemHandler.serializeNBT());
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (tag.contains("inventory", Tag.TAG_COMPOUND)) {
            itemHandler.deserializeNBT(tag.getCompound("inventory"));
        }

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

    //Capabilities

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        if (PropulsionCompatibility.CC_ACTIVE && computerBehaviour.isPeripheralCap(cap)) {
            return computerBehaviour.getPeripheralCapability();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    private ItemStackHandler createItemHandler() {
        return new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return stack.getItem() instanceof AssemblyGaugeItem && super.isItemValid(slot, stack);
            }
        };
    }

    public record GaugeValidationResult(boolean isValid, Optional<Component> reason) {}

    private record SelectedRegion (
        Vector3d geometricCenter,
        boolean hasBlocks,
        List<BlockPos> blockPositions,
        Set<ChunkPos> chunkPosSet
    ) {}
}
