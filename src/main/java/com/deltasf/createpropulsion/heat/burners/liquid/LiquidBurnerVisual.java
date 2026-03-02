package com.deltasf.createpropulsion.heat.burners.liquid;

import java.util.function.Consumer;

import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlock;
import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public class LiquidBurnerVisual extends AbstractBlockEntityVisual<LiquidBurnerBlockEntity> implements SimpleDynamicVisual {
    private final TransformedInstance fan;
    private final Direction direction;
    private final PoseStack ms;

    public LiquidBurnerVisual(VisualizationContext context, LiquidBurnerBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        direction = blockState.getValue(AbstractBurnerBlock.FACING).getOpposite();
        fan = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(PropulsionPartialModels.LIQUID_BURNER_FAN)).createInstance();
        ms = new PoseStack();
        animate();
    }

    @Override
    public void beginFrame(DynamicVisual.Context ctx) { animate(); }

    private void animate() {
        Level level = blockEntity.getLevel();
        if (level == null) return;

        float time = AnimationTickHolder.getRenderTime(level);
        if (blockEntity.lastRenderTime == -1) blockEntity.lastRenderTime = time;
        float dt = time - blockEntity.lastRenderTime;
        blockEntity.lastRenderTime = time;

        if (blockEntity.isFanSpinning()) {
            blockEntity.fanAngle += dt * LiquidBurnerRenderer.FAN_SPEED;
            blockEntity.fanAngle %= (float) (Math.PI * 2);
        }

        ms.pushPose();
        
        var msr = TransformStack.of(ms);
        msr.translate(getVisualPosition());
        
        ms.translate(0.5, 0.5, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(direction)));

        ms.translate(0, -2/16.0f, 0);
        ms.mulPose(Axis.XP.rotation(-blockEntity.fanAngle));
        ms.translate(0, 2/16.0f, 0);
        
        ms.translate(-0.5, -0.5, -0.5);

        fan.setTransform(ms).setChanged();
        
        ms.popPose();
    }

    @Override
    public void updateLight(float partialTick) { relight(fan); }

    @Override
    protected void _delete() { fan.delete(); }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) { consumer.accept(fan); }
}
