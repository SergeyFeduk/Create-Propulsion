package com.deltasf.createpropulsion.thruster.creative_thruster;

import java.util.List;

import javax.annotation.Nullable;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.compat.PropulsionCompatibility;
import com.deltasf.createpropulsion.particles.ParticleTypes;
import com.deltasf.createpropulsion.particles.plasma.PlasmaParticleData;
import com.deltasf.createpropulsion.thruster.AbstractThrusterBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public class CreativeThrusterBlockEntity extends AbstractThrusterBlockEntity {
    private CreativeThrusterPowerScrollValueBehaviour powerBehaviour;

    public enum PlumeType { PLASMA, PLUME, NONE }
    public PlumeType plumeType = PlumeType.PLASMA;

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
        float currentPower = getPower();
        if (currentPower  > 0) {
            float thrustMultiplier = (float) (double) PropulsionConfig.CREATIVE_THRUSTER_THRUST_MULTIPLIER.get();
            float powerMultiplier = powerBehaviour.getTargetThrust();
            thrust = thrustMultiplier * currentPower * powerMultiplier;
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

    //Particles

    public void cyclePlumeType() {
        int ordinal = plumeType.ordinal() + 1;
        if (ordinal >= PlumeType.values().length) {
            ordinal = 0;
        }
        plumeType = PlumeType.values()[ordinal];
        setChanged();
        sendData();
    }

    @Override
    protected double getNozzleOffsetFromCenter() {
        return 0.55;
    }

    @Override
    protected boolean shouldEmitParticles() {
        if (plumeType == PlumeType.NONE) return false;

        if (!isPowered()) return false; 
        Level level = getLevel();
        if (level == null) return false;

        Direction facing = getBlockState().getValue(CreativeThrusterBlock.FACING);
        BlockPos plumeOccupiedPosition = worldPosition.relative(facing.getOpposite());
        return !level.getBlockState(plumeOccupiedPosition).isFaceSturdy(level, plumeOccupiedPosition, facing);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected ParticleOptions createParticleOptions() {
        if (plumeType == PlumeType.PLASMA) {
            return new PlasmaParticleData((ParticleType<PlasmaParticleData>) ParticleTypes.getPlasmaType());
        }
        //Default is plume :P
        return super.createParticleOptions();
    }

    @Override
    protected void addThrusterDetails(List<Component> tooltip, boolean isPlayerSneaking) {
        float maxThrustKN = powerBehaviour.getTargetThrust() / 1000.0f;
        float currentPower = getPower();
        int currentThrustKN = (int)Math.ceil(maxThrustKN * currentPower);

        //"Thrust Output: 400/450 kN"
        LangBuilder thrustBuilder = CreateLang.builder();
        thrustBuilder.add(CreateLang.translate("gui.goggles.thruster.thrust_output").text(": "));
        thrustBuilder.add(CreateLang.number(currentThrustKN).text(" / ").add(CreateLang.number(maxThrustKN)).style(ChatFormatting.GRAY));
        thrustBuilder.space().add(CreateLang.translate("gui.goggles.thruster.unit_kn"));
        thrustBuilder.forGoggles(tooltip);

        //"Particle: Plasma"
        LangBuilder particleBuilder = CreateLang.builder()
            .add(CreateLang.translate("gui.goggles.creative_thruster.particle")).text(": ");

        switch (plumeType) {
            case PLASMA -> particleBuilder.add(CreateLang.translate("gui.goggles.creative_thruster.particle.plasma").style(ChatFormatting.AQUA));
            case PLUME -> particleBuilder.add(CreateLang.translate("gui.goggles.creative_thruster.particle.plume").style(ChatFormatting.GOLD));
            case NONE -> particleBuilder.add(CreateLang.translate("gui.goggles.creative_thruster.particle.none").style(ChatFormatting.DARK_GRAY));
        }

        particleBuilder.forGoggles(tooltip);

    }

    @Override
    protected LangBuilder getGoggleStatus() {
        if (isPowered()) {
            return CreateLang.translate("gui.goggles.thruster.status.working").style(ChatFormatting.GREEN);
        }
        return CreateLang.translate("gui.goggles.thruster.status.not_powered").style(ChatFormatting.GOLD);
    }

    // CC slop

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

    //NBT

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putInt("plumeType", plumeType.ordinal());
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        if (compound.contains("plumeType")) {
            int idx = compound.getInt("plumeType");
            plumeType = PlumeType.values()[Mth.clamp(idx, 0, PlumeType.values().length - 1)];
        }
    }
}
