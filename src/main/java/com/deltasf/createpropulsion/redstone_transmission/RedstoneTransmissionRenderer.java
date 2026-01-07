package com.deltasf.createpropulsion.redstone_transmission;

import com.deltasf.createpropulsion.redstone_transmission.RedstoneTransmissionBlockEntity.TransmissionMode;
import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static com.deltasf.createpropulsion.redstone_transmission.RedstoneTransmissionBlock.HORIZONTAL_FACING;
import static com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock.AXIS;

public class RedstoneTransmissionRenderer extends KineticBlockEntityRenderer<RedstoneTransmissionBlockEntity> {
    public RedstoneTransmissionRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(RedstoneTransmissionBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null || VisualizationManager.supportsVisualization(level)) return;

        Direction facing = be.getBlockState().getValue(HORIZONTAL_FACING);
        BlockState state = be.getBlockState();
        Direction.Axis axis = state.getValue(AXIS);
        BlockPos pos = be.getBlockPos();
        float time = AnimationTickHolder.getRenderTime(level);

        //Shafts
        for (Direction direction : Iterate.directionsInAxis(axis)) {
            float speedToRender;
            if (be.hasSource() && be.getSourceFacing() == direction) {
                speedToRender = getNeighborSpeed(be, direction);
            } else {
                speedToRender = be.getSpeed();
            }

            float angle = (time * speedToRender * 3f / 10f) % 360;
            angle += getRotationOffsetForPosition(be, pos, axis);
            angle = angle / 180f * (float) Math.PI;

            SuperByteBuffer shaftBuffer = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, direction);
            kineticRotationTransform(shaftBuffer, be, axis, angle, light);
            shaftBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()));
        }

        //Plus/minus/gauge
        int shift_up = be.get_shift_up();
        int shift_down = be.get_shift_down();

        SuperByteBuffer partial_plus = CachedBuffers.partial(PropulsionPartialModels.TRANSMISSION_PLUS, be.getBlockState());
        SuperByteBuffer partial_minus = CachedBuffers.partial(PropulsionPartialModels.TRANSMISSION_MINUS, be.getBlockState());
        SuperByteBuffer dialBuffer = CachedBuffers.partial(AllPartialModels.GAUGE_DIAL, be.getBlockState())
            .rotateCentered((float) ((-facing.toYRot() - 90) / 180 * Math.PI), Direction.UP);

        if(be.getBlockState().getValue(AXIS).isHorizontal()) {
            partial_plus = partial_plus.rotateCenteredDegrees(90, Direction.Axis.X);
            partial_minus = partial_minus.rotateCenteredDegrees(90, Direction.Axis.X);
            dialBuffer = dialBuffer.rotateCenteredDegrees(90, Direction.Axis.Z);
        }

        //In direct mode both plus and minus sides control the same thing, so they should have the same redstone tint
        if (be.controlMode.get() == TransmissionMode.DIRECT) {
            int max_shift = Math.max(shift_up, shift_down);
            shift_up = max_shift;
            shift_down = max_shift;
        }

        dialBuffer.translate(2f / 16, 5.75f / 16, 5.75f / 16)
            .rotate(be.getGaugeTarget(partialTicks), Direction.EAST)
            .translate(0, -5.75f / 16, -5.75f / 16)
            .light(light)
            .renderInto(ms, buffer.getBuffer(RenderType.solid()));

        partial_plus
            .rotateCentered((float) (facing.toYRot() / 180 * Math.PI), Direction.UP)
            .light(light)
            .overlay(overlay)
            .color(Color.mixColors(0x470102, 0xCD0000, shift_up / 15f))
            .renderInto(ms, buffer.getBuffer(RenderType.cutout()));

        partial_minus
            .rotateCentered((float) (facing.toYRot() / 180 * Math.PI), Direction.UP)
            .light(light)
            .overlay(overlay)
            .color(Color.mixColors(0x470102, 0xCD0000, shift_down / 15f))
            .renderInto(ms, buffer.getBuffer(RenderType.cutout()));
    }

    public static float getNeighborSpeed(RedstoneTransmissionBlockEntity be, Direction direction) {
        Level level = be.getLevel();
        if (level == null) return 0;
        BlockPos neighborPos = be.getBlockPos().relative(direction);
        BlockEntity neighborBE = level.getBlockEntity(neighborPos);
        if (neighborBE instanceof KineticBlockEntity kbe) {
            return kbe.getSpeed();
        }
        return 0;
    }
}
