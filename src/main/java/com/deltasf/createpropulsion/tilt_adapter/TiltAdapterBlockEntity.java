package com.deltasf.createpropulsion.tilt_adapter;

import java.util.List;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.compat.PropulsionCompatibility;
import com.deltasf.createpropulsion.compat.computercraft.ComputerBehaviour;
import com.deltasf.createpropulsion.utility.FlickerAwareTicker;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity.SequenceContext;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;

public class TiltAdapterBlockEntity extends SplitShaftBlockEntity {
    public static final float SIGNAL_RANGE = 15.0f;

    protected int redstoneLeft = 0;
    protected int redstoneRight = 0;
    
    protected float targetAngle = 0f;
    protected float networkTargetAngle = 0f;
    protected float currentAngle = 0f;
    protected int activeMoveDirection = 0;
    protected float activeSequenceLimit = 0f;
    protected float computerTargetAngle = 0f;

    public FlickerAwareTicker flickerTicker;
    public AbstractComputerBehaviour computerBehaviour;

    public TiltAdapterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        flickerTicker = new FlickerAwareTicker(this, 60); 
        behaviours.add(flickerTicker);

        if (PropulsionCompatibility.CC_ACTIVE) {
            behaviours.add(computerBehaviour = new ComputerBehaviour(this));
        }
    }

    @Override
    public void tick() {
        super.tick();
        Level level = getLevel();
        if (level == null || level.isClientSide) return;

        float speed = Math.abs(getTheoreticalSpeed());
        if (activeMoveDirection != 0 && speed > 0 && activeSequenceLimit > 0) {
            float step = KineticBlockEntity.convertToAngular(speed);
            float actualStep = Math.min(step, activeSequenceLimit);
            
            currentAngle += actualStep * activeMoveDirection;
            activeSequenceLimit -= actualStep;

            if (activeSequenceLimit <= 0) {
                activeSequenceLimit = 0;
                currentAngle = networkTargetAngle;
                flickerTicker.scheduleUpdate(this::syncNetworkState);
            }
        }

        checkRedstoneAndSpeed();
    }

    public int getLeft() { return redstoneLeft; }
    public int getRight() { return redstoneRight; }

    public void setComputerTargetAngle(float angle) {
        computerTargetAngle = angle;
    }

    private void checkRedstoneAndSpeed() {
        Level level = getLevel();
        if (level == null) return;

        BlockState state = getBlockState();
        Axis axis = state.getValue(RotatedPillarKineticBlock.AXIS);
        boolean positiveDir = state.getValue(TiltAdapterBlock.POSITIVE);
        boolean alignedX = state.getValue(TiltAdapterBlock.ALIGNED_X);

        Direction posSignalSide = (axis == Axis.X) ? Direction.SOUTH : (axis == Axis.Z ? Direction.WEST : (alignedX ? Direction.NORTH : Direction.EAST));
        Direction negSignalSide = (axis == Axis.X) ? Direction.NORTH : (axis == Axis.Z ? Direction.EAST : (alignedX ? Direction.SOUTH : Direction.WEST));

        if (!positiveDir) {
            Direction temp = posSignalSide; posSignalSide = negSignalSide; negSignalSide = temp;
        }

        int newLeft = level.getSignal(worldPosition.relative(posSignalSide), posSignalSide);
        int newRight = level.getSignal(worldPosition.relative(negSignalSide), negSignalSide);
        
        if (newLeft != redstoneLeft || newRight != redstoneRight) {
            redstoneLeft = newLeft;
            redstoneRight = newRight;
            sendData();
        }

        if (getTheoreticalSpeed() == 0) {
            if (activeMoveDirection != 0) {
                flickerTicker.scheduleUpdate(this::syncNetworkState);
            }
            return;
        }

        double newTarget;

        if (PropulsionCompatibility.CC_ACTIVE && computerBehaviour != null && computerBehaviour.hasAttachedComputer()) {
            newTarget = computerTargetAngle;
        } else {
            int diff = redstoneLeft - redstoneRight; 
            newTarget = (diff / SIGNAL_RANGE) * PropulsionConfig.TILT_ADAPTER_ANGLE_RANGE.get();
        }
        
        if (Math.abs(newTarget - targetAngle) > 0.001f) {
            targetAngle = (float)newTarget;
            flickerTicker.scheduleUpdate(this::syncNetworkState);
        }
    }

    private void syncNetworkState() {
        float speed = Math.abs(getTheoreticalSpeed());
        float delta = targetAngle - currentAngle;

        if (Math.abs(delta) > 0.001f && speed > 0) {
            activeMoveDirection = (int) Math.signum(delta);
            activeSequenceLimit = Math.abs(delta);
            networkTargetAngle = targetAngle;
            
            float kineticSpeed = speed * activeMoveDirection;
            sequenceContext = new SequenceContext(SequencerInstructions.TURN_ANGLE, activeSequenceLimit / Math.abs(kineticSpeed));
            
            boolean returningToZero = Math.abs(targetAngle) < 0.001f;
            ((ISnappingSequenceContext) (Object) sequenceContext).setSnapToZero(returningToZero);
        } else {
            activeMoveDirection = 0;
            activeSequenceLimit = 0;
            sequenceContext = null;
            currentAngle = targetAngle;
        }

        detachKinetics();
        attachKinetics();
        sendData();
    }

    @Override
    public float propagateRotationTo(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs) {
        Direction directionToTarget = Direction.getNearest(diff.getX(), diff.getY(), diff.getZ());
        if (directionToTarget == getBackFace(stateFrom)) return 0;
        return super.propagateRotationTo(target, stateFrom, stateTo, diff, connectedViaAxes, connectedViaCogs);
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (face == TiltAdapterBlock.getDirection(getBlockState())) return activeMoveDirection;
        if (hasSource() && getSourceFacing() != getBackFace(getBlockState())) return 0;
        return 1;
    }

    private Direction getBackFace(BlockState state) {
        Axis axis = state.getValue(RotatedPillarKineticBlock.AXIS);
        boolean positive = state.hasProperty(TiltAdapterBlock.POSITIVE) ? state.getValue(TiltAdapterBlock.POSITIVE) : true;
        return Direction.get(positive ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE, axis);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (PropulsionCompatibility.CC_ACTIVE && computerBehaviour != null && computerBehaviour.isPeripheralCap(cap)) {
            return computerBehaviour.getPeripheralCapability();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void initialize() {
        super.initialize();
        Level level = getLevel();
        if (level != null && !level.isClientSide) flickerTicker.scheduleUpdate(this::syncNetworkState);
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        compound.putFloat("targetAngle", targetAngle);
        compound.putFloat("networkTargetAngle", networkTargetAngle);
        compound.putFloat("currentAngle", currentAngle);
        compound.putInt("activeMoveDirection", activeMoveDirection);
        compound.putFloat("activeSequenceLimit", activeSequenceLimit);
        compound.putInt("redstoneLeft", redstoneLeft);
        compound.putInt("redstoneRight", redstoneRight);
        compound.putFloat("computerTargetAngle", computerTargetAngle);
        super.write(compound, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        targetAngle = compound.getFloat("targetAngle");
        networkTargetAngle = compound.getFloat("networkTargetAngle");
        currentAngle = compound.getFloat("currentAngle");
        activeMoveDirection = compound.getInt("activeMoveDirection");
        activeSequenceLimit = compound.getFloat("activeSequenceLimit");
        redstoneLeft = compound.getInt("redstoneLeft");
        redstoneRight = compound.getInt("redstoneRight");
        computerTargetAngle = compound.getFloat("computerTargetAngle");
        super.read(compound, clientPacket);
    }

    @Override protected void copySequenceContextFrom(KineticBlockEntity sourceBE) {}
    @Override protected boolean syncSequenceContext() { return true; }
}