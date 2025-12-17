package com.deltasf.createpropulsion.heat.burners.solid;

import java.util.List;

import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.LangBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.deltasf.createpropulsion.heat.HeatMapper;
import com.deltasf.createpropulsion.heat.IHeatConsumer;
import com.deltasf.createpropulsion.heat.IHeatSource;
import com.deltasf.createpropulsion.heat.HeatMapper.HeatLevelString;
import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlock;
import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlockEntity;
import com.deltasf.createpropulsion.heat.burners.BurnerDamager;
import com.deltasf.createpropulsion.registries.PropulsionCapabilities;
import com.deltasf.createpropulsion.utility.burners.BurnerFuelBehaviour;
import com.deltasf.createpropulsion.utility.burners.IBurner;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

public class SolidBurnerBlockEntity extends AbstractBurnerBlockEntity implements IBurner {
    private BurnerFuelBehaviour fuelInventory;
    private int burnTime = 0;
    private HeatLevelString heatLevelName = HeatLevelString.COLD;
    private boolean isPowered = false;
    private BurnerDamager damager;

    private static final float MAX_HEAT = 400.0f;
    private static final float PASSIVE_LOSS_PER_TICK = 0.05f;

    public SolidBurnerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void setBurnTime(int burnTime) {
        this.burnTime = burnTime;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        fuelInventory = new BurnerFuelBehaviour(this, () -> {});
        behaviours.add(fuelInventory);
        
        damager = new BurnerDamager(this);
        behaviours.add(damager);
    }

    public ItemStack getFuelStack() {
        return fuelInventory.fuelStack;
    }

    public float getHeatPerTick() { return 1; }

    @SuppressWarnings("null")
    public void updatePoweredState() {
        if (level == null || level.isClientSide()) {
            return;
        }
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

        //Calculate rates
        float heatGeneration = (burnTime > 0) ? getHeatPerTick() : 0;
        if (burnTime > 0) burnTime--;

        float heatConsumedLastTick = offerHeatToConsumer();
        float passiveLoss = 0;
        if (heatConsumedLastTick == 0) {
            heatConsumedLastTick = heatSource.getCapability().map(cap -> cap.getHeatStored() > 0 ? PASSIVE_LOSS_PER_TICK : 0f).orElse(0f);
        }

        //Apply heat changes
        float netHeatChange = heatGeneration - passiveLoss - heatConsumedLastTick;
        heatSource.getCapability().ifPresent(cap -> {
            if (netHeatChange > 0) cap.generateHeat(netHeatChange);
            else cap.extractHeat(Math.abs(netHeatChange), false);
        });

        //Thermostat
        boolean refueled = false;
        if (burnTime <= 0 && needsRefuel()) {
            refueled = fuelInventory.tryConsumeFuel();
        }

        //Sync and update state
        if (refueled) {
            notifyUpdate();
        }

        updateBlockState();
        updateHeatLevelName();
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

            //Get minimum heat required by consumer
            float thresholdPercent = consumer.getOperatingThreshold();
            float thresholdInHU = MAX_HEAT * thresholdPercent;

            //Predict heat on next tick
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

            // The consumer decides how much heat to pull
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
        boolean isBurningNow = burnTime > 0;
        HeatLevel currentHeatLevel = calculateHeatLevel();

        if (getBlockState().getValue(SolidBurnerBlock.LIT) != isBurningNow || getBlockState().getValue(AbstractBurnerBlock.HEAT) != currentHeatLevel) {
            level.setBlock(worldPosition, getBlockState()
                .setValue(SolidBurnerBlock.LIT, isBurningNow)
                .setValue(AbstractBurnerBlock.HEAT, currentHeatLevel), 3);
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
    protected float getBaseHeatCapacity() {
        return MAX_HEAT;
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


        ItemStack fuel = fuelInventory.fuelStack;
        if (!fuel.isEmpty()) {
            LangBuilder fuelName = CreateLang.builder().add(fuel.getHoverName()).style(ChatFormatting.GRAY);
            LangBuilder fuelCount = CreateLang.builder().text("x").text(String.valueOf(fuel.getCount())).style(ChatFormatting.GREEN);

            CreateLang.builder().add(fuelName).space().add(fuelCount).forGoggles(tooltip);
        }
        
        return true;
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER)
            return fuelInventory.getCapability(cap);
        return super.getCapability(cap, side);
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putInt("burnTime", burnTime);
        tag.putString("heatLevelName", heatLevelName.name());
        tag.putBoolean("isPowered", isPowered); 
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        burnTime = tag.getInt("burnTime");
        heatLevelName = HeatLevelString.valueOf(tag.getString("heatLevelName"));
        isPowered = tag.getBoolean("isPowered");
    }
}
