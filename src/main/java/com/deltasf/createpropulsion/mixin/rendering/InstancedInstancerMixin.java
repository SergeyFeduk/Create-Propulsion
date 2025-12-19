package com.deltasf.createpropulsion.mixin.rendering;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.deltasf.createpropulsion.accessors.IInstancedInstancerExposer;

import java.util.List;

@Mixin(targets = "dev.engine_room.flywheel.backend.engine.instancing.InstancedInstancer", remap = false)
public abstract class InstancedInstancerMixin implements IInstancedInstancerExposer {

    @Shadow
    abstract List<?> draws();

    @Override
    public List<?> createpropulsion$getDraws() {
        return this.draws();
    }
}

