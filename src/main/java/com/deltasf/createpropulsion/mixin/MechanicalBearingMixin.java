package com.deltasf.createpropulsion.mixin;

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
    @Shadow(remap = false) protected ScrollOptionBehaviour<MechanicalBearingBlockEntity.RotationMode> movementMode;
    @Shadow(remap = false) protected float angle;
    @Shadow(remap = false) protected boolean running;
    @Shadow(remap = false) protected boolean assembleNextTick;

    @Unique private boolean shouldSnapOnStop = false;

    public MechanicalBearingMixin(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void yourMod$captureSnapFlag(CallbackInfo ci) {
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
    private void yourMod$onTickSnapCheck(CallbackInfo ci) {
        Level level = getLevel();
        if (level == null || level.isClientSide) return;
        if (movementMode.get() != MechanicalBearingBlockEntity.RotationMode.ROTATE_NEVER_PLACE) { return; }
        if (getSpeed() != 0) { return; }

        if (shouldSnapOnStop) {
            shouldSnapOnStop = false;
            float absAngle = Math.abs(angle);
            float threshold = 1.75f; //Must be less than 1/15 * TiltAdapterBlockEntity.MAX_ANGLE
            
            if (absAngle < threshold || absAngle > (360 - threshold)) {
                angle = 0;
                setChanged();
                sendData(); 
            }
        }
    }
}
