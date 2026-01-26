package com.deltasf.createpropulsion.balloons.injectors.hot_air_pump;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.balloons.particles.BalloonParticleSystem;
import com.deltasf.createpropulsion.balloons.particles.ShipParticleHandler;
import com.deltasf.createpropulsion.balloons.registries.ClientBalloonRegistry;
import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.deltasf.createpropulsion.registries.PropulsionSpriteShifts;
import com.deltasf.createpropulsion.utility.math.MathUtility;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class HotAirPumpRenderer extends KineticBlockEntityRenderer<HotAirPumpBlockEntity>  {
    private static final float TEXTURE_SIZE = 64.0f;
    private static final float V_PIXEL_TOP = 8.75f;
    private static final float V_PIXEL_BOTTOM = 11.00f; 
    private static final float PIXELS_PER_Y = (V_PIXEL_BOTTOM - V_PIXEL_TOP) / (22.0f - 13.0f);

    private static final float MIN_INTERPOLATION_VALUE = 0.53f;
    private static final float MAX_INTERPOLATION_VALUE = 1f;

    private static final float MAX_VISUAL_SPEED = 0.75f; 
    private static final float MAX_RPM = 256.0f;
    private static final float FAN_SPEED_MULTIPLIER = 1.0f;
    private static final float MIN_VISUAL_SPEED = 0.075f;

    private static final float MEMBRANE_DECAY = 0.2f;

    private static final float PARTICLE_SPAWN_MULTIPLIER = 0.15f; 

    public HotAirPumpRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(HotAirPumpBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;
        
        BlockState state = be.getBlockState();

        SuperByteBuffer cogModel = CachedBuffers.partialFacing(PropulsionPartialModels.HOT_AIR_PUMP_COG, state, Direction.NORTH);
        SuperByteBuffer fanModel = CachedBuffers.partialFacing(PropulsionPartialModels.LIQUID_BURNER_FAN, state, Direction.NORTH);
        SuperByteBuffer membraneModel = CachedBuffers.partialFacing(PropulsionPartialModels.HOT_AIR_PUMP_MEMBRANE, state, Direction.NORTH);
        SuperByteBuffer meshModel = CachedBuffers.partialFacing(PropulsionPartialModels.HOT_AIR_PUMP_MESH, state, Direction.NORTH);

        VertexConsumer cutoutBuffer = buffer.getBuffer(RenderType.cutout());
        VertexConsumer solidBuffer = buffer.getBuffer(RenderType.solid());
        
        //Time
        float time = AnimationTickHolder.getRenderTime(level);
        if (be.lastRenderTime == -1) be.lastRenderTime = time;
        float dt = time - be.lastRenderTime;
        be.lastRenderTime = time;

        //Cog
        standardKineticRotationTransform(cogModel, be, light).renderInto(ms, solidBuffer);

        //Fan
        float heat = be.getLastHeatConsumed();
        if (heat > 0) {
            be.fanAngle += (heat * FAN_SPEED_MULTIPLIER) * dt;
            be.fanAngle %= (float) (Math.PI * 2);
        }

        fanModel
            .translate(2/16.0f,12/16.0f,0)
            .rotate(Axis.Z, -(float)Math.PI / 2.0f)
            .translate(0.5f,6/16.0f,0.5f)
            .rotate(Axis.X, be.fanAngle)
            .translate(-0.5f,-6/16.0f,-0.5f)
            .light(light)
            .overlay(overlay)
            .renderInto(ms, solidBuffer);

        //Membrane
        float currentRpm = Math.abs(be.getSpeed());
        float targetMembraneSpeed;

        if (currentRpm <= 0) {
            targetMembraneSpeed = 0;
        } else {
            float tRpm = Math.min(currentRpm / MAX_RPM, 1.0f);
            float speedCurve = tRpm * tRpm * tRpm * (tRpm * (tRpm * 6 - 15) + 10); //Smoothstep
            targetMembraneSpeed = MIN_VISUAL_SPEED + (MAX_VISUAL_SPEED - MIN_VISUAL_SPEED) * speedCurve;
        }

        be.membraneSpeed = MathUtility.expDecay(be.membraneSpeed, targetMembraneSpeed, MEMBRANE_DECAY, dt);
        be.membraneTime += be.membraneSpeed * dt;

        float t = MathUtility.sineInRange(be.membraneTime, MIN_INTERPOLATION_VALUE, MAX_INTERPOLATION_VALUE);
        float tRaw = MathUtility.sineInRange(be.membraneTime, 0, MAX_INTERPOLATION_VALUE);

        float scaleY = 0.5f + (0.5f * t);
        float translateY = (2.0f / 16.0f) * (1.0f - t);
        float meshTranslateY = -(9.0f / 16.0f) * (1.0f - tRaw);

        Player player = Minecraft.getInstance().player;
        if (player != null && player.distanceToSqr(be.getBlockPos().getX(), be.getBlockPos().getY(), be.getBlockPos().getZ()) < BalloonParticleSystem.SPAWN_RADIUS_SQ) {
            float deltaT = t - be.clientLastVisualT;
            be.clientLastVisualT = t;
            
            if (deltaT < 0) {
                //Inhale
                double amount = be.getInjectionAmount();
                if (amount > 0) {
                    be.clientParticleBuffer += amount * PARTICLE_SPAWN_MULTIPLIER * dt;
                }
            } else {
                //Exhale
                if (t > 0.95f && be.clientParticleBuffer >= 1.0f) {
                    int count = (int) be.clientParticleBuffer;
                    if (count > 0) {
                        be.clientParticleBuffer -= count;
                        spawnPumpParticles(be, count, level, meshTranslateY);
                    }
                }
            }
        }

        //Membrane
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
        
        meshModel
            .translate(0, meshTranslateY - 0.02f, 0)
            .light(light)
            .overlay(overlay)
            .renderInto(ms, cutoutBuffer);

    }

    private void spawnPumpParticles(HotAirPumpBlockEntity be, int count, Level level, float meshTranslateY) {
        int targetBalloonId = ClientBalloonRegistry.getBalloonIdForHai(be.getId());
        if (targetBalloonId == -1) return;

        Ship ship = VSGameUtilsKt.getShipManagingPos(level, be.getBlockPos());
        if (ship == null) return;

        ShipParticleHandler handler = BalloonParticleSystem.getHandler(ship.getId());
        if (handler == null) return;

        final float Y_MAX = 0.8f + meshTranslateY;
        final float Y_MIN = Y_MAX - 0.1f;
        final float RADIUS = 5.0f / 16.0f;

        double centerX = be.getBlockPos().getX() + 0.5;
        double centerY = be.getBlockPos().getY();
        double centerZ = be.getBlockPos().getZ() + 0.5;

        float speed = Math.min(be.membraneSpeed * 2.666f, 1.5f);
        float life = Math.min(0.8f / speed, 1);

        for (int i = 0; i < count; i++) {
            double localY = Y_MIN + level.random.nextFloat() * (Y_MAX - Y_MIN);
            double offsetX = (level.random.nextFloat() * 2 * RADIUS) - RADIUS;
            double offsetZ = (level.random.nextFloat() * 2 * RADIUS) - RADIUS;

            handler.spawnStream(
                centerX + offsetX, 
                centerY + localY, 
                centerZ + offsetZ, 
                speed,
                0.3f,
                life,
                0.2f,
                targetBalloonId
            );
        }
    }

}
