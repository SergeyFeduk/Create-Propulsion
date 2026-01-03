package com.deltasf.createpropulsion.thruster.creative_thruster;

import java.util.List;

import javax.annotation.Nullable;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.compat.PropulsionCompatibility;
import com.deltasf.createpropulsion.thruster.AbstractThrusterBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public class CreativeThrusterBlockEntity extends AbstractThrusterBlockEntity {
    private CreativeThrusterPowerScrollValueBehaviour powerBehaviour;

    public CreativeThrusterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        powerBehaviour = new CreativeThrusterPowerScrollValueBehaviour(this);
        powerBehaviour.value = 49;
        powerBehaviour.withCallback(i -> {
            updateThrust(getBlockState());
            sendData(); 
        });
        behaviours.add(powerBehaviour);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (PropulsionCompatibility.CC_ACTIVE && computerBehaviour.isPeripheralCap(cap)) {
            return computerBehaviour.getPeripheralCapability().cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void updateThrust(BlockState currentBlockState) {
        float thrust = 0;
        int power = getOverriddenPowerOrState(currentBlockState);
        if (power > 0) {
            float powerPercentage = power / 15.0f;
            float thrustMultiplier = (float) (double) PropulsionConfig.CREATIVE_THRUSTER_THRUST_MULTIPLIER.get();
            float powerMultiplier = powerBehaviour.getTargetThrust();
            thrust = thrustMultiplier * powerPercentage * powerMultiplier;
        }
        thrusterData.setThrust(thrust);
        isThrustDirty = false;
    }

    @Override
    protected boolean isWorking() { return true; }

    @Override
    public void calculateObstruction(Level level, BlockPos pos, Direction forwardDirection) {
        this.emptyBlocks = OBSTRUCTION_LENGTH;
    }

    @Override
    protected double getNozzleOffsetFromCenter() {
        return 0.7;
    }

    @Override
    protected boolean shouldEmitParticles() {
        if (!isPowered()) return false; 
        Level level = getLevel();
        if (level == null) return false;

        Direction facing = getBlockState().getValue(CreativeThrusterBlock.FACING);
        BlockPos plumeOccupiedPosition = worldPosition.relative(facing.getOpposite());
        return !level.getBlockState(plumeOccupiedPosition).isFaceSturdy(level, plumeOccupiedPosition, facing);
    }

    @Override
    protected void addThrusterDetails(List<Component> tooltip, boolean isPlayerSneaking) {
        float maxThrustKN = powerBehaviour.getTargetThrust() / 1000.0f;
        int power = getOverriddenPowerOrState(getBlockState());
        float powerPercentage = power / 15.0f;
        int currentThrustKN = (int)Math.ceil(maxThrustKN * powerPercentage);
        // "Thrust Output: 400/450 kN"
        CreateLang.builder()
            .add(CreateLang.translate("gui.goggles.thruster.thrust_output")).text(": ")
            .add(CreateLang.number(currentThrustKN)).text(" / ").add(CreateLang.number(maxThrustKN))
            .space()
            .add(CreateLang.translate("gui.goggles.thruster.unit_kn"))
            .style(ChatFormatting.GRAY)
            .forGoggles(tooltip);
    }

    @Override
    protected LangBuilder getGoggleStatus() {
        if (isPowered()) {
            return CreateLang.translate("gui.goggles.thruster.status.working").style(ChatFormatting.GREEN);
        }
        return CreateLang.translate("gui.goggles.thruster.status.not_powered").style(ChatFormatting.GOLD);
    }

    // CC slop

    public int getOverriddenPowerOrState(BlockState state) {
        return super.getOverriddenPowerOrState(state);
    }

    public void setThrustConfig(int value) {
        int clamped = Math.max(0, Math.min(value, 99));
        if (powerBehaviour.getValue() != clamped) {
            powerBehaviour.setValue(clamped);
            updateThrust(getBlockState());
            setChanged();
            sendData();
        }
    }

    public int getThrustConfig() {
        return powerBehaviour.getValue();
    }

    public float getTargetThrustNewtons() {
        return powerBehaviour.getTargetThrust();
    }
}
