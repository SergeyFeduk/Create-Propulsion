package com.deltasf.createpropulsion.heat.engine;

import java.util.List;

import com.deltasf.createpropulsion.heat.IHeatConsumer;
import com.deltasf.createpropulsion.registries.PropulsionCapabilities;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public class StirlingEngineBlockEntity extends GeneratingKineticBlockEntity implements IHeatConsumer {
    public static final float GENERATED_SU = 8.0f;
    public static final float MAX_GENERATED_RPM = 256.0f;
    public static final float HEAT_CONSUMPTION_RATE = 1.0f; 

    private final LazyOptional<IHeatConsumer> heatConsumerCapability;
    protected StirlingScrollValueBehaviour targetSpeedBehaviour;
    private int activeTicks = 0;
    private boolean firstTick = true;

    public StirlingEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.heatConsumerCapability = LazyOptional.of(() -> this);
    }

    @Override
    public void initialize() {
        super.initialize();
        if (activeTicks > 0 || getGeneratedSpeed() > getTheoreticalSpeed()) {
            updateGeneratedRotation();
        }
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviors) {
        super.addBehaviours(behaviors);
        targetSpeedBehaviour = new StirlingScrollValueBehaviour(Lang.translate("whenthe").component(), this, new StirlingEngineValueBox());
        targetSpeedBehaviour.value = 4;
        targetSpeedBehaviour.withCallback(i -> this.updateGeneratedRotation());
        behaviors.add(targetSpeedBehaviour);
    }

    @SuppressWarnings("null")
    @Override
    public void tick() {
        super.tick();
        if (level.isClientSide) return;

        if (firstTick) {
            firstTick = false;
            if (activeTicks > 0) {
                reActivateSource = true;
            }
        }

        if (activeTicks > 0) {
            activeTicks--;
            if (activeTicks == 0) {
                updateGeneratedRotation();
            }
        }
    }

    @Override
    public boolean isActive() {
        return true; 
    }

    @Override
    public float getOperatingThreshold() {
        return 0.1f;
    }

    @Override
    public float consumeHeat(float maxAvailable, boolean simulate) {
        float toConsume = Math.min(HEAT_CONSUMPTION_RATE, maxAvailable);

        if (!simulate && toConsume > 0) {
            boolean wasInactive = activeTicks == 0;
            this.activeTicks = 3;

            //We were off, but now we are activate -> update rotation
            if (wasInactive) {
                updateGeneratedRotation();
            }
        }

        return toConsume;
    }

     @Override
    public float getGeneratedSpeed() {
        if (activeTicks <= 0) return 0f;
        int generatedRPM = targetSpeedBehaviour.getRPM();
        return convertToDirection(generatedRPM, getBlockState().getValue(StirlingEngineBlock.FACING));
    }

    @Override
    public float calculateAddedStressCapacity() {
        if (activeTicks <= 0) return 0f;
        float rpm = targetSpeedBehaviour.getUnsignedRPM();
        if (rpm == 0) return 0f; 

        float stressFactor = MAX_GENERATED_RPM / rpm;
        float capacity = stressFactor * GENERATED_SU;

        this.lastCapacityProvided = capacity;
        return capacity;
    }

    //Caps

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == PropulsionCapabilities.HEAT_CONSUMER && side == Direction.DOWN) {
            return heatConsumerCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        heatConsumerCapability.invalidate();
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putInt("activeTicks", activeTicks);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        activeTicks = compound.getInt("activeTicks");
    }
}
