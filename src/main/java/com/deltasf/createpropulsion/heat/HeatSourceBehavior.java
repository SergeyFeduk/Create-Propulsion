package com.deltasf.createpropulsion.heat;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.LazyOptional;

public class HeatSourceBehavior extends BlockEntityBehaviour {
    //Have some tea
    public static final BehaviourType<HeatSourceBehavior> TYPE = new BehaviourType<>();
    private HeatBuffer heatBuffer; 
    private LazyOptional<IHeatSource> capability;

    public HeatSourceBehavior(SmartBlockEntity be, float capacity, float expectedHeatProduction) {
        super(be);
        this.heatBuffer = new HeatBuffer(0, capacity, expectedHeatProduction);
        this.capability = LazyOptional.of(() -> this.heatBuffer);
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    public LazyOptional<IHeatSource> getCapability() {
        return this.capability;
    }

    @Override
    public void write(CompoundTag nbt, boolean clientPacket) {
        nbt.put("HeatBuffer", heatBuffer.serializeNBT());
        super.write(nbt, clientPacket);
    }

    @Override
    public void read(CompoundTag nbt, boolean clientPacket) {
        heatBuffer.deserializeNBT(nbt.getCompound("HeatBuffer"));
        super.read(nbt, clientPacket);
    }

    @Override
    public void unload() {
        super.unload();
        capability.invalidate();
    }
}
