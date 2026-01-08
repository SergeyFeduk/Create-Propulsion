package com.deltasf.createpropulsion.balloons.injectors.hot_air_pump;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.injectors.AirInjectorObstructionBehaviour;
import com.deltasf.createpropulsion.balloons.injectors.HotAirInjectorBehaviour;
import com.deltasf.createpropulsion.balloons.injectors.IHotAirInjector;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.deltasf.createpropulsion.heat.IHeatConsumer;
import com.deltasf.createpropulsion.registries.PropulsionCapabilities;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;


public class HotAirPumpBlockEntity extends KineticBlockEntity implements IHotAirInjector, IHeatConsumer {
    //Behaviours
    private HotAirInjectorBehaviour injectorBehaviour;
    private AirInjectorObstructionBehaviour obstructionBehaviour;

    private final LazyOptional<IHeatConsumer> heatConsumerCap;

    public HotAirPumpBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        this.heatConsumerCap = LazyOptional.of(() -> this);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        injectorBehaviour = new HotAirInjectorBehaviour(this);
        behaviours.add(injectorBehaviour);

        obstructionBehaviour = new AirInjectorObstructionBehaviour(this);
        behaviours.add(obstructionBehaviour);
    }

    //TODO: 
    @Override
    public void tick() {
        super.tick();
    }

    // Hot air injector impl

    @Override
    public UUID getId() {
        return injectorBehaviour.getId();
    }

    @Override
    public void attemptScan() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        
        // Ship check
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
        if (ship == null) return;
        
        // Balloon check
        Balloon balloon = BalloonShipRegistry.forShip(ship.getId(), level).getBalloonOf(getId());
        if (balloon != null) return;
        
        // Perform scan via behaviour
        injectorBehaviour.performScan();
    }

    //TODO: 
    @Override
    public double getInjectionAmount() {
        return 0;
    }

    //TODO: 
    @Override
    public void onBalloonLoaded() {
    }

    // IHeatConsumer impl

    //TODO: 
    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public float getOperatingThreshold() {
        return 0.0f;
    }

    @Override
    public float consumeHeat(float maxAvailable, float expectedHeatOutput, boolean simulate) {
        return 0;
    }

    //Caps

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == PropulsionCapabilities.HEAT_CONSUMER && side == Direction.DOWN) {
            return heatConsumerCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        heatConsumerCap.invalidate();
    }
}
