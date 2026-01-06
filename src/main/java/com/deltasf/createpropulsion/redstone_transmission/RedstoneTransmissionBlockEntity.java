package com.deltasf.createpropulsion.redstone_transmission;

import com.deltasf.createpropulsion.registries.PropulsionIcons;
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
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.deltasf.createpropulsion.redstone_transmission.RedstoneTransmissionBlock.HORIZONTAL_FACING;
import static com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock.AXIS;

public class RedstoneTransmissionBlockEntity extends SplitShaftBlockEntity {
    public static final int MAX_VALUE = 256;

    ScrollOptionBehaviour<TransmissionMode> controlMode;
    private int shift_level = 0;
    private float prevGaugeTarget = 0f;
    private float gaugeTarget = 0f;

    public RedstoneTransmissionBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        shift_level = compound.getInt("transmission_shift");
        super.read(compound, clientPacket);
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        compound.putInt("transmission_shift", shift_level);
        super.write(compound, clientPacket);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        controlMode = new ScrollOptionBehaviour<>(TransmissionMode.class, Component.translatable("createpropulsion.redstone_transmission.control_mode"), this, new TransmissionValueBox());
        behaviours.add(controlMode);
    }

    public void updateShift(int shift_up, int shift_down){
        int newValue;
        if(controlMode.get() == TransmissionMode.INCREMENTAL) {
            newValue = Mth.clamp(shift_level + shift_up - shift_down, 0, MAX_VALUE);
        } else {
            newValue = Math.max(shift_up, shift_down) * 17;
        }
        if (shift_level != newValue) {
            detachKinetics();
            removeSource();
            shift_level = newValue;
            attachKinetics();
        }
    }

    public int get_shift_up() {
        Level level = getLevel();
        if(level == null) return 0;
        Direction facing = getBlockState().getValue(HORIZONTAL_FACING);
        return level.getSignal(getBlockPos().relative(facing.getCounterClockWise()), facing.getCounterClockWise());
    }

    public int get_shift_down() {
        Level level = getLevel();
        if(level == null) return 0;
        Direction facing = getBlockState().getValue(HORIZONTAL_FACING);
        return level.getSignal(getBlockPos().relative(facing.getClockWise()), facing.getClockWise());
    }

    @Override
    public void tick() {
        super.tick();
        prevGaugeTarget = gaugeTarget;
        gaugeTarget += Mth.clamp(Mth.PI / 2 * -shift_level / 255f - gaugeTarget, - Mth.PI / 4, Mth.PI / 4) / 10f;
    }

    @Override
    public void lazyTick() {
        Level level = getLevel();
        if(level == null || level.isClientSide) {
            return;
        }
        updateShift(get_shift_up(), get_shift_down());
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (shift_level == 0) return 0;
        if (hasSource() && getSourceFacing() == face) return (float) MAX_VALUE / shift_level;
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
            Vec3 result = getSouthLocation();
            if (state.getValue(AXIS).isHorizontal()) {
                result = VecHelper.rotateCentered(result, 270, Direction.Axis.X);
            }
            Direction side = state.getValue(HORIZONTAL_FACING);
            float horizontalAngle = AngleHelper.horizontalAngle(side);
            return VecHelper.rotateCentered(result, horizontalAngle, Direction.Axis.Y);
        }

        @Override
        public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
            Direction facing = state.getValue(HORIZONTAL_FACING);
            float yRot = AngleHelper.horizontalAngle(facing);
            ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yRot));
            if(state.getValue(AXIS).isHorizontal()){
                ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(270));
            }
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        CreateLang.builder()
                .add(Component.translatable("createpropulsion.gui.goggles.redstone_transmission.title"))
                .forGoggles(tooltip);

        CreateLang.builder()
                .add(Component.translatable("createpropulsion.gui.goggles.redstone_transmission.control_mode"))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        //CreateLang newLine ain't working for me...
        CreateLang.builder()
                .space()
                .add(Component.translatable(controlMode.get().getTranslationKey()))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        CreateLang.builder()
                .add(Component.translatable("createpropulsion.gui.goggles.redstone_transmission.internal_shift_title"))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        int max_shift_modified = controlMode.get().equals(TransmissionMode.DIRECT) ? 15 : MAX_VALUE;
        int shift_modified = shift_level * max_shift_modified / MAX_VALUE;
        IRotate.SpeedLevel transmitStyle = IRotate.SpeedLevel.NONE;
        if(shift_level >= MAX_VALUE / 2) {
            transmitStyle = IRotate.SpeedLevel.FAST;
        } else if (shift_level >= MAX_VALUE / 4) {
            transmitStyle = IRotate.SpeedLevel.MEDIUM;
        } else if (shift_level >= MAX_VALUE / 8) {
            transmitStyle = IRotate.SpeedLevel.SLOW;
        }

        CreateLang.builder()
                .add(Component.translatable(
                        "createpropulsion.gui.goggles.redstone_transmission.internal_shift_number",
                        shift_modified,
                        max_shift_modified
                        ))
                .style(transmitStyle.getTextColor())
                .forGoggles(tooltip);

        CreateLang.builder()
                .add(Component.translatable("createpropulsion.gui.goggles.redstone_transmission.output"))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        IRotate.SpeedLevel.getFormattedSpeedText(speed, isOverStressed()).forGoggles(tooltip);

        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        return true;
    }

    public enum TransmissionMode implements INamedIconOptions {
        DIRECT(PropulsionIcons.DIRECT_CONTROL), INCREMENTAL(PropulsionIcons.INCREMENTAL_CONTROL), ;

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
