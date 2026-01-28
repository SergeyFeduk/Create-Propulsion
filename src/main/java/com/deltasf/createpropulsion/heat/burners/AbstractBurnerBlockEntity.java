package com.deltasf.createpropulsion.heat.burners;

import java.util.List;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.utility.CreateLang;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.deltasf.createpropulsion.heat.HeatMapper;
import com.deltasf.createpropulsion.heat.HeatSourceBehavior;
import com.deltasf.createpropulsion.heat.IHeatConsumer;
import com.deltasf.createpropulsion.heat.IHeatSource;
import com.deltasf.createpropulsion.heat.HeatMapper.HeatLevelString;
import com.deltasf.createpropulsion.registries.PropulsionCapabilities;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public abstract class AbstractBurnerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    protected HeatSourceBehavior heatSource;
    protected HeatLevelString heatLevelName = HeatLevelString.COLD;
    protected boolean isPowered = false;

    protected static final float PASSIVE_LOSS_PER_TICK = 0.05f;

    public AbstractBurnerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        heatSource = new HeatSourceBehavior(this, getBaseHeatCapacity(), getHeatPerTick());
        behaviours.add(heatSource);
    }

    protected abstract float getBaseHeatCapacity();

    protected abstract Direction getHeatCapSide();

    protected abstract float getHeatPerTick();

    @SuppressWarnings("null")
    public void updatePoweredState() {
        if (level == null || level.isClientSide()) return;
        boolean currentlyPowered = level.getBestNeighborSignal(worldPosition) > 0;
        if (this.isPowered != currentlyPowered) {
            this.isPowered = currentlyPowered;
            notifyUpdate();
        }
    }

    protected void tickHeatPhysics(float heatGeneration) {
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
    }

    @SuppressWarnings("null")
    protected boolean shouldThermostatBurn() {
        if (isPowered) return true; //Redstone override
        if (level == null) return false;
        
        BlockEntity beAbove = level.getBlockEntity(worldPosition.above());
        if (beAbove == null) return false;

        return beAbove.getCapability(PropulsionCapabilities.HEAT_CONSUMER, Direction.DOWN)
            .cast()
            .filter(c -> c instanceof IHeatConsumer)
            .map(c -> (IHeatConsumer) c).map(consumer -> {
                if (!consumer.isActive()) return false;

                float thresholdPercent = consumer.getOperatingThreshold();
                float thresholdInHU = getBaseHeatCapacity() * thresholdPercent;

                float currentHeat = heatSource.getCapability().map(IHeatSource::getHeatStored).orElse(0f);
                float expectedHeatProduction = heatSource.getCapability().map(IHeatSource::getExpectedHeatProduction).orElse(1f);
                //Prediction logic
                float consumptionNextTick = consumer.consumeHeat(currentHeat, expectedHeatProduction, true); 
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
                float thresholdInHU = getBaseHeatCapacity() * thresholdPercent;
                float expectedHeatProduction = heatSource.getCapability().map(IHeatSource::getExpectedHeatProduction).orElse(1f);

                if (consumer.isActive() && availableHeat >= thresholdInHU) {
                    return consumer.consumeHeat(availableHeat, expectedHeatProduction,  false);
                }
                return 0f;
            }).orElse(0f);
    }

    protected void updateHeatLevelName() {
        HeatLevelString previousName = heatLevelName;
        float availableHeat = heatSource.getCapability().map(IHeatSource::getHeatStored).orElse(0f);
        float percentage = availableHeat / getBaseHeatCapacity();

        heatLevelName = HeatMapper.getHeatString(percentage);
        if (previousName != heatLevelName) {
            notifyUpdate();
        }
    }

    protected HeatLevel calculateHeatLevel() {
        return heatSource.getCapability().map(cap -> {
            if (cap.getHeatStored() == 0) return HeatLevel.NONE;
            float percentage = cap.getHeatStored() / cap.getMaxHeatStored();
            return HeatMapper.getHeatLevel(percentage);
        }).orElse(HeatLevel.NONE);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        addHeatInfoToTooltip(tooltip);
        addSpecificTooltip(tooltip, isPlayerSneaking);
        return true;
    }

    protected abstract void addSpecificTooltip(List<Component> tooltip, boolean isPlayerSneaking);

    private void addHeatInfoToTooltip(List<Component> tooltip) {
        ChatFormatting color = null;
        String key = null;

        switch (heatLevelName) {
            case COLD: color = ChatFormatting.BLUE; key = "gui.goggles.burner.heat.cold"; break;
            case WARM: color = ChatFormatting.GOLD; key = "gui.goggles.burner.heat.warm"; break;
            case HOT: color = ChatFormatting.GOLD; key = "gui.goggles.burner.heat.hot"; break;
            case SEARING: color = ChatFormatting.RED; key = "gui.goggles.burner.heat.searing"; break;
            default: color = ChatFormatting.BLUE; break;
        }

        //Heat level
        if (key != null)
            CreateLang.builder().add(CreateLang.translate("gui.goggles.burner.status")).text(": ").add(CreateLang.translate(key).style(color)).forGoggles(tooltip);

        //Thermostat
        CreateLang.builder()
            .add(CreateLang.translate("gui.goggles.burner.thermostat"))
            .text(": ")
            .add(CreateLang.translate(!isPowered ? "gui.goggles.burner.thermostat.on" : "gui.goggles.burner.thermostat.off")
                .style(!isPowered ? ChatFormatting.GREEN : ChatFormatting.RED))
            .forGoggles(tooltip);
    }

    //Caps

    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == PropulsionCapabilities.HEAT_SOURCE && side == getHeatCapSide()) {
            return heatSource.getCapability().cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        heatSource.getCapability().invalidate();
    }

    //NBT

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putString("heatLevelName", heatLevelName.name());
        tag.putBoolean("isPowered", isPowered);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (tag.contains("heatLevelName"))
            heatLevelName = HeatLevelString.valueOf(tag.getString("heatLevelName"));
        isPowered = tag.getBoolean("isPowered");
    }
}
