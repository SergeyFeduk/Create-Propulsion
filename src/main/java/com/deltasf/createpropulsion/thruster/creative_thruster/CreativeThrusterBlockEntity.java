package com.deltasf.createpropulsion.thruster.creative_thruster;

import java.util.List;

import javax.annotation.Nullable;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.thruster.AbstractThrusterBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

    //TODO: Creative thruster computer beh (+ update insides of abstract thruster)
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        return super.getCapability(cap, side);
    }

    @Override
    public void updateThrust(BlockState currentBlockState) {
        float thrust = 0;
        int power = getOverriddenPowerOrState(currentBlockState);
        if (power > 0) {
            float powerPercentage = power / 15.0f;
            float thrustMultiplier = (float) (double) PropulsionConfig.THRUSTER_THRUST_MULTIPLIER.get(); //TODO: Different config
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
    protected boolean shouldEmitParticles() {
        if (!isPowered()) return false; 
        Level level = getLevel();
        if (level == null) return false;
        BlockPos behind = worldPosition.relative(getBlockState().getValue(CreativeThrusterBlock.FACING));
        return level.getBlockState(behind).isAir(); 
    }

    @Override
    protected LangBuilder getGoggleStatus() {
        if (isPowered()) {
            int power = getOverriddenPowerOrState(getBlockState());
            float powerPercentage = power / 15.0f;
            float maxForce = powerBehaviour.getTargetThrust();
            float currentForceKN = (maxForce * powerPercentage) / 1000.0f;

            return CreateLang.translate("gui.goggles.thruster.status.working")
                    .style(ChatFormatting.GREEN)
                    .text(" (" + String.format("%.1f", currentForceKN) + " kN)");
        }
        return CreateLang.translate("gui.goggles.thruster.status.not_powered").style(ChatFormatting.GOLD);
    }
}
