package com.deltasf.createpropulsion.physics_assembler;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Vector3d;
import org.slf4j.Logger;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import org.valkyrienskies.mod.common.assembly.ShipAssembler;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.compat.VS2AssemblyCompat;
import com.deltasf.createpropulsion.compat.PropulsionCompatibility;
import com.deltasf.createpropulsion.compat.computercraft.ComputerBehaviour;
import com.deltasf.createpropulsion.network.PropulsionPackets;
import com.deltasf.createpropulsion.physics_assembler.packets.AssemblyFailedPacket;
import com.mojang.logging.LogUtils;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

@SuppressWarnings("null")
public class PhysicsAssemblerBlockEntity extends SmartBlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();

    public PhysicsAssemblerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }
    //CC
    public AbstractComputerBehaviour computerBehaviour;

    private boolean assemblyPending;

    private final ItemStackHandler itemHandler = createItemHandler();
    private final LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.of(() -> itemHandler);

    public void shipify() {
        Level world = getLevel();
        if (!(world instanceof ServerLevel level) || assemblyPending) return;

        ItemStack gaugeStack = itemHandler.getStackInSlot(0);
        if (gaugeStack.isEmpty() || !(gaugeStack.getItem() instanceof AssemblyGaugeItem)) return;

        BlockPos posA = AssemblyGaugeItem.getPosA(gaugeStack);
        BlockPos posB = AssemblyGaugeItem.getPosB(gaugeStack);

        if (posA == null || posB == null) return;

        CompletableFuture<Void> chunkLoadFuture = requestMissingRegionChunks(level, posA, posB);
        if (chunkLoadFuture != null) {
            assemblyPending = true;
            chunkLoadFuture.whenComplete((unused, throwable) ->
                level.getServer().execute(() -> {
                    assemblyPending = false;
                    if (isRemoved() || getLevel() != level) return;
                    if (throwable != null) {
                        LOGGER.error("Failed to load Create Propulsion assembly chunks at {}", worldPosition, throwable);
                        sendAssemblyFailed(level);
                        return;
                    }
                    shipify();
                })
            );
            return;
        }

        SelectedRegion region = getGeometricCenterOfBlocksInRegion(level, posA, posB);
        if (region.hasBlacklistedBlocks()) {
            sendAssemblyFailed(level);
            return;
        }
        if (!region.hasBlocks()) return;

        assemblyPending = true;
        VS2AssemblyCompat.queueAssemble(level, region.blockPositions(), 1.0)
            .whenComplete((result, throwable) ->
                level.getServer().execute(() -> {
                    assemblyPending = false;
                    if (isRemoved() || getLevel() != level) return;
                    if (throwable != null) {
                        LOGGER.error("Create Propulsion queued assembly failed at {}", worldPosition, throwable);
                        sendAssemblyFailed(level);
                    }
                })
            );
    }

    @Nullable
    private CompletableFuture<Void> requestMissingRegionChunks(ServerLevel level, BlockPos posA, BlockPos posB) {
        Set<ChunkPos> chunks = new LinkedHashSet<>();
        int minChunkX = Math.min(posA.getX(), posB.getX()) >> 4;
        int maxChunkX = Math.max(posA.getX(), posB.getX()) >> 4;
        int minChunkZ = Math.min(posA.getZ(), posB.getZ()) >> 4;
        int maxChunkZ = Math.max(posA.getZ(), posB.getZ()) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                chunks.add(new ChunkPos(chunkX, chunkZ));
            }
        }
        return VS2AssemblyCompat.requestChunks(level, chunks);
    }

    @Nullable
    private LevelChunk getLoadedChunk(ServerLevel level, int chunkX, int chunkZ) {
        return level.getChunkSource().getChunkNow(chunkX, chunkZ);
    }

    private void sendAssemblyFailed(ServerLevel level) {
        LevelChunk chunk = getLoadedChunk(level, worldPosition.getX() >> 4, worldPosition.getZ() >> 4);
        if (chunk != null) {
            PropulsionPackets.sendToTracking(new AssemblyFailedPacket(worldPosition), chunk);
        }
    }

    private SelectedRegion getGeometricCenterOfBlocksInRegion(ServerLevel level, BlockPos posA, BlockPos posB) {
        int minActualX = Integer.MAX_VALUE, minActualY = Integer.MAX_VALUE, minActualZ = Integer.MAX_VALUE;
        int maxActualX = Integer.MIN_VALUE, maxActualY = Integer.MIN_VALUE, maxActualZ = Integer.MIN_VALUE;

        int regionMinX = Math.min(posA.getX(), posB.getX());
        int regionMinY = Math.min(posA.getY(), posB.getY());
        int regionMinZ = Math.min(posA.getZ(), posB.getZ());

        int regionMaxX = Math.max(posA.getX(), posB.getX());
        int regionMaxY = Math.max(posA.getY(), posB.getY());
        int regionMaxZ = Math.max(posA.getZ(), posB.getZ());

        boolean blocksFound = false;
        Set<BlockPos> blocksList = new LinkedHashSet<>();
        BlockPos.MutableBlockPos currentPos = new BlockPos.MutableBlockPos();
        long currentChunkKey = Long.MIN_VALUE;
        LevelChunk currentChunk = null;

        for (int x = regionMinX; x <= regionMaxX; x++) {
            for (int y = regionMinY; y <= regionMaxY; y++) {
                for (int z = regionMinZ; z <= regionMaxZ; z++) {
                    long chunkKey = ChunkPos.asLong(x >> 4, z >> 4);
                    if (chunkKey != currentChunkKey) {
                        currentChunkKey = chunkKey;
                        currentChunk = getLoadedChunk(level, x >> 4, z >> 4);
                    }
                    if (currentChunk == null) {
                        return new SelectedRegion(new Vector3d(), false, new LinkedHashSet<>(), false);
                    }
                    currentPos.set(x, y, z);
                    BlockState blockState = currentChunk.getBlockState(currentPos);
                    if (!blockState.isAir()) {
                        if (AssemblyBlacklistManager.isBlacklisted(blockState.getBlock())) {
                            return new SelectedRegion(new Vector3d(), false, new LinkedHashSet<>(), true);
                        }
                        blocksFound = true;
                        blocksList.add(currentPos.immutable());
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
        if (!blocksFound) {
            return new SelectedRegion(new Vector3d(), false, blocksList, false);
        }
        double actualCenterX = (minActualX + maxActualX) / 2.0;
        double actualCenterY = (minActualY + maxActualY) / 2.0;
        double actualCenterZ = (minActualZ + maxActualZ) / 2.0;
        return new SelectedRegion(new Vector3d(actualCenterX, actualCenterY, actualCenterZ), true, blocksList, false);
    }

    //Gauge

    public GaugeValidationResult canInsertGauge(ItemStack gauge) {
        BlockPos posA = AssemblyGaugeItem.getPosA(gauge);
        BlockPos posB = AssemblyGaugeItem.getPosB(gauge);

        //Check gauge positions
        if (posA == null || posB == null) {
            Component reason = Component.literal("Gauge is not fully configured").withStyle(s -> s.withColor(AssemblyUtility.CANCEL_COLOR));
            return new GaugeValidationResult(false, Optional.of(reason));
        }

        //Check if the assembler is inside the region
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

        //Check distance between assembler and region
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

    public void spawnEffect(ParticleOptions particle, float maxMotion, int amount) {
        Level world = getLevel();
        if (world == null || !world.isClientSide)
            return;

        RandomSource r = world.random;
        Vec3 center = VecHelper.getCenterOf(getBlockPos());

        for (int i = 0; i < amount; i++) {
            Vec3 motion = VecHelper.offsetRandomly(Vec3.ZERO, r, maxMotion);
            Direction face = Direction.getNearest(motion.x, motion.y, motion.z);
            double px = center.x + (face.getStepX() * 0.55);
            double py = center.y + (face.getStepY() * 0.55);
            double pz = center.z + (face.getStepZ() * 0.55);
            float spread = 0.9f;
            
            if (face.getAxis() != Direction.Axis.X) {
                px += (r.nextFloat() - 0.5f) * spread;
            }
            if (face.getAxis() != Direction.Axis.Y) {
                py += (r.nextFloat() - 0.5f) * spread;
            }
            if (face.getAxis() != Direction.Axis.Z) {
                pz += (r.nextFloat() - 0.5f) * spread;
            }

            world.addParticle(particle, px, py, pz, motion.x, motion.y, motion.z);
        }
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
        Set<BlockPos> blockPositions,
        boolean hasBlacklistedBlocks
    ) {}
}
