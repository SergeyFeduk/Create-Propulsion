package com.deltasf.createpropulsion.balloons.injectors.hot_air_pump;

import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.deltasf.createpropulsion.registries.PropulsionSpriteShifts;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class HotAirPumpRenderer extends KineticBlockEntityRenderer<HotAirPumpBlockEntity>  {
    private static final float TEXTURE_SIZE = 64.0f;
    private static final float V_PIXEL_TOP = 8.75f;
    private static final float V_PIXEL_BOTTOM = 11.00f; 
    private static final float PIXELS_PER_Y = (V_PIXEL_BOTTOM - V_PIXEL_TOP) / (22.0f - 13.0f);

    private static final float MIN_INTERPOLATION_VALUE = 0.53f;
    private static final float MAX_INTERPOLATION_VALUE = 1f;

    public HotAirPumpRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    //TODO: Fan movement (proportionla to heat input) & membrane movement (proportional to RPM)
    @Override
    protected void renderSafe(HotAirPumpBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;

        float time = AnimationTickHolder.getRenderTime(level) / 5.0f;
        float t = sineInRange(time, MIN_INTERPOLATION_VALUE, MAX_INTERPOLATION_VALUE);
        float tRaw = sineInRange(time, 0, MAX_INTERPOLATION_VALUE);
        
        BlockState state = be.getBlockState();

        SuperByteBuffer cogModel = CachedBuffers.partialFacing(PropulsionPartialModels.HOT_AIR_PUMP_COG, state, Direction.NORTH);
        SuperByteBuffer fanModel = CachedBuffers.partialFacing(PropulsionPartialModels.LIQUID_BURNER_FAN, state, Direction.NORTH);
        SuperByteBuffer membraneModel = CachedBuffers.partialFacing(PropulsionPartialModels.HOT_AIR_PUMP_MEMBRANE, state, Direction.NORTH);
        SuperByteBuffer meshModel = CachedBuffers.partialFacing(PropulsionPartialModels.HOT_AIR_PUMP_MESH, state, Direction.NORTH);

        VertexConsumer cutoutBuffer = buffer.getBuffer(RenderType.cutout());
        VertexConsumer solidBuffer = buffer.getBuffer(RenderType.solid());
        
        //Cog
        standardKineticRotationTransform(cogModel, be, light).renderInto(ms, solidBuffer);

        //Fan
        fanModel
            .translate(2/16.0f,12/16.0f,0)
            .rotate(Axis.Z, -(float)Math.PI / 2.0f)
            .translate(0.5f,6/16.0f,0.5f)
            .rotate(Axis.X, time)
            .translate(-0.5f,-6/16.0f,-0.5f)
            .light(light)
            .overlay(overlay)
            .renderInto(ms, solidBuffer);

        //Membrane
        float scaleY = 0.5f + (0.5f * t);
        float translateY = (2.0f / 16.0f) * (1.0f - t);
        SpriteShiftEntry worldSpaceShift = new SpriteShiftEntry() {
            @Override
            public float getTargetU(float globalU) { return PropulsionSpriteShifts.HOT_AIR_PUMP.getTargetU(globalU); }

            @Override
            public float getTargetV(float globalV) {
                TextureAtlasSprite sprite = PropulsionSpriteShifts.HOT_AIR_PUMP.getOriginal();
                float v0 = sprite.getV0();
                float vRange = sprite.getV1() - v0;
                float localVPixel = ((globalV - v0) / vRange) * TEXTURE_SIZE;
                float originalY = 22.0f - (localVPixel - V_PIXEL_TOP) / PIXELS_PER_Y;
                float newY = (originalY * scaleY) + (translateY * 16.0f);
                float targetVPixel = V_PIXEL_TOP + (22.0f - newY) * PIXELS_PER_Y;
                return v0 + (targetVPixel / TEXTURE_SIZE) * vRange;
            }
        };

        membraneModel
            .translate(0, translateY, 0)
            .scale(1, scaleY, 1)
            .shiftUV(worldSpaceShift)
            .light(light)
            .overlay(overlay)
            .renderInto(ms, cutoutBuffer);

        //Mesh
        float meshTranslateY = -(9.0f / 16.0f) * (1.0f - tRaw);
        
        meshModel
            .translate(0, meshTranslateY - 0.02f, 0)
            .light(light)
            .overlay(overlay)
            .renderInto(ms, cutoutBuffer);

    }

    public static float sineInRange(float time, float bottom, float top) {
        float sin = (float) Math.sin(time);
        float normalized = (sin + 1.0f) * 0.5f;
        return bottom + normalized * (top - bottom);
    }
}
