package com.deltasf.createpropulsion.mixin.rendering;

import com.deltasf.createpropulsion.propeller.rendering.PropellerRenderControl;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.impl.event.RenderContextImpl;
import dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderBuffers;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    //This mixin invokes second instancing render pass after translucent world geometry. This pass renders only those draws, that were skipped during first render pass

    @Shadow @Nullable private ClientLevel level;
    @Shadow @Final private RenderBuffers renderBuffers;

    @Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=particles"))
    private void createpropulsion$renderAfterTranslucent(PoseStack poseStack, float partialTick, long finishNanoTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        VisualizationManager manager = VisualizationManager.get(level);
        if (manager instanceof VisualizationManagerImpl impl) {
            RenderContextImpl context = RenderContextImpl.create((LevelRenderer)(Object)this, level, renderBuffers, poseStack, projectionMatrix, camera, partialTick);
            PropellerRenderControl.renderStage = 1; //Translucent mode
            try {
                //Force second render
                var engine = impl.getEngineImpl();
                if (engine != null) {
                    engine.render(context);
                }
            } finally {
                PropellerRenderControl.renderStage = 0; //Default mode
            }
        }
    }
}