package com.deltasf.createpropulsion.heat.burners.liquid;

import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.deltasf.createpropulsion.heat.HeatMapper;
import com.deltasf.createpropulsion.heat.IHeatConsumer;
import com.deltasf.createpropulsion.heat.IHeatSource;
import com.deltasf.createpropulsion.heat.HeatMapper.HeatLevelString;
import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlock;
import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlockEntity;
import com.deltasf.createpropulsion.heat.burners.BurnerDamager;
import com.deltasf.createpropulsion.registries.PropulsionCapabilities;
import com.deltasf.createpropulsion.thruster.FluidThrusterProperties;
import com.deltasf.createpropulsion.thruster.ThrusterFuelManager;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class LiquidBurnerBlockEntity extends AbstractBurnerBlockEntity {
    private HeatLevelString heatLevelName = HeatLevelString.COLD;
    private boolean isPowered = false;
    private BurnerDamager damager;

    private int burnTime = 0;

    protected SmartFluidTankBehaviour tank;
    private final Map<Direction, LazyOptional<IFluidHandler>> fluidCaps = new IdentityHashMap<>();

    private static final float MAX_HEAT = 600.0f;
    private static final float PASSIVE_LOSS_PER_TICK = 0.05f;
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

    protected Direction.Axis getPipeAxis() {
        if (getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)) {
            return getBlockState().getValue(HorizontalDirectionalBlock.FACING).getAxis();
        }
        return Direction.Axis.X;
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

    public float getHeatPerTick() { return 2; }

    @SuppressWarnings("null")
    public void updatePoweredState() {
        if (level == null || level.isClientSide()) return;
        boolean currentlyPowered = level.getBestNeighborSignal(worldPosition) > 0;
        if (this.isPowered != currentlyPowered) {
            this.isPowered = currentlyPowered;
            notifyUpdate();
        }
    }

    @SuppressWarnings("null")
    @Override
    public void tick() {
        super.tick();

        if (level.isClientSide()) return;

        float heatGeneration = (burnTime > 0) ? getHeatPerTick() : 0;
        if (burnTime > 0) burnTime--;

        float heatConsumedLastTick = offerHeatToConsumer();
        float passiveLoss = 0;
        if (heatConsumedLastTick == 0) {
            passiveLoss = heatSource.getCapability().map(cap -> cap.getHeatStored() > 0 ? PASSIVE_LOSS_PER_TICK : 0f).orElse(0f);
        }

        float netHeatChange = heatGeneration - passiveLoss - heatConsumedLastTick;
        heatSource.getCapability().ifPresent(cap -> {
            if (netHeatChange > 0) cap.generateHeat(netHeatChange);
            else cap.extractHeat(Math.abs(netHeatChange), false);
        });

        boolean refueled = false;
        if (burnTime <= 0) {
            if (needsRefuel()) {
                refueled = tryConsumeFuel();
            }
        }

        if (refueled) {
            notifyUpdate();
        }

        updateBlockState();
        updateHeatLevelName();
    }

    private boolean tryConsumeFuel() {
        if (tank.isEmpty()) return false;

        FluidStack fluidInTank = tank.getPrimaryHandler().getFluidInTank(0);
        if (fluidInTank.getAmount() < FUEL_CONSUMPTION_MB) return false;
        FluidThrusterProperties fuelProperties = ThrusterFuelManager.getProperties(fluidInTank.getFluid());
        if (fuelProperties == null) return false;

        float multiplier = fuelProperties.consumptionMultiplier;
        if (multiplier < 1) multiplier = 1;
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
    private boolean needsRefuel() {
        if (isPowered) {
            return true;
        }

        if (level == null) return false;
        BlockEntity beAbove = level.getBlockEntity(worldPosition.above());
        if (beAbove == null) return false;

        return beAbove.getCapability(PropulsionCapabilities.HEAT_CONSUMER, Direction.DOWN)
            .cast()
            .filter(c -> c instanceof IHeatConsumer)
            .map(c -> (IHeatConsumer) c).map(consumer -> {
                if (!consumer.isActive()) {
                    return false;
                }

                float thresholdPercent = consumer.getOperatingThreshold();
                float thresholdInHU = MAX_HEAT * thresholdPercent;

                float currentHeat = heatSource.getCapability().map(IHeatSource::getHeatStored).orElse(0f);
                float consumptionNextTick = consumer.consumeHeat(currentHeat, true);
                float heatNextTick = currentHeat - consumptionNextTick - PASSIVE_LOSS_PER_TICK;

                return heatNextTick < thresholdInHU;
            }).orElse(false);
    }

    @SuppressWarnings("null")
    private float offerHeatToConsumer() {
        if (level == null) return 0f;
        BlockEntity beAbove = level.getBlockEntity(worldPosition.above());
        if (beAbove == null) return 0f;
        
        return beAbove.getCapability(PropulsionCapabilities.HEAT_CONSUMER, Direction.DOWN)
            .cast()
            .filter(c -> c instanceof IHeatConsumer) 
            .map(c -> (IHeatConsumer) c).map(consumer -> {
                float availableHeat = heatSource.getCapability().map(IHeatSource::getHeatStored).orElse(0f);
                if (availableHeat <= 0) return 0f;

                float thresholdPercent = consumer.getOperatingThreshold();
                float thresholdInHU = MAX_HEAT * thresholdPercent;

                if (consumer.isActive() && availableHeat >= thresholdInHU) {
                    return consumer.consumeHeat(availableHeat, false);
                }

                return 0f;
            }).orElse(0f);
    }

    @SuppressWarnings("null")
    private void updateBlockState() {
        HeatLevel currentHeatLevel = calculateHeatLevel();

        BlockState currentState = getBlockState();
        if (currentState.getValue(AbstractBurnerBlock.HEAT) != currentHeatLevel) {
            level.setBlock(worldPosition, currentState.setValue(AbstractBurnerBlock.HEAT, currentHeatLevel), 3);
        }
    }

    private void updateHeatLevelName() {
        HeatLevelString previousName = heatLevelName;
        float availableHeat = heatSource.getCapability().map(IHeatSource::getHeatStored).orElse(0f);
        float percentage = availableHeat / getBaseHeatCapacity();

        heatLevelName = HeatMapper.getHeatString(percentage);
        if (previousName != heatLevelName) {
            notifyUpdate();
        }
    }

    private HeatLevel calculateHeatLevel() {
        return heatSource.getCapability().map(cap -> {
            if (cap.getHeatStored() == 0) return HeatLevel.NONE;
            float percentage = cap.getHeatStored() / cap.getMaxHeatStored();
            return HeatMapper.getHeatLevel(percentage);
        }).orElse(HeatLevel.NONE);
    }


    @Override
    protected Direction getHeatCapSide() { return Direction.UP; }

    @Override
    protected float getBaseHeatCapacity() { return MAX_HEAT; }

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

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ChatFormatting color = null;
        String key = null;

        switch (heatLevelName) {
            case COLD:
                color = ChatFormatting.BLUE;
                key = "gui.goggles.burner.heat.cold";
                break;
            case WARM:
                color = ChatFormatting.GOLD;
                key = "gui.goggles.burner.heat.warm";
                break;
            case HOT:
                color = ChatFormatting.GOLD;
                key = "gui.goggles.burner.heat.hot";
                break;
            case SEARING:
                color = ChatFormatting.RED;
                key = "gui.goggles.burner.heat.searing";
                break;
            default:
                color = ChatFormatting.BLUE;
                break;
        }

        //Heat level
        CreateLang.builder().add(CreateLang.translate("gui.goggles.burner.status")).text(": ").add(CreateLang.translate(key).style(color)).forGoggles(tooltip);

        //Thermostat on/off
        CreateLang.builder()
            .add(CreateLang.translate("gui.goggles.burner.thermostat"))
            .text(": ")
            .add(CreateLang.translate(!isPowered ? "gui.goggles.burner.thermostat.on" : "gui.goggles.burner.thermostat.off")
                .style(!isPowered ? ChatFormatting.GREEN : ChatFormatting.RED))
            .forGoggles(tooltip);

        //Fuel
        containedFluidTooltip(tooltip, isPlayerSneaking, tank.getCapability().cast());
        return true;
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putString("heatLevelName", heatLevelName.name());
        tag.putBoolean("isPowered", isPowered);
        tag.putInt("burnTime", burnTime);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        heatLevelName = HeatLevelString.valueOf(tag.getString("heatLevelName"));
        isPowered = tag.getBoolean("isPowered");
        burnTime = tag.getInt("burnTime");
    }
}