package com.deltasf.createpropulsion.balloons.blocks.hot_air_burner;

import java.util.List;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.blocks.AbstractHotAirInjectorBlockEntity;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Lang;

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

//2) I need to first just FIGURE OUT when to make this block perform a scan. First of all, let me explain to you, what it is and why it is like that. 

public class HotAirBurnerBlockEntity extends AbstractHotAirInjectorBlockEntity implements IHaveGoggleInformation {
    private HotAirBurnerFuelBehaviour fuelInventory;
    private int leverPosition = 0; // 0-1-2

    public HotAirBurnerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        fuelInventory = new HotAirBurnerFuelBehaviour(this);
        behaviours.add(fuelInventory);
    }

    public void cycleLever(boolean isShiftPressed) {
        if (isShiftPressed) {
            leverPosition = Math.max(0, leverPosition - 1);
        } else {
            leverPosition = Math.min(2, leverPosition + 1);
        }
        notifyUpdate();
    }

    public int getLeverPosition() {
        return leverPosition;
    }

    public ItemStack getFuelStack() {
        return fuelInventory.fuelStack;
    }

    @Override
    public double getInjectionAmount() {
        return 1;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        //Temp, for debugging purposes
        Ship ship = VSGameUtilsKt.getShipManagingPos(getLevel(), worldPosition);
        if (ship == null) return false;
        Balloon balloon = BalloonShipRegistry.forShip(ship.getId()).getBalloonOf(this.haiId);
        if (balloon == null) {
            Lang.builder().text("Balloon not found, hai is not active").forGoggles(tooltip);
            return true;
        }

        Lang.builder().text("Balloon").forGoggles(tooltip);
        Lang.builder().text("Hot air: ")
            .add(Lang.number(balloon.hotAir))
            .text(" / ")
            .add(Lang.number(balloon.getVolumeSize())).forGoggles(tooltip);
        
        double percentage = balloon.hotAir / balloon.getVolumeSize();
        Lang.builder().add(Lang.number(percentage * 100)).text("%").forGoggles(tooltip);
        
        Lang.builder().text("Holes: " + balloon.holes.size()).forGoggles(tooltip);
        return true;
    }

    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER)
            return fuelInventory.getCapability(cap);
        return super.getCapability(cap, side);
    }

    @Override
    protected void read(CompoundTag tag, boolean isClient) {
        super.read(tag, isClient);
        leverPosition = tag.getInt("leverPosition");
    }

    @Override
    protected void write(CompoundTag tag, boolean isClient) {
        super.write(tag, isClient);
        tag.putInt("leverPosition", leverPosition);
    }

}
