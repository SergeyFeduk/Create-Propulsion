package com.deltasf.createpropulsion.mixin.rendering;

import com.deltasf.createpropulsion.propeller.rendering.PropellerRenderControl;
import dev.engine_room.flywheel.backend.engine.AbstractInstancer;
import dev.engine_room.flywheel.backend.engine.DrawManager;
import dev.engine_room.flywheel.backend.engine.EngineImpl;
import dev.engine_room.flywheel.backend.engine.InstancerKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;

@Mixin(EngineImpl.class)
public class EngineImplMixin {

    /// This mixin forces EngineImpl to hide specific draws during first rendering stage

    @Shadow(remap = false) @Final private DrawManager<? extends AbstractInstancer<?>> drawManager;

    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void createpropulsion$preRender(dev.engine_room.flywheel.api.backend.RenderContext context, CallbackInfo ci) {
        Map<InstancerKey<?>, AbstractInstancer<?>> instancers = 
            (Map<InstancerKey<?>, AbstractInstancer<?>>) ((DrawManagerAccessor) drawManager).createpropulsion$getInstancers();

        for (Map.Entry<InstancerKey<?>, AbstractInstancer<?>> entry : instancers.entrySet()) {
            if (PropellerRenderControl.shouldSkip(entry.getKey())) {
                PropellerRenderControl.hideInstancer(entry.getValue());
            }
        }
    }

    @Inject(method = "render", at = @At("RETURN"), remap = false)
    private void createpropulsion$postRender(dev.engine_room.flywheel.api.backend.RenderContext context, CallbackInfo ci) {
        PropellerRenderControl.hiddenDraws.clear();
    }
}