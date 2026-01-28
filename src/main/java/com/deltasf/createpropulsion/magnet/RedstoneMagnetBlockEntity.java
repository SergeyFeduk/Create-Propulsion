package com.deltasf.createpropulsion.magnet;

import java.util.List;
import java.util.UUID;

import org.joml.Vector3i;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.deltasf.createpropulsion.compat.PropulsionCompatibility;
import com.deltasf.createpropulsion.compat.computercraft.ComputerBehaviour;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public class RedstoneMagnetBlockEntity extends SmartBlockEntity {
    public RedstoneMagnetBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    private UUID magnetId;
    private boolean needsUpdate = true;
    private int power = 0;

    //CC peripheral
    public AbstractComputerBehaviour computerBehaviour;
    public boolean overridePower = false;
    public int overridenPower;

    private boolean updatedAttachment = false;

    public void setPower(int power) {
        if (this.power == power) return;
        this.power = power;
        if (!overridePower) {
            this.scheduleUpdate();
        }
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
        int effectivePower = getEffectivePower();

        if (effectivePower > 0) {
            long currentShipId = -1;
            Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
            if (ship != null) {
                currentShipId = ship.getId();
                MagnetForceAttachment.ensureAttachmentExists(level, worldPosition);
            }

            Vector3i currentDipoleDir = VectorConversionsMCKt.toJOML(currentState.getValue(RedstoneMagnetBlock.FACING).getNormal());
            MagnetData magnetData = MagnetRegistry.forLevel(level).getOrCreateMagnet(this.magnetId, worldPosition, currentShipId, currentDipoleDir, effectivePower);
            magnetData.cancelRemoval();
            magnetData.update(worldPosition, currentShipId, currentDipoleDir, effectivePower);
            MagnetRegistry.forLevel(level).updateMagnetPosition(magnetData);

        } else {
            MagnetRegistry.forLevel(level).scheduleRemoval(this.magnetId);
        }
    }

    public void scheduleUpdate() {
        this.needsUpdate = true;
    }

    public void onBlockBroken() {
        if (this.magnetId != null) {
            MagnetRegistry.forLevel(level).scheduleRemoval(this.magnetId);
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

        //TODO: Uhh, this should already be handled by ForgeEvents::onServerStart
        if (!updatedAttachment) {
            //update attachment level reference
            var serverShip = VSGameUtilsKt.getLoadedShipManagingPos((ServerLevel)level, worldPosition);
            if (serverShip != null) {
                var magnetAttachment  = serverShip.getAttachment(MagnetForceAttachment.class);
                if (magnetAttachment != null && magnetAttachment.level == null) {
                    magnetAttachment.level = level;
                }
                updatedAttachment = true;
            }
        }

        MagnetData data = MagnetRegistry.forLevel(level).getMagnet(this.magnetId);
        if (data != null && data.shipId != -1) {
            var serverShip = VSGameUtilsKt.getLoadedShipManagingPos((ServerLevel)level, worldPosition);
            if (serverShip == null) {
                MagnetRegistry.forLevel(level).removeAllMagnetsForShip(data.shipId); //Technically we could just remove only this magnet but who cares
            } else {
                MagnetRegistry.forLevel(level).updateMagnetPosition(data);
            }
        }
    }

    @Override
    public void onLoad() { 
        scheduleUpdate();
    }

    public int getEffectivePower() {
        if (PropulsionCompatibility.CC_ACTIVE && overridePower) {
            return overridenPower;
        }
        return this.power;
    }

    @SuppressWarnings("null")
    public void updateBlockstateFromPower() {
        if (level == null || level.isClientSide) return;
        
        BlockState currentState = getBlockState();
        boolean shouldBePowered = getEffectivePower() > 0;
        
        if (currentState.getValue(RedstoneMagnetBlock.POWERED) != shouldBePowered) {
            level.setBlock(getBlockPos(), currentState.setValue(RedstoneMagnetBlock.POWERED, shouldBePowered), 2);
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        if (PropulsionCompatibility.CC_ACTIVE) {
            behaviours.add(computerBehaviour = new ComputerBehaviour(this));
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (PropulsionCompatibility.CC_ACTIVE && computerBehaviour.isPeripheralCap(cap)) {
            return computerBehaviour.getPeripheralCapability();
        }
        return super.getCapability(cap, side);
    }

    //NBT

    @Override
    protected void read(CompoundTag tag, boolean isClient) {
        super.read(tag, isClient);
        if (tag.hasUUID("MagnetId")) {
            this.magnetId = tag.getUUID("MagnetId");
        }
        this.power = tag.getInt("Power");
        if (PropulsionCompatibility.CC_ACTIVE) {
            this.overridePower = tag.getBoolean("overridePower");
            this.overridenPower = tag.getInt("overridenPower");
        }    
    }

    @Override
    protected void write(CompoundTag tag, boolean isClient) {
        super.write(tag, isClient);
        if (this.magnetId == null) {
            this.magnetId = UUID.randomUUID();
        }
        tag.putUUID("MagnetId", this.magnetId);
        tag.putInt("Power", this.power);
        if (PropulsionCompatibility.CC_ACTIVE) {
            tag.putBoolean("overridePower", this.overridePower);
            tag.putInt("overridenPower", this.overridenPower);
        }    
    }
}
