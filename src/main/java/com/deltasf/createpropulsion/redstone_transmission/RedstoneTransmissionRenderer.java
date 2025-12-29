package com.deltasf.createpropulsion.redstone_transmission;

import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import static com.deltasf.createpropulsion.redstone_transmission.RedstoneTransmissionBlock.SHIFT_LEVEL;

public class RedstoneTransmissionRenderer extends SplitShaftRenderer {
    public RedstoneTransmissionRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(SplitShaftBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        if (VisualizationManager.supportsVisualization(be.getLevel()) || !(be instanceof RedstoneTransmissionBlockEntity rtbe)) return;

        Direction facing = be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        int shift_up = be.getLevel().getSignal(be.getBlockPos().relative(facing.getCounterClockWise()), facing.getCounterClockWise());
        int shift_down = be.getLevel().getSignal(be.getBlockPos().relative(facing.getClockWise()), facing.getClockWise());

        SuperByteBuffer partial_plus = CachedBuffers.partial(PropulsionPartialModels.TRANSMISSION_PLUS, be.getBlockState());
        SuperByteBuffer partial_minus = CachedBuffers.partial(PropulsionPartialModels.TRANSMISSION_MINUS, be.getBlockState());
        SuperByteBuffer dialBuffer = CachedBuffers.partial(AllPartialModels.GAUGE_DIAL, be.getBlockState());

        dialBuffer
                .rotateCentered((float) ((-facing.toYRot() - 90) / 180 * Math.PI), Direction.UP)
                .translate(2f / 16, 5.75f / 16, 5.75f / 16)
                .rotate(rtbe.getGaugeTarget(partialTicks), Direction.EAST)
                .translate(0, -5.75f / 16, -5.75f / 16)
                .light(light)
                .renderInto(ms, buffer.getBuffer(RenderType.solid()));

        partial_plus.light(light).overlay(overlay).color(Color.mixColors(0x470102, 0xCD0000, shift_up / 15f)).renderInto(ms, buffer.getBuffer(RenderType.cutout()));
        partial_minus.light(light).overlay(overlay).color(Color.mixColors(0x470102, 0xCD0000, shift_down / 15f)).renderInto(ms, buffer.getBuffer(RenderType.cutout()));
    }
}
