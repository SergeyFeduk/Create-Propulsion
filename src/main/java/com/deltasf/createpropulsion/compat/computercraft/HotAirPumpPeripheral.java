package com.deltasf.createpropulsion.compat.computercraft;

import java.util.Optional;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.hot_air.HotAirSolver;
import com.deltasf.createpropulsion.balloons.injectors.hot_air_pump.HotAirPumpBlockEntity;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;

import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.level.Level;

public class HotAirPumpPeripheral extends SyncedPeripheral<HotAirPumpBlockEntity> {
    public HotAirPumpPeripheral(HotAirPumpBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public final String getType() {
        return "hot_air_pump";
    }

    @LuaFunction
    public final double getInjectionAmount() {
        return blockEntity.getInjectionAmount();
    }

    @LuaFunction(mainThread = true)
    public final double getHotAir() {
        Balloon balloon = getBalloon();
        return balloon != null ? balloon.hotAir : 0.0;
    }

    @LuaFunction(mainThread = true)
    public final double getBalloonVolume() {
        Balloon balloon = getBalloon();
        return balloon != null ? balloon.getVolumeSize() : 0.0;
    }

    @LuaFunction(mainThread = true)
    public final double predictSteadyHotAir(Optional<Double> tolerance) {
        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide) return 0.0;

        Ship ship = VSGameUtilsKt.getShipManagingPos(level, blockEntity.getBlockPos());
        if (!(ship instanceof ServerShip serverShip)) return 0.0;

        BalloonRegistry registry = BalloonShipRegistry.forShip(ship.getId(), level);
        Balloon balloon = registry.getBalloonOf(blockEntity.getId());
        if (balloon == null) return 0.0;

        return HotAirSolver.predictSteadyHotAir(level, balloon, null, registry, serverShip, tolerance.orElse(0.01));
    }

    private Balloon getBalloon() {
        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide) return null;

        Ship ship = VSGameUtilsKt.getShipManagingPos(level, blockEntity.getBlockPos());
        if (ship == null) return null;

        return BalloonShipRegistry.forShip(ship.getId(), level).getBalloonOf(blockEntity.getId());
    }
}
