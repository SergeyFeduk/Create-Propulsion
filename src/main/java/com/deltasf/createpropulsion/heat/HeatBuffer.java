package com.deltasf.createpropulsion.heat;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public class HeatBuffer implements IHeatSource, INBTSerializable<CompoundTag> {
    protected int heat;
    protected int capacity;

    public HeatBuffer(int initialHeat, int capacity) {
        this.heat = initialHeat;
        this.capacity = capacity;
    }

    @Override
    public int extractHeat(int amount, boolean simulate) {
        int heatToExtract = Math.min(heat, amount);
        if (!simulate) {
            heat -= heatToExtract;
        }
        return heatToExtract;
    }

    @Override
    public int getHeatStored() {
        return heat;
    }

    @Override
    public int getMaxHeatStored() {
        return capacity;
    }

    public void generateHeat(int amount) {
        heat = Math.min(capacity, heat + amount);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Heat", this.heat);
        tag.putInt("Capacity", this.capacity);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.heat = nbt.getInt("Heat");
        this.capacity = nbt.getInt("Capacity");
    }
}
