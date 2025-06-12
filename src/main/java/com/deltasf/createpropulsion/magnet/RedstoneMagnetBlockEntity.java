package com.deltasf.createpropulsion.magnet;

import java.util.List;
import java.util.UUID;

import org.joml.Vector3i;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RedstoneMagnetBlockEntity extends SmartBlockEntity {
    public RedstoneMagnetBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    private UUID magnetId;
    private boolean needsUpdate = true;
    private int power = 0;

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    public void setPower(int power) {
        if (this.power == power) return;
        this.power = power;
        this.scheduleUpdate();
        setChanged();
    }

    @SuppressWarnings("null")
    public void updateMagnetState() {
        if (level == null || level.isClientSide) return;

        //Ensure valid UUID
        if (this.magnetId == null) {
            this.magnetId = UUID.randomUUID();
            setChanged();
        }

        BlockState currentState = level.getBlockState(worldPosition);
        //boolean shouldBeActive = currentState.getValue(RedstoneMagnetBlock.POWERED);

        if (this.power > 0) {
            long currentShipId = -1;
            Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
            if (ship != null) {
                currentShipId = ship.getId();
                MagnetForceAttachment.ensureAttachmentExists(level, worldPosition);
            }

            Vector3i currentDipoleDir = VectorConversionsMCKt.toJOML(currentState.getValue(RedstoneMagnetBlock.FACING).getNormal());
            MagnetData magnetData = MagnetRegistry.get().forLevel(level).getOrCreateMagnet(this.magnetId, worldPosition, currentShipId, currentDipoleDir, this.power);
            magnetData.cancelRemoval();
            magnetData.update(worldPosition, currentShipId, currentDipoleDir, this.power);
            MagnetRegistry.get().forLevel(level).updateMagnetPosition(magnetData);

        } else {
            MagnetRegistry.get().forLevel(level).scheduleRemoval(this.magnetId);
        }
    }

    public void scheduleUpdate() {
        this.needsUpdate = true;
    }

    public void onBlockBroken() {
        if (this.magnetId != null) {
            MagnetRegistry.get().forLevel(level).scheduleRemoval(this.magnetId);
        }
    }

    @SuppressWarnings("null")
    @Override
    public void tick() {
        if (level.isClientSide) return;
        
        if (needsUpdate) {
            updateMagnetState();
            needsUpdate = false;
        }

        MagnetData data = MagnetRegistry.get().forLevel(level).getMagnet(this.magnetId);
        if (data != null && data.shipId != -1) {
            MagnetRegistry.get().forLevel(level).updateMagnetPosition(data);
        }
    }

    @Override
    public void onLoad() { 
        scheduleUpdate();
    }

    //NBT

    @Override
    protected void read(CompoundTag tag, boolean isClient) {
        super.read(tag, isClient);
        if (tag.hasUUID("MagnetId")) {
            this.magnetId = tag.getUUID("MagnetId");
        }
        this.power = tag.getInt("Power");
    }

    @Override
    protected void write(CompoundTag tag, boolean isClient) {
        super.write(tag, isClient);
        if (this.magnetId == null) {
            this.magnetId = UUID.randomUUID();
        }
        tag.putUUID("MagnetId", this.magnetId);
        tag.putInt("Power", this.power);
    }
}
