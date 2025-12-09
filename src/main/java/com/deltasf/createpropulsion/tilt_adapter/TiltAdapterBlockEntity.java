package com.deltasf.createpropulsion.tilt_adapter;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlockEntity.SequenceContext;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;

//TODO: Add flicker tally protection
public class TiltAdapterBlockEntity extends SplitShaftBlockEntity {
    public static final float SIGNAL_RANGE = 15.0f;

    //State
    protected int redstoneLeft = 0;
    protected int redstoneRight = 0;
    protected float currentAngle = 0f;
    protected float startAngle = 0f;
    protected float targetAngle = 0f;

    //Movement
    protected boolean isMoving = false;
    protected int moveDirection = 0;           
    protected long moveStartTime = -1;         
    protected int predictedDuration = -1;      
    protected boolean waitingForSync = false;

    public TiltAdapterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        Level level = getLevel();
        if (level == null || level.isClientSide) return;

        //Dead reckoning
        if (isMoving && !waitingForSync && moveStartTime != -1) {
            long ticksExisted = level.getGameTime() - moveStartTime;
            float speed = Math.abs(getTheoreticalSpeed());
            float degreesPerTick = KineticBlockEntity.convertToAngular(speed);
            
            float traveled = ticksExisted * degreesPerTick;
            float totalDistance = Math.abs(targetAngle - startAngle);
            if (traveled > totalDistance) traveled = totalDistance;

            currentAngle = startAngle + (traveled * moveDirection);

            if (ticksExisted >= predictedDuration) {
                finishMovement();
            }
        }

        checkRedstoneAndSpeed();

        if (waitingForSync) {
            executeNewMove();
        }
    }

    public int getLeft() { return redstoneLeft; }
    public int getRight() { return redstoneRight; }

    private void checkRedstoneAndSpeed() {
        Level level = getLevel();
        if (level == null) return;

        BlockState state = getBlockState();
        Axis axis = state.getValue(RotatedPillarKineticBlock.AXIS);
        boolean positiveDir = state.getValue(TiltAdapterBlock.POSITIVE);
        boolean alignedX = state.getValue(TiltAdapterBlock.ALIGNED_X);

        Direction posSignalSide;
        Direction negSignalSide;

        if (axis == Axis.X) {
            posSignalSide = Direction.SOUTH;
            negSignalSide = Direction.NORTH;
        } else if (axis == Axis.Z) {
            posSignalSide = Direction.WEST;
            negSignalSide = Direction.EAST;
        } else {
            if (alignedX) {
                posSignalSide = Direction.NORTH;
                negSignalSide = Direction.SOUTH;
            } else {
                posSignalSide = Direction.EAST;
                negSignalSide = Direction.WEST;
            }
        }

        if (!positiveDir) {
            Direction temp = posSignalSide;
            posSignalSide = negSignalSide;
            negSignalSide = temp;
        }

        int newLeft = level.getSignal(worldPosition.relative(posSignalSide), posSignalSide);
        int newRight = level.getSignal(worldPosition.relative(negSignalSide), negSignalSide);
        if (newLeft != redstoneLeft || newRight != redstoneRight) {
            redstoneLeft = newLeft;
            redstoneRight = newRight;
            sendData();
        }

        if (getTheoreticalSpeed() == 0) {
            if (isMoving) forceStop();
            return;
        }

        int diff = redstoneLeft - redstoneRight; 
        double newTarget = (diff / SIGNAL_RANGE) * PropulsionConfig.TILT_ADAPTER_ANGLE_RANGE.get();
        boolean targetChanged = Math.abs(newTarget - targetAngle) > 0.001f;
        
        if (targetChanged) {
            targetAngle = (float)newTarget;
            triggerSyncFlush();
        }
    }

    private void triggerSyncFlush() {
        waitingForSync = true;
        startAngle = currentAngle; 
        
        isMoving = false;
        moveDirection = 0;
        moveStartTime = -1;
        predictedDuration = -1;
        
        sequenceContext = null;

        updateDownstreamNetwork();
    }

    private void executeNewMove() {
        waitingForSync = false;
        float delta = targetAngle - startAngle;
        Level level = getLevel();
        if (level == null) return;

        isMoving = true;
        moveDirection = (int) Math.signum(delta);
        moveStartTime = level.getGameTime();
        
        float absDelta = Math.abs(delta);
        float speed = Math.abs(getTheoreticalSpeed());

        double kineticSpeed = speed * moveDirection;
        sequenceContext = new SequenceContext(SequencerInstructions.TURN_ANGLE, kineticSpeed == 0.0 ? 0.0 : Math.abs(delta) / kineticSpeed);
        ((ISnappingSequenceContext) (Object) sequenceContext).setSnapToZero(true);
        
        double degreesPerTick = KineticBlockEntity.convertToAngular(speed);
        predictedDuration = (int) Math.ceil(absDelta / degreesPerTick) + 2;

        updateDownstreamNetwork();
    }

    private void finishMovement() {
        isMoving = false;
        moveDirection = 0;
        moveStartTime = -1;
        predictedDuration = -1;
        
        currentAngle = targetAngle;
        startAngle = targetAngle;
        
        sequenceContext = null; 
        
        updateDownstreamNetwork();
    }

    private void forceStop() {
        isMoving = false;
        moveDirection = 0;
        sequenceContext = null;
        waitingForSync = false;
        updateDownstreamNetwork();
    }

    private void updateDownstreamNetwork() {
        detachKinetics();
        attachKinetics();
        sendData();
    }

    @Override
    public float propagateRotationTo(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs) {
        Direction directionToTarget = Direction.getNearest(diff.getX(), diff.getY(), diff.getZ());
        Direction backFace = getBackFace(stateFrom);
        if (directionToTarget == backFace) { return 0; }
        return super.propagateRotationTo(target, stateFrom, stateTo, diff, connectedViaAxes, connectedViaCogs);
    }

   @Override
    public float getRotationSpeedModifier(Direction face) {
        //Prevent receiving from the front side
        if (hasSource()) {
            Direction sourceFace = getSourceFacing();
            Direction backFace = getBackFace(getBlockState());
            if (sourceFace != backFace) {
                return 0;
            }
        }

        //Source is valid
        if (hasSource() && face != getSourceFacing()) {
            if (waitingForSync) return 0;
            return moveDirection;
        }
        return 1;
    }

    private Direction getBackFace(BlockState state) {
        Axis axis = state.getValue(RotatedPillarKineticBlock.AXIS);
        boolean positive = state.hasProperty(TiltAdapterBlock.POSITIVE) ? state.getValue(TiltAdapterBlock.POSITIVE) : true;
        Direction.AxisDirection backDir = positive ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE;
        return Direction.get(backDir, axis);
    }

    @Override
    public void onSpeedChanged(float previousSpeed) {
        super.onSpeedChanged(previousSpeed);
        if (isMoving) {
            triggerSyncFlush();
        }
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        super.initialize();
        if (!level.isClientSide) {
            triggerSyncFlush();
        }
    }

    //NBT slop

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {
        compound.putFloat("currentAngle", currentAngle);
        compound.putFloat("startAngle", startAngle);
        compound.putFloat("targetAngle", targetAngle);
        compound.putBoolean("isMoving", isMoving);
        compound.putInt("moveDirection", moveDirection);
        compound.putInt("redstoneLeft", redstoneLeft);
        compound.putInt("redstoneRight", redstoneRight);
        super.write(compound, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        currentAngle = compound.getFloat("currentAngle");
        startAngle = compound.getFloat("startAngle");
        targetAngle = compound.getFloat("targetAngle");
        isMoving = compound.getBoolean("isMoving");
        moveDirection = compound.getInt("moveDirection");
        redstoneLeft = compound.getInt("redstoneLeft");
        redstoneRight = compound.getInt("redstoneRight");
        moveStartTime = -1; 
        super.read(compound, clientPacket);
    }

    //CONSUME THE CONTEXT
    @Override
    protected void copySequenceContextFrom(KineticBlockEntity sourceBE) {}

    @Override
    protected boolean syncSequenceContext() {
        return true;
    }
}