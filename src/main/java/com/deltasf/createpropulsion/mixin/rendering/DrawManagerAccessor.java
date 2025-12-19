package com.deltasf.createpropulsion.mixin.rendering;

import dev.engine_room.flywheel.backend.engine.AbstractInstancer;
import dev.engine_room.flywheel.backend.engine.DrawManager;
import dev.engine_room.flywheel.backend.engine.InstancerKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(DrawManager.class)
public interface DrawManagerAccessor {
    @Accessor(value = "instancers", remap = false)
    Map<InstancerKey<?>, AbstractInstancer<?>> createpropulsion$getInstancers();
}
