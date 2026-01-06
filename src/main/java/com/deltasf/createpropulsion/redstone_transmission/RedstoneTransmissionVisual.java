package com.deltasf.createpropulsion.redstone_transmission;

import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import net.createmod.catnip.theme.Color;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.function.Consumer;

public class RedstoneTransmissionVisual extends SplitShaftVisual implements SimpleDynamicVisual {

    OrientedInstance minus;
    OrientedInstance plus;
    TransformedInstance hand;

    PoseStack ms;

    public RedstoneTransmissionVisual(VisualizationContext modelManager, SplitShaftBlockEntity blockEntity, float partialTick) {
        super(modelManager, blockEntity, partialTick);

        Direction facing = blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);

        ms = new PoseStack();
        var msr = TransformStack.of(ms);
        msr.translate(getVisualPosition());
        msr.pushPose();
        msr.center().rotateTo(Direction.EAST, facing).translate(2f / 16, 0, 0).uncenter();

        minus = instancerProvider().instancer(InstanceTypes.ORIENTED, Models.partial(PropulsionPartialModels.TRANSMISSION_MINUS)).createInstance();
        plus = instancerProvider().instancer(InstanceTypes.ORIENTED, Models.partial(PropulsionPartialModels.TRANSMISSION_PLUS)).createInstance();
        hand = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.GAUGE_DIAL)).createInstance();

        minus.rotateTo(Direction.SOUTH, facing).position(getVisualPosition()).setChanged();
        plus.rotateTo(Direction.SOUTH, facing).position(getVisualPosition()).setChanged();
        hand.setTransform(ms).setChanged();

        msr.popPose();
    }

    @Override
    protected void _delete() {
        super._delete();
        minus.delete();
        plus.delete();
        hand.delete();
    }

    @Override
    public void updateLight(float partialTick) {
        super.updateLight(partialTick);
        relight(minus, plus, hand);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        super.collectCrumblingInstances(consumer);
        consumer.accept(minus);
        consumer.accept(plus);
        consumer.accept(hand);
    }

    @Override
    public void beginFrame(Context context) {

        if (!(blockEntity instanceof RedstoneTransmissionBlockEntity rtbe)) return;

        Direction facing = blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        int shift_up = rtbe.get_shift_up();
        int shift_down = rtbe.get_shift_down();
        Color up_color = new Color(Color.mixColors(0x470102, 0xCD0000, shift_up / 15f));
        Color down_color = new Color(Color.mixColors(0x470102, 0xCD0000, shift_down / 15f));

        minus.color(down_color.getRed(), down_color.getGreen(), down_color.getBlue()).setChanged();
        plus.color(up_color.getRed(), up_color.getGreen(), up_color.getBlue()).setChanged();

        var msr = TransformStack.of(ms);
        msr.pushPose();

        float dialPivot = 5.75f / 16;

        msr.center().rotateTo(Direction.EAST, facing).translate(2f / 16, 0, 0).uncenter()
                .translate(0, dialPivot, dialPivot)
                .rotate(rtbe.getGaugeTarget(context.partialTick()), Direction.EAST)
                .translate(0, -dialPivot, -dialPivot);;

        hand.setTransform(ms).setChanged();

        msr.popPose();
    }
}
