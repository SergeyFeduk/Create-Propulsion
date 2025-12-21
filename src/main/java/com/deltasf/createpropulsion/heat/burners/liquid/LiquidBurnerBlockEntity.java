package com.deltasf.createpropulsion.heat.burners.liquid;

import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlock;
import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlockEntity;
import com.deltasf.createpropulsion.heat.burners.BurnerDamager;
import com.deltasf.createpropulsion.registries.PropulsionCapabilities;
import com.deltasf.createpropulsion.thruster.FluidThrusterProperties;
import com.deltasf.createpropulsion.thruster.ThrusterFuelManager;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class LiquidBurnerBlockEntity extends AbstractBurnerBlockEntity {
    protected SmartFluidTankBehaviour tank;
    private BurnerDamager damager;
    private int burnTime = 0;

    public float fanAngle = 0;
    public float lastRenderTime = -1;

    private final Map<Direction, LazyOptional<IFluidHandler>> fluidCaps = new IdentityHashMap<>();

    private static final float MAX_HEAT = 600.0f;
    private static final int FUEL_CONSUMPTION_MB = 2;
    private static final int BASE_BURN_DURATION = 20;

    public LiquidBurnerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        
        damager = new BurnerDamager(this);
        behaviours.add(damager);

        tank = SmartFluidTankBehaviour.single(this, 200);
        behaviours.add(tank);
    }

    public int getBurnTime() {
        return burnTime;
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    private boolean hasEnoughFuel() {
        if (tank.isEmpty()) return false;
        FluidStack fluid = tank.getPrimaryHandler().getFluidInTank(0);
        if (fluid.getAmount() < FUEL_CONSUMPTION_MB) return false;
        return ThrusterFuelManager.getProperties(fluid.getFluid()) != null;
    }

    private boolean isConnectedToConsumer() {
        Level level = getLevel();
        if (level == null) return false;
        BlockPos posAbove = worldPosition.above();
        BlockEntity blockEntityAbove = level.getBlockEntity(posAbove);
        if (blockEntityAbove == null) return false;
        return blockEntityAbove.getCapability(PropulsionCapabilities.HEAT_CONSUMER, Direction.DOWN).isPresent();
    }

    public boolean isFanSpinning() {
        if (isBurning()) return true;
        if (hasEnoughFuel() && isConnectedToConsumer()) return true;
        return false;
    }

    @SuppressWarnings("null")
    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide()) {
            if (burnTime > 0) burnTime--;
            tickParticles();
            return;
        }

        float heatGeneration = (burnTime > 0) ? getHeatPerTick() : 0;
        if (burnTime > 0) burnTime--;

        tickHeatPhysics(heatGeneration);

        boolean refueled = false;
        if (burnTime <= 0) {
            if (shouldThermostatBurn()) {
                refueled = tryConsumeFuel();
            }
        }

        if (refueled) notifyUpdate();

        updateBlockState();
        updateHeatLevelName();
    }

    private void tickParticles() {
        Level level = getLevel();
        if (level == null) return;
        
        //Smoke
        if (isBurning() && level.getGameTime() % 2 == 0) {
            final float PIPE_OFFSET = 2.5f / 16.0f;
            final float Y_OFFSET = 0.3f;
            final Vec3 EXHAUST_VELOCITY = new Vec3(0.01, 0.05, 0);

            //Alternate pipes
            boolean isLeft = level.getGameTime() % 4 == 0;
            spawnParticleEffect(
                new Vec3(0.6, Y_OFFSET, isLeft ? PIPE_OFFSET : -PIPE_OFFSET), 
                EXHAUST_VELOCITY,
                ParticleTypes.SMOKE
            );
        }

        //TODO: Air sucked in
        //Likely will need a separate particle type with behavior similar to airflow particle ones
    }

    private void spawnParticleEffect(Vec3 localOffset, Vec3 localVelocity, ParticleOptions particle) {
        Level level = getLevel();
        if (level == null) return;
        Direction facing = getBlockState().getValue(AbstractBurnerBlock.FACING);
        float yRot = -facing.toYRot();
        
        Vec3 offset = VecHelper.rotate(localOffset, yRot, Direction.Axis.Y);
        Vec3 velocity = VecHelper.rotate(localVelocity, yRot, Direction.Axis.Y);
        Vec3 spawnPos = VecHelper.getCenterOf(worldPosition).add(offset);

        level.addParticle(particle, spawnPos.x, spawnPos.y, spawnPos.z, velocity.x, velocity.y, velocity.z);
    }

    private boolean tryConsumeFuel() {
        if (tank.isEmpty()) return false;

        FluidStack fluidInTank = tank.getPrimaryHandler().getFluidInTank(0);
        if (fluidInTank.getAmount() < FUEL_CONSUMPTION_MB) return false;

        FluidThrusterProperties fuelProperties = ThrusterFuelManager.getProperties(fluidInTank.getFluid());
        if (fuelProperties == null) return false;

        float multiplier = fuelProperties.consumptionMultiplier;
        if (multiplier <= 0) multiplier = 1;
        
        int duration = (int) (BASE_BURN_DURATION / multiplier);
        if (duration < 1) duration = 1;

        FluidStack drained = tank.getPrimaryHandler().drain(FUEL_CONSUMPTION_MB, IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() == FUEL_CONSUMPTION_MB) {
            this.burnTime = duration;
            return true;
        }
        return false;
    }

    @SuppressWarnings("null")
    private void updateBlockState() {
        HeatLevel currentHeatLevel = calculateHeatLevel();
        BlockState currentState = getBlockState();
        if (currentState.getValue(AbstractBurnerBlock.HEAT) != currentHeatLevel) {
            level.setBlock(worldPosition, currentState.setValue(AbstractBurnerBlock.HEAT, currentHeatLevel), 3);
        }
    }

    public float getHeatPerTick() { return 2; }

    @Override
    protected Direction getHeatCapSide() { return Direction.UP; }
    @Override
    protected float getBaseHeatCapacity() { return MAX_HEAT; }

    @Override
    protected void addSpecificTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        containedFluidTooltip(tooltip, isPlayerSneaking, tank.getCapability().cast());
    }

    //Pipes & caps

    protected Direction.Axis getPipeAxis() {
        if (getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)) {
            return getBlockState().getValue(HorizontalDirectionalBlock.FACING).getAxis();
        }
        return Direction.Axis.X;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER && side != null) {
            if (side.getAxis() == getPipeAxis()) {
                return fluidCaps.computeIfAbsent(side, s -> LazyOptional.of(() -> new PassthroughFluidHandler(this, s))).cast();
            }
        }
        return super.getCapability(cap, side);
    }

    public void updatePipeCapability() {
        fluidCaps.values().forEach(LazyOptional::invalidate);
        fluidCaps.clear();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        fluidCaps.values().forEach(LazyOptional::invalidate);
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putInt("burnTime", burnTime);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        burnTime = tag.getInt("burnTime");
    }
}