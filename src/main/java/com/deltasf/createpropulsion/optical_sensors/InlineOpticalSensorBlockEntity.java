package com.deltasf.createpropulsion.optical_sensors;

import java.util.List;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.joml.Math;
import org.joml.Quaterniondc;
import org.joml.Vector3d;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.optical_sensors.rendering.BeamRenderData;
import com.deltasf.createpropulsion.Config;
import com.mojang.datafixers.util.Pair;

public class InlineOpticalSensorBlockEntity extends SmartBlockEntity {
    private int currentTick = -1; // -1 to run raycast immediately after placement
    private float raycastDistance = getMaxRaycastDistanceNormalized();

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

    public InlineOpticalSensorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn == null ? CreatePropulsion.INLINE_OPTICAL_SENSOR_BLOCK_ENTITY.get() : typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    //Raycast getters
    public float getRaycastDistance() {
        return raycastDistance;
    }

    public float getZAxisOffset(){
        return -0.125f;
    }

    protected float getMaxRaycastDistance(){
        return Config.INLINE_OPTICAL_SENSOR_MAX_DISTANCE.get();
    }

    //Adjusts max raycast distance to account for starting point displacement along relative Z axis with a rounding of 0.5 blocks
    private float getMaxRaycastDistanceNormalized() {
        return getMaxRaycastDistance() - getZAxisOffset() % 0.5f;
    }

    private Vec3 getStartingPoint(Vec3 directionVec) {
        Vec3 base = directionVec.multiply(getZAxisOffset(), getZAxisOffset(), getZAxisOffset());
        return base.add(Vec3.atLowerCornerWithOffset(worldPosition, 0.5, 0.5, 0.5));
    }

    @Override
    public void tick(){
        super.tick();
        Level level = this.getLevel(); //Antinullwarnmagic
        if (level == null || level.isClientSide()) return;
        currentTick++;
        if (currentTick % Config.OPTICAL_SENSOR_TICKS_PER_UPDATE.get() != 0) return;
        //Raycast
        performRaycast(level);
    }

    @SuppressWarnings("null") //Because VSCode
    private void performRaycast(Level level) {
        BlockState state = this.getBlockState();
        BlockPos shipLocalPos = this.getBlockPos();
        BlockPos hitBlockPos = shipLocalPos;
        //Perhaps, this is hell
        float maxRaycastDistance = getMaxRaycastDistanceNormalized();
        Pair<Vec3, Vec3> raycastPositions = calculateRaycastPositions(shipLocalPos, state.getValue(InlineOpticalSensorBlock.FACING), maxRaycastDistance); 

        //Perform raycast using world coordinates
        ClipContext.Fluid clipFluid = Config.OPTICAL_SENSOR_CLIP_FLUID.get() ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE;
        ClipContext context = new ClipContext(raycastPositions.getFirst(), raycastPositions.getSecond(), ClipContext.Block.COLLIDER, clipFluid, null);
        BlockHitResult hit = level.clip(context);

        // Calculate power based on world distance
        int newPower = 0;
        float distance = maxRaycastDistance;
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 hitPos = hit.getLocation(); // This is world-space
            hitBlockPos = hit.getBlockPos();
            distance = (float)raycastPositions.getFirst().distanceTo(hitPos);
            distance = Math.min(distance, maxRaycastDistance); //Just in case
            //Calculate power
            float invDistancePercent = 1.0f - (distance / maxRaycastDistance);
            newPower = (int)Math.round(Math.lerp(0, 15, invDistancePercent));
        } 
        updateRaycastDistance(level, state, distance);
        updateRedstoneSignal(level, state, shipLocalPos, newPower, hitBlockPos);
    }

    private void updateRaycastDistance(Level level, BlockState state, float distance) {
        this.raycastDistance = distance;
        if (!level.isClientSide()) {
            setChanged();
            level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    private Pair<Vec3, Vec3> calculateRaycastPositions(BlockPos shipLocalPos, Direction facingDirection, float maxRaycastDistance) {
        Vec3 localDirectionVector = new Vec3(facingDirection.step());
        Vec3 localFromCenter = getStartingPoint(localDirectionVector);
        Vec3 localDisplacement = localDirectionVector.scale(maxRaycastDistance);
        //Resulting vectors
        Vec3 worldFrom;
        Vec3 worldDisplacement;
        //Ship check
        LoadedShip ship = null;
        //Technically we can cache onShip and ship and recalculate them only when block position changes
        boolean onShip = VSGameUtilsKt.isBlockInShipyard(level, shipLocalPos);
        if (onShip) {
            ship = VSGameUtilsKt.getShipObjectManagingPos(level, shipLocalPos);
        }
        if (onShip && ship != null && ship.getTransform() != null) {
            //Positioned on VS Ship
            worldFrom = VSGameUtilsKt.toWorldCoordinates(ship, localFromCenter);
            Quaterniondc shipRotation = ship.getTransform().getShipToWorldRotation();
            Vector3d rotatedDisplacementJOML = new Vector3d();
            shipRotation.transform(localDisplacement.x, localDisplacement.y, localDisplacement.z, rotatedDisplacementJOML);
            worldDisplacement = new Vec3(rotatedDisplacementJOML.x, rotatedDisplacementJOML.y, rotatedDisplacementJOML.z);
        } else {
            //Positioned in world
            worldFrom = localFromCenter;
            worldDisplacement = localDisplacement;
        }
        //Target position offset
        Vec3 worldTo = worldFrom.add(worldDisplacement);
        return new Pair<>(worldFrom, worldTo);
    }

    protected void updateRedstoneSignal(Level level, BlockState state, BlockPos pos, int newPower, BlockPos hitBlockPos) {
        int oldPower = state.getValue(InlineOpticalSensorBlock.POWER);
        if (oldPower != newPower) {
            BlockState updatedState = state.setValue(InlineOpticalSensorBlock.POWER, newPower).setValue(InlineOpticalSensorBlock.POWERED, newPower > 0);
            level.setBlock(pos, updatedState, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
            //Update block behind
            Direction facingDir = state.getValue(InlineOpticalSensorBlock.FACING).getOpposite();
            level.updateNeighborsAt(pos.relative(facingDir), state.getBlock());
        }
    }

    //Networking
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
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
        load(tag);
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket){
        super.write(tag, clientPacket);
        tag.putFloat("raycastDistance", this.raycastDistance);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket){
        super.read(tag, clientPacket);
        if (tag.contains("raycastDistance", CompoundTag.TAG_FLOAT)) {
            this.raycastDistance = tag.getFloat("raycastDistance");
        } else {
            this.raycastDistance = getMaxRaycastDistanceNormalized();
        }
    }
}
