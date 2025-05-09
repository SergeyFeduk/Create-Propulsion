package com.deltasf.createpropulsion.optical_sensors;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.Config;
import com.deltasf.createpropulsion.optical_sensors.rendering.BeamRenderData;
import com.mojang.datafixers.util.Pair;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.world.phys.HitResult;

public abstract class AbstractOpticalSensorBlockEntity extends SmartBlockEntity {
    private int currentTick = -1; // -1 to run raycast immediately after placement/load
    protected float raycastDistance = 0;

    @OnlyIn(Dist.CLIENT)
    private BeamRenderData beamRenderData;

    @OnlyIn(Dist.CLIENT)
    public BeamRenderData getClientBeamRenderData() {
        // Initialize lazily on first access on the client
        if (this.beamRenderData == null) {
            this.beamRenderData = new BeamRenderData();
        }
        return this.beamRenderData;
    }

    public float getRaycastDistance() {
        return raycastDistance;
    }

    protected AbstractOpticalSensorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    //Abstract methods

    @Override
    public abstract void addBehaviours(List<BlockEntityBehaviour> behaviours);

    public abstract float getZAxisOffset();

    protected abstract float getMaxRaycastDistance();

    protected abstract void updateRedstoneSignal(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockPos pos, int rawNewPower, @Nullable BlockPos hitBlockPos);

    @Override
    public void tick() {
        super.tick();
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        currentTick++;
        if (currentTick % Config.OPTICAL_SENSOR_TICKS_PER_UPDATE.get() != 0) return;

        // Reset tick counter to prevent overflow
        if (currentTick >= Config.OPTICAL_SENSOR_TICKS_PER_UPDATE.get()) {
            currentTick = 0;
        }

        performRaycast(level);
    }
    
    protected Vec3 getStartingPoint(Vec3 directionVec) {
        Vec3 blockCenter = Vec3.atLowerCornerWithOffset(worldPosition, 0.5, 0.5, 0.5);
        Vec3 offset = directionVec.multiply(getZAxisOffset(), getZAxisOffset(), getZAxisOffset());
        return blockCenter.add(offset);
    }

    @SuppressWarnings("null")
    private void performRaycast(@Nonnull Level level) {
        BlockState state = this.getBlockState();
        BlockPos currentBlockPos = this.getBlockPos();

        Direction facingDirection = state.getValue(AbstractOpticalSensorBlock.FACING);
        Vec3 localDirectionVector = new Vec3(facingDirection.step());

        float effectiveMaxDistance = getMaxRaycastDistance();

        Pair<Vec3, Vec3> raycastPositions = calculateRaycastPositions(currentBlockPos, localDirectionVector, effectiveMaxDistance);
        Vec3 worldFrom = raycastPositions.getFirst();
        Vec3 worldTo = raycastPositions.getSecond();

        // Perform raycast using world coordinates
        ClipContext.Fluid clipFluid = Config.OPTICAL_SENSOR_CLIP_FLUID.get() ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE;
        ClipContext context = new ClipContext(worldFrom, worldTo, ClipContext.Block.COLLIDER, clipFluid, null);
        BlockHitResult hit = level.clip(context);

        // Calculate power based on world distance
        int rawNewPower = 0;
        float distance = effectiveMaxDistance;
        BlockPos hitBlockPos = null;

        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 hitPos = hit.getLocation();
            hitBlockPos = hit.getBlockPos();

            distance = (float)worldFrom.distanceTo(hitPos);
            distance = Math.min(distance, effectiveMaxDistance);

            float invDistancePercent = 1.0f - (distance / effectiveMaxDistance);
            rawNewPower = (int)Math.round(org.joml.Math.lerp(0, 15, invDistancePercent));
        }

        updateRaycastDistance(level, state, distance);

        updateRedstoneSignal(level, state, currentBlockPos, rawNewPower, hitBlockPos);
    }

    private void updateRaycastDistance(@Nonnull Level level, @Nonnull BlockState state, float distance) {
        if (Math.abs(this.raycastDistance - distance) > 0.01f) {
            this.raycastDistance = distance;
            setChanged();
            if (!level.isClientSide()) {
                level.sendBlockUpdated(this.worldPosition, state, state, 3);
            }
        }
    }

    private Pair<Vec3, Vec3> calculateRaycastPositions(BlockPos localBlockPos, Vec3 localDirectionVector, float maxRaycastDistance) {
        Level level = getLevel();

        Vec3 localFromCenter = getStartingPoint(localDirectionVector);
        Vec3 localDisplacement = localDirectionVector.scale(maxRaycastDistance);

        Vec3 worldFrom;
        Vec3 worldDisplacement;

        boolean onShip = VSGameUtilsKt.isBlockInShipyard(level, localBlockPos);

        if (onShip) {
            LoadedShip ship = VSGameUtilsKt.getShipObjectManagingPos(level, localBlockPos);
            if (ship != null && ship.getTransform() != null) {
                worldFrom = VSGameUtilsKt.toWorldCoordinates(ship, localFromCenter);

                Quaterniondc shipRotation = ship.getTransform().getShipToWorldRotation();
                Vector3d rotatedDisplacementJOML = new Vector3d();
                shipRotation.transform(localDisplacement.x, localDisplacement.y, localDisplacement.z, rotatedDisplacementJOML);
                worldDisplacement = new Vec3(rotatedDisplacementJOML.x, rotatedDisplacementJOML.y, rotatedDisplacementJOML.z);
            } else {
                 worldFrom = localFromCenter;
                 worldDisplacement = localDisplacement;
            }
        } else {
            worldFrom = localFromCenter;
            worldDisplacement = localDisplacement;
        }

        Vec3 worldTo = worldFrom.add(worldDisplacement);

        return new Pair<>(worldFrom, worldTo);
    }

    // Networking and nbt

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        write(tag, true);
        return tag;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        read(tag, true);
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putFloat("raycastDistance", this.raycastDistance);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);

        if (tag.contains("raycastDistance", CompoundTag.TAG_FLOAT)) {
            this.raycastDistance = tag.getFloat("raycastDistance");
        } else {
            this.raycastDistance = getMaxRaycastDistance();
        }
    }
}
