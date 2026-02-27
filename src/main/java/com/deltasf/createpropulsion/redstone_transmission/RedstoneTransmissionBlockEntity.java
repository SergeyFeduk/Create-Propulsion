package com.deltasf.createpropulsion.redstone_transmission;

import com.deltasf.createpropulsion.compat.PropulsionCompatibility;
import com.deltasf.createpropulsion.compat.computercraft.ComputerBehaviour;
import com.deltasf.createpropulsion.registries.PropulsionIcons;
import com.deltasf.createpropulsion.utility.FlickerAwareTicker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import java.util.List;

import static com.deltasf.createpropulsion.redstone_transmission.RedstoneTransmissionBlock.HORIZONTAL_FACING;
import static com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock.AXIS;

public class RedstoneTransmissionBlockEntity extends SplitShaftBlockEntity {
    public static final int MAX_VALUE = 256;

    ScrollOptionBehaviour<TransmissionMode> controlMode;
    private int shift_level = 0;
    private float prevGaugeTarget = 0f;
    private float gaugeTarget = 0f;

    private static final int FLICKER_THRESHOLD = 100;
    private FlickerAwareTicker ticker;
    private int tickCounter = 0;

    public ComputerBehaviour computerBehaviour;

    public RedstoneTransmissionBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        shift_level = compound.getInt("transmission_shift");
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        super.write(compound, clientPacket);
        compound.putInt("transmission_shift", shift_level);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);

        ticker = new FlickerAwareTicker(this, FLICKER_THRESHOLD);
        behaviours.add(ticker);

        controlMode = new ScrollOptionBehaviour<>(TransmissionMode.class, Component.translatable("createpropulsion.redstone_transmission.control_mode"), this, new TransmissionValueBox());
        
        controlMode.withCallback(i -> {
            if (TransmissionMode.values()[i] == TransmissionMode.INCREMENTAL) {
                ticker.scheduleUpdate(() -> attemptShiftUpdate(0));
            }
        });
            
        behaviours.add(controlMode);

        if (PropulsionCompatibility.CC_ACTIVE) {
            behaviours.add(computerBehaviour = new ComputerBehaviour(this));
        }
    }

    private boolean attemptShiftUpdate(int newLevel) {
        if (shift_level == newLevel) return false;

        if (getFlickerScore() > ticker.getThreshold()) return false;

        detachKinetics();
        shift_level = newLevel;
        attachKinetics();
        setChanged();
        sendData();
        if (level != null)
            level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
        
        return true;
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

    public int get_current_shift() {
        return shift_level;
    }

    @Override
    public void tick() {
        super.tick();
        prevGaugeTarget = gaugeTarget;
        gaugeTarget += Mth.clamp(Mth.PI / 2 * -shift_level / (float) MAX_VALUE - gaugeTarget, - Mth.PI / 4, Mth.PI / 4) / 10f;

        Level level = getLevel();
        if (level == null || level.isClientSide) return;

        boolean peripheralControlled = PropulsionCompatibility.CC_ACTIVE && computerBehaviour != null && computerBehaviour.hasAttachedComputer();
        if (peripheralControlled) return;

        int shiftUp = get_shift_up();
        int shiftDown = get_shift_down();

        if (controlMode.get() == TransmissionMode.DIRECT) {
            int signal = Math.max(shiftUp, shiftDown);
            int target = (int) ((signal / 15.0f) * MAX_VALUE);
            
            attemptShiftUpdate(target);
            
        } else {
            tickCounter++;
            int target = Mth.clamp(shift_level + shiftUp - shiftDown, 0, MAX_VALUE);
            boolean atBottomEdge = shift_level == 0 && shiftUp > 0;
            boolean atTopEdge = shift_level == MAX_VALUE && shiftDown > 0;
            boolean isIdle = tickCounter >= 10;
            if (atBottomEdge || atTopEdge || isIdle) {
                if (attemptShiftUpdate(target)) {
                    tickCounter = 0;
                }
            }
        }
    }

    public int getComparatorOutput() {
        return Math.round((float) shift_level / MAX_VALUE * 15);
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (!hasSource() || face == getSourceFacing()) return 1f;
        if (shift_level == 0) return 0f;
        return 1f;
    }

    @Override
    public float propagateRotationTo(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs) {
        if (connectedViaAxes && shift_level > 0) {
            Direction targetFace = Direction.getNearest(diff.getX(), diff.getY(), diff.getZ());
            if (hasSource() && targetFace != getSourceFacing()) {
                return (float) shift_level / MAX_VALUE;
            }
        }
        return 0f;
    }

    public float getGaugeTarget(float partialTick) {
        return Mth.lerp(partialTick, prevGaugeTarget, gaugeTarget);
    }

    public static class TransmissionValueBox extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8,8,0.5f);
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
        //"Transmission Mode: Direct"
        Component selectedMode = Component.translatable(controlMode.get().getTranslationKey());
        CreateLang.builder()
            .add(Component.translatable("createpropulsion.gui.goggles.redstone_transmission.title"))
            .text(": ")
            .add(selectedMode)
            .forGoggles(tooltip);

        CreateLang.builder()
            .add(Component.translatable("createpropulsion.gui.goggles.redstone_transmission.internal_shift_title"))
            .text(":")
            .style(ChatFormatting.GRAY)
            .forGoggles(tooltip);

        int max_shift_modified = controlMode.get().equals(TransmissionMode.DIRECT) ? 15 : MAX_VALUE;
        int shift_modified = Math.round((float) shift_level / MAX_VALUE * max_shift_modified);
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
            .text(":")
            .style(ChatFormatting.GRAY)
            .forGoggles(tooltip);

        IRotate.SpeedLevel.getFormattedSpeedText(speed * shift_level / MAX_VALUE, isOverStressed()).forGoggles(tooltip);

        if (PropulsionCompatibility.CC_ACTIVE && computerBehaviour != null && computerBehaviour.hasAttachedComputer()) {
            CreateLang.translate("gui.goggles.cc.peripheral_controlled", new Object[0]).style(ChatFormatting.GRAY).forGoggles(tooltip);
        }

        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        return true;
    }

    public TransmissionMode getTransmissionMode() {
        return controlMode.get();
    }

    public void setTransmissionMode(TransmissionMode mode) {
        controlMode.setValue(mode.ordinal());
    }

    public void setShiftFromPeripheral(int level) {
        TransmissionMode mode = controlMode.get();
        int target;
        
        if (mode == TransmissionMode.DIRECT) {
            float clampedLevel = Math.max(0, Math.min(15, level));
            target = Math.round((clampedLevel / 15.0f) * MAX_VALUE);
        } else {
            target = Math.max(0, Math.min(MAX_VALUE, level));
        }
        
        ticker.scheduleUpdate(() -> attemptShiftUpdate(target));
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (PropulsionCompatibility.CC_ACTIVE && computerBehaviour != null && computerBehaviour.isPeripheralCap(cap)) {
            return computerBehaviour.getPeripheralCapability();
        }
        return super.getCapability(cap, side);
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
