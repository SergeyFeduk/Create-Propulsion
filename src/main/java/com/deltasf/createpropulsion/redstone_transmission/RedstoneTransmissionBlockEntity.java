package com.deltasf.createpropulsion.redstone_transmission;

import com.deltasf.createpropulsion.PropulsionAllIcons;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.deltasf.createpropulsion.redstone_transmission.RedstoneTransmissionBlock.MAX_VALUE;
import static com.deltasf.createpropulsion.redstone_transmission.RedstoneTransmissionBlock.SHIFT_LEVEL;
import static com.deltasf.createpropulsion.redstone_transmission.RedstoneTransmissionBlock.HORIZONTAL_FACING;

public class RedstoneTransmissionBlockEntity extends SplitShaftBlockEntity {

    ScrollOptionBehaviour<TransmissionMode> controlMode;
    private float prevGaugeTarget = 0f;
    private float gaugeTarget = 0f;

    public RedstoneTransmissionBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        controlMode = new ScrollOptionBehaviour<>(TransmissionMode.class, Component.translatable("createpropulsion.redstone_transmission.control_mode"), this, new TransmissionValueBox());
        behaviours.add(controlMode);
    }

    public void updateShift(int shift_up, int shift_down){
        int value = getBlockState().getValue(SHIFT_LEVEL);
        int newValue;
        if(controlMode.get() == TransmissionMode.INCREMENTAL) {
            newValue = Mth.clamp(value + shift_up - shift_down, 0, MAX_VALUE);
        } else {
            newValue = Math.max(shift_up, shift_down) * 17;
        }
        if (value != newValue) {
            detachKinetics();
            level.setBlock(getBlockPos(), getBlockState().setValue(SHIFT_LEVEL, newValue), 3);
            attachKinetics();
        }
    }

    @Override
    public void tick() {
        super.tick();
        prevGaugeTarget = gaugeTarget;
        gaugeTarget += Mth.clamp(Mth.PI / 2 * -getBlockState().getValue(SHIFT_LEVEL) / 255f - gaugeTarget, - Mth.PI / 4, Mth.PI / 4) / 10f;
    }

    @Override
    public void lazyTick() {
        if(level == null) {
            return;
        }
        Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        int shift_up = level.getSignal(getBlockPos().relative(facing.getCounterClockWise()), facing.getCounterClockWise());
        int shift_down = level.getSignal(getBlockPos().relative(facing.getClockWise()), facing.getClockWise());
        updateShift(shift_up, shift_down);
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (getBlockState().getValue(SHIFT_LEVEL) == 0) return 0;
        if (hasSource() && getSourceFacing() == face) return (float) MAX_VALUE / getBlockState().getValue(SHIFT_LEVEL);
        else return 1;
    }

    public float getGaugeTarget(float partialTick) {
        return Mth.lerp(partialTick, prevGaugeTarget, gaugeTarget);
    }

    public static class TransmissionValueBox extends ValueBoxTransform.Sided {

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8,8,-0.125);
        }

        @Override
        public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            Direction side = state.getValue(HORIZONTAL_FACING);
            float horizontalAngle = AngleHelper.horizontalAngle(side);
            return VecHelper.rotateCentered(getSouthLocation(), horizontalAngle, Direction.Axis.Y);
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
            Direction facing = state.getValue(HORIZONTAL_FACING);
            float yRot = AngleHelper.horizontalAngle(facing);
            ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yRot));
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CreateLang.builder()
                .add(Component.translatable("createpropulsion.gui.goggles.redstone_transmission.control_mode"))
                .text(": ")
                .add(Component.translatable(controlMode.get().getTranslationKey()))
                .forGoggles(tooltip);
        
        CreateLang.builder()
                .add(Component.translatable(
                                    "createpropulsion.gui.goggles.redstone_transmission.internal_shift",
                                    getBlockState().getValue(SHIFT_LEVEL) / (controlMode.get().equals(TransmissionMode.DIRECT) ? 17 : 1),
                                    controlMode.get().equals(TransmissionMode.DIRECT) ? 15 : 255
                        )
                )
                .forGoggles(tooltip);

        CreateLang.builder()
                .add(Component.translatable("createpropulsion.gui.goggles.redstone_transmission.output"))
                .add(IRotate.SpeedLevel.getFormattedSpeedText(speed, isOverStressed()).component())
                .forGoggles(tooltip);

        return true;
    }

    public enum TransmissionMode implements INamedIconOptions {
        DIRECT(PropulsionAllIcons.DIRECT_CONTROL), INCREMENTAL(PropulsionAllIcons.INCREMENTAL_CONTROL), ;

        private String translationKey;
        private AllIcons icon;

        TransmissionMode(AllIcons icon) {
            this.icon = icon;
            translationKey = "createpropulsion.redstone_transmission.control_mode." + Lang.asId(name());
        }

        @Override
        public AllIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }
    }
}
