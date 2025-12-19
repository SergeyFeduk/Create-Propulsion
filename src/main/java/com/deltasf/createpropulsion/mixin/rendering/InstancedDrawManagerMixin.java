package com.deltasf.createpropulsion.mixin.rendering;

import com.deltasf.createpropulsion.propeller.rendering.PropellerRenderControl;
import dev.engine_room.flywheel.backend.engine.instancing.InstancedDrawManager;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.List;

@Mixin(InstancedDrawManager.class)
public class InstancedDrawManagerMixin {

    /// This mixin forces draws to be filtered through render control class so they are actually skipped. 

    @Shadow(remap = false) private List<?> draws;
    @Shadow(remap = false) private List<?> oitDraws;

    @Redirect(method = "submitDraws", at = @At(value = "FIELD", target = "Ldev/engine_room/flywheel/backend/engine/instancing/InstancedDrawManager;draws:Ljava/util/List;", opcode = Opcodes.GETFIELD), remap = false)
    private List<?> createpropulsion$filterDraws(InstancedDrawManager instance) {
        return PropellerRenderControl.filterDraws(this.draws);
    }
    
    @Redirect(method = "submitOitDraws", at = @At(value = "FIELD", target = "Ldev/engine_room/flywheel/backend/engine/instancing/InstancedDrawManager;oitDraws:Ljava/util/List;", opcode = Opcodes.GETFIELD), remap = false)
    private List<?> createpropulsion$filterOitDraws(InstancedDrawManager instance) {
        return PropellerRenderControl.filterDraws(this.oitDraws);
    }
}