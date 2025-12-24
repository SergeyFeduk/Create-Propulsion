package com.deltasf.createpropulsion.heat.burners.solid;

import java.util.List;

import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.LangBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlock;
import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlockEntity;
import com.deltasf.createpropulsion.heat.burners.BurnerDamager;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

public class SolidBurnerBlockEntity extends AbstractBurnerBlockEntity implements IBurner {
    private BurnerFuelBehaviour fuelInventory;
    private BurnerDamager damager;
    
    private int burnTime = 0;
    private static final float MAX_HEAT = 400.0f;

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

    @Override
    public float getHeatPerTick() { return 1; }

    @SuppressWarnings("null")
    @Override
    public void tick() {
        super.tick();

        if (level.isClientSide()) return;

        //Calculate rates
        float heatGeneration = (burnTime > 0) ? getHeatPerTick() : 0;
        if (burnTime > 0) burnTime--;

        //Apply heat changes
        tickHeatPhysics(heatGeneration);

        //Thermostat
        boolean refueled = false;
        if (burnTime <= 0 && shouldThermostatBurn()) {
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
    private void updateBlockState() {
        boolean isBurningNow = burnTime > 0;
        HeatLevel currentHeatLevel = calculateHeatLevel();

        BlockState state = getBlockState();
        if (state.getValue(SolidBurnerBlock.LIT) != isBurningNow || state.getValue(AbstractBurnerBlock.HEAT) != currentHeatLevel) {
            level.setBlock(worldPosition, state
                .setValue(SolidBurnerBlock.LIT, isBurningNow)
                .setValue(AbstractBurnerBlock.HEAT, currentHeatLevel), 3);
        }
    }

    @Override
    protected Direction getHeatCapSide() { return Direction.UP; }

    @Override
    protected float getBaseHeatCapacity() { return MAX_HEAT; }

    @Override
    protected void addSpecificTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        ItemStack fuel = fuelInventory.fuelStack;
        if (!fuel.isEmpty()) {
            LangBuilder fuelName = CreateLang.builder().add(fuel.getHoverName()).style(ChatFormatting.GRAY);
            LangBuilder fuelCount = CreateLang.builder().text("x").text(String.valueOf(fuel.getCount())).style(ChatFormatting.GREEN);

            CreateLang.builder().add(fuelName).space().add(fuelCount).forGoggles(tooltip);
        }
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
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        burnTime = tag.getInt("burnTime");
    }
}