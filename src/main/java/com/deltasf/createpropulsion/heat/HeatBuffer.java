package com.deltasf.createpropulsion.heat;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public class HeatBuffer implements IHeatSource, INBTSerializable<CompoundTag> {
    protected float heat;
    protected float capacity;
    protected float expectedHeatProduction;

    public HeatBuffer(float initialHeat, float capacity, float expectedHeatProduction) {
        this.heat = initialHeat;
        this.capacity = capacity;
        this.expectedHeatProduction = expectedHeatProduction;
    }

    @Override
    public float extractHeat(float amount, boolean simulate) {
        float heatToExtract = Math.min(heat, amount);
        if (!simulate) {
            heat -= heatToExtract;
        }
        return heatToExtract;
    }

    @Override
    public float getHeatStored() {
        return heat;
    }

    @Override
    public float getMaxHeatStored() {
        return capacity;
    }

    @Override
    public float getExpectedHeatProduction() {
        return expectedHeatProduction;
    }

    @Override
    public void generateHeat(float amount) {
        heat = Math.min(capacity, heat + amount);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Heat", this.heat);
        tag.putFloat("Capacity", this.capacity);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.heat = nbt.getFloat("Heat");
        this.capacity = nbt.getFloat("Capacity");
    }
}
