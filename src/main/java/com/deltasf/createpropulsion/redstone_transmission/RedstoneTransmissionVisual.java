package com.deltasf.createpropulsion.redstone_transmission;

import com.deltasf.createpropulsion.redstone_transmission.RedstoneTransmissionBlockEntity.TransmissionMode;
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

import java.util.function.Consumer;

import static com.deltasf.createpropulsion.redstone_transmission.RedstoneTransmissionBlock.HORIZONTAL_FACING;
import static com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock.AXIS;

public class RedstoneTransmissionVisual extends SplitShaftVisual implements SimpleDynamicVisual {
    private OrientedInstance minus;
    private OrientedInstance plus;
    private TransformedInstance hand;

    private PoseStack ms;

    public RedstoneTransmissionVisual(VisualizationContext modelManager, SplitShaftBlockEntity blockEntity, float partialTick) {
        super(modelManager, blockEntity, partialTick);

        Direction facing = blockEntity.getBlockState().getValue(HORIZONTAL_FACING);

        minus = instancerProvider().instancer(InstanceTypes.ORIENTED, Models.partial(PropulsionPartialModels.TRANSMISSION_MINUS)).createInstance();
        plus = instancerProvider().instancer(InstanceTypes.ORIENTED, Models.partial(PropulsionPartialModels.TRANSMISSION_PLUS)).createInstance();
        hand = instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.GAUGE_DIAL)).createInstance();

        ms = new PoseStack();
        var msr = TransformStack.of(ms);
        msr.translate(getVisualPosition());
        msr.pushPose();
        msr.rotateCenteredDegrees(-facing.toYRot() - 90, Direction.UP);
        if(blockEntity.getBlockState().getValue(AXIS).isHorizontal()) {
            minus = minus.rotateTo(Direction.SOUTH, Direction.UP);
            plus = plus.rotateTo(Direction.SOUTH, Direction.UP);
            msr.rotateCenteredDegrees(90, Direction.Axis.Z);
        }
        msr.translate(2f / 16, 0, 0);

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

        Direction facing = blockEntity.getBlockState().getValue(HORIZONTAL_FACING);
        int shift_up = rtbe.get_shift_up();
        int shift_down = rtbe.get_shift_down();
        //In direct mode both plus and minus sides control the same thing, so they should have the same redstone tint
        if (rtbe.controlMode.get() == TransmissionMode.DIRECT) {
            int max_shift = Math.max(shift_up, shift_down);
            shift_up = max_shift;
            shift_down = max_shift;
        }

        Color up_color = new Color(Color.mixColors(0x470102, 0xCD0000, shift_up / 15f));
        Color down_color = new Color(Color.mixColors(0x470102, 0xCD0000, shift_down / 15f));

        minus.color(down_color.getRed(), down_color.getGreen(), down_color.getBlue()).setChanged();
        plus.color(up_color.getRed(), up_color.getGreen(), up_color.getBlue()).setChanged();

        var msr = TransformStack.of(ms);
        msr.pushPose();

        float dialPivot = 5.75f / 16;

        msr.rotateCenteredDegrees(-facing.toYRot() - 90, Direction.UP);
        if(rtbe.getBlockState().getValue(AXIS).isHorizontal()) {
            msr.rotateCenteredDegrees(90, Direction.Axis.Z);
        }
        msr.translate(2f / 16, dialPivot, dialPivot)
                .rotate(rtbe.getGaugeTarget(context.partialTick()), Direction.EAST)
                .translate(0, -dialPivot, -dialPivot);;

        hand.setTransform(ms).setChanged();

        msr.popPose();
    }
}
