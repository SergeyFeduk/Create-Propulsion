package com.deltasf.createpropulsion.mixin.feature.tilt_adapter;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.tilt_adapter.ISnappingSequenceContext;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MechanicalBearingBlockEntity.class)
public abstract class MechanicalBearingMixin extends KineticBlockEntity {

    ///This mixin forces mechanical bearing to snap to angle = 0 when it is in downstream network related to tilt_adapter (and therefore uses sequence context emitted by tilt adapter)
    ///Snapping is performed within angular range produced by one redstone signal level of tilt adapter with current settings

    @Shadow(remap = false) protected ScrollOptionBehaviour<MechanicalBearingBlockEntity.RotationMode> movementMode;
    @Shadow(remap = false) protected float angle;
    @Shadow(remap = false) protected boolean running;
    @Shadow(remap = false) protected boolean assembleNextTick;
    @Shadow(remap = false) protected double sequencedAngleLimit;

    @Unique private boolean shouldSnapOnStop = false;
    @Unique private double capturedOldLimit = -1.0;

    public MechanicalBearingMixin(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Inject(method = "onSpeedChanged", at = @At("HEAD"), remap = false)
    private void captureOldLimit(float prevSpeed, CallbackInfo ci) {
        if (sequenceContext != null && ((Object)sequenceContext) instanceof ISnappingSequenceContext snap && snap.shouldSnapToZero()) {
            capturedOldLimit = this.sequencedAngleLimit;
        } else {
            capturedOldLimit = -1.0;
        }
    }

    @Inject(method = "onSpeedChanged", at = @At("TAIL"), remap = false)
    private void fixLimitReset(float prevSpeed, CallbackInfo ci) {
        if (capturedOldLimit >= 0 && sequenceContext != null && ((Object)sequenceContext) instanceof ISnappingSequenceContext snap && snap.shouldSnapToZero()) {
            this.sequencedAngleLimit = capturedOldLimit;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void captureSnapFlag(CallbackInfo ci) {
        Level level = getLevel();
        if (level == null || level.isClientSide) return;
        if (sequenceContext == null) return;
        //Warcrime
        if (((Object)sequenceContext) instanceof ISnappingSequenceContext snapCtx) {
            shouldSnapOnStop = snapCtx.shouldSnapToZero();
        } else {
            shouldSnapOnStop = false;
        }
    }

    @Inject(
        method = "tick", 
        at = @At(
            value = "FIELD", 
            target = "Lcom/simibubi/create/content/contraptions/bearing/MechanicalBearingBlockEntity;assembleNextTick:Z", 
            opcode = Opcodes.PUTFIELD, 
            shift = At.Shift.AFTER,
            remap = false
        ),
        remap = false
    )
    private void onTickSnapCheck(CallbackInfo ci) {
        Level level = getLevel();
        if (level == null || level.isClientSide) return;
        if (movementMode.get() != MechanicalBearingBlockEntity.RotationMode.ROTATE_NEVER_PLACE) { return; }
        if (getSpeed() != 0) { return; }

        if (shouldSnapOnStop) {
            shouldSnapOnStop = false;
            float absAngle = Math.abs(angle);

            float tiltAdapterRange = PropulsionConfig.TILT_ADAPTER_ANGLE_RANGE.get().floatValue();
            float threshold = Math.max(0.333f, (1 / 15.0f * tiltAdapterRange) - 0.25f);
            
            if (absAngle < threshold || absAngle > (360 - threshold)) {
                angle = 0;
                setChanged();
                sendData(); 
            }
        }
    }
}
