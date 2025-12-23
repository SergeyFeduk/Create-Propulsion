package com.deltasf.createpropulsion.impact_sensor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;


public class ImpactSensorBlockEntity extends BlockEntity {
    private final ImpactSensorData sensorData;
    private boolean initialized = false;
    //private double currentOutput = 0.0;

    public ImpactSensorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.sensorData = new ImpactSensorData();
    }

    public void tick() {
        Level level = getLevel();
        if (level == null || level.isClientSide) return;

        if (!initialized) {
            initializeAttachment();
            initialized = true;
        }

        double impact = sensorData.getAndResetSignal();
        if (impact > 0.0) { 
            //currentOutput = impact;
            System.out.println("Impact: " + impact);
        } else {
            //currentOutput = 0.0;
        }
    }

    private void initializeAttachment() {
        if (level instanceof ServerLevel serverLevel) {
            LoadedServerShip ship = VSGameUtilsKt.getLoadedShipManagingPos(serverLevel, worldPosition);
            if (ship != null) {
                ImpactSensorAttachment attachment = ImpactSensorAttachment.getOrCreate(ship);
                if (attachment != null) {
                    attachment.addSensor(worldPosition, sensorData);
                }
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel serverLevel) {
            ImpactSensorAttachment attachment = ImpactSensorAttachment.get(serverLevel, worldPosition);
            if (attachment != null) {
                attachment.removeSensor(worldPosition);
            }
        }
    }
}
