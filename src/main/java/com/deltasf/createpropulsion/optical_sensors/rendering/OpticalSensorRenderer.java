package com.deltasf.createpropulsion.optical_sensors.rendering;

import javax.annotation.Nonnull;

import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.deltasf.createpropulsion.optical_sensors.AbstractOpticalSensorBlock;
import com.deltasf.createpropulsion.optical_sensors.AbstractOpticalSensorBlockEntity;
import com.deltasf.createpropulsion.registries.PropulsionBlockEntities;
import com.deltasf.createpropulsion.registries.PropulsionItems;
import com.deltasf.createpropulsion.registries.PropulsionRenderTypes;
import com.deltasf.createpropulsion.utility.TranslucentBeamRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;

public class OpticalSensorRenderer extends SafeBlockEntityRenderer<AbstractOpticalSensorBlockEntity>{
    
    public OpticalSensorRenderer(BlockEntityRendererProvider.Context context) { super();}

    private static final float RAY_THICKNESS = 0.25f; // 4 pixels wide
    private static final float START_ALPHA = 0.3f;
    private static final float END_ALPHA = 0.0f;
    private static final Vector4f RAY_COLOR = new Vector4f(0.8f, 0.1f, 0.1f, 1.0f);
    private static final Vector4f RAY_POWERED_COLOR = new Vector4f(0.1f, 0.8f, 0.1f, 1.0f);

    //#region Reusable variables for to make it almost-zero alloc code yay

    // Colors
    private static final Vector4f START_COLOR = new Vector4f(
        RAY_COLOR.x(), RAY_COLOR.y(), RAY_COLOR.z(), RAY_COLOR.w() * START_ALPHA
    );
    private static final Vector4f START_POWERED_COLOR = new Vector4f(
        RAY_POWERED_COLOR.x(), RAY_POWERED_COLOR.y(), RAY_POWERED_COLOR.z(), RAY_POWERED_COLOR.w() * START_ALPHA
    );
    private static final Vector4f END_COLOR = new Vector4f(
        RAY_COLOR.x(), RAY_COLOR.y(), RAY_COLOR.z(), RAY_COLOR.w() * END_ALPHA
    );
    private static final Vector4f END_POWERED_COLOR = new Vector4f(
        RAY_POWERED_COLOR.x(), RAY_POWERED_COLOR.y(), RAY_POWERED_COLOR.z(), RAY_POWERED_COLOR.w() * END_ALPHA
    );

    private static final Vector3f MODIFIED_TEMP_COLOR = new Vector3f();
    private static final Vector4f MODIFIED_START_COLOR = new Vector4f();
    private static final Vector4f MODIFIED_END_COLOR = new Vector4f();

    // Positions
    private final Vector3f localStartPos = new Vector3f();
    private final Vector3f directionVec = new Vector3f();
    private final Vector3f localEndPos = new Vector3f();
    private final Vector3f worldUp = new Vector3f();
    private final Vector3f sideVector = new Vector3f();
    private final Vector3f upVector = new Vector3f();

    // Corner offsets
    private final Vector3f offset_BL = new Vector3f(); // Bottom-Left offset
    private final Vector3f offset_BR = new Vector3f(); // Bottom-Right offset
    private final Vector3f offset_TR = new Vector3f(); // Top-Right offset
    private final Vector3f offset_TL = new Vector3f(); // Top-Left offset

    // AABB Calculation Vectors
    private static final float HALF_THICKNESS = RAY_THICKNESS * 0.5f;
    private final Vector3d worldStart = new Vector3d();
    private final Vector3d worldEnd = new Vector3d();

    //#endregion

    @Override
    protected void renderSafe(AbstractOpticalSensorBlockEntity blockEntity, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay) {
        //Filtering if we are OPTICAL_SENSOR_BLOCK_ENTITY
        if (blockEntity.getType() == PropulsionBlockEntities.OPTICAL_SENSOR_BLOCK_ENTITY.get()) {
            FilteringRenderer.renderOnBlockEntity(blockEntity, partialTicks, poseStack, bufferSource, light, overlay);
        }
        //Skip rendering if there are invisibility lens inserted
        if (blockEntity.hasLens(PropulsionItems.INVISIBILITY_LENS.get())) return; //Invisibiliy lens -> no beam rendering for ya

        BeamRenderData beamData = blockEntity.getClientBeamRenderData();
            
        //Laser beam
        float distance = blockEntity.getRaycastDistance();
        if (distance <= 1e-6f) return; // Same position case
        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(BlockStateProperties.FACING);
        boolean powered = state.getValue(AbstractOpticalSensorBlock.POWERED);

        //Get start and end pos
        this.directionVec.set(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        this.localStartPos.set(this.directionVec);
        this.localStartPos.mul(blockEntity.getZAxisOffset()); 
        this.localStartPos.add(0.5f, 0.5f, 0.5f);

        this.directionVec.mul(distance, this.localEndPos);
        this.localEndPos.add(this.localStartPos);

        //Ray shape calculation
        this.worldUp.set(0, 1, 0);
        if (Math.abs(this.directionVec.dot(this.worldUp)) > 0.99f) {
            this.worldUp.set(1, 0, 0);
        }

        // Calculate perpendicular vectors
        this.directionVec.cross(this.worldUp, this.sideVector).normalize();; // side = ray x up
        this.directionVec.cross(this.sideVector, this.upVector).normalize();; // up = ray x side

        this.sideVector.mul(HALF_THICKNESS);
        this.upVector.mul(HALF_THICKNESS);

        //Vertices 
        this.offset_BL.set(this.sideVector).negate().sub(this.upVector); // -side - up
        this.offset_BR.set(this.sideVector).sub(this.upVector);          // +side - up
        this.offset_TR.set(this.sideVector).add(this.upVector);          // +side + up
        this.offset_TL.set(this.sideVector).negate().add(this.upVector); // -side + up

        this.localStartPos.add(this.offset_BL, beamData.sBottomLeft);
        this.localStartPos.add(this.offset_BR, beamData.sBottomRight);
        this.localStartPos.add(this.offset_TR, beamData.sTopRight);
        this.localStartPos.add(this.offset_TL, beamData.sTopLeft);

        this.localEndPos.add(this.offset_BL, beamData.eBottomLeft);
        this.localEndPos.add(this.offset_BR, beamData.eBottomRight);
        this.localEndPos.add(this.offset_TR, beamData.eTopRight);
        this.localEndPos.add(this.offset_TL, beamData.eTopLeft);

        beamData.normalBottom.set(this.upVector);
        beamData.normalRight.set(this.sideVector).negate();
        beamData.normalTop.set(this.upVector).negate();
        beamData.normalLeft.set(this.sideVector);

        //Apply custom color if not powered and has optical lenses inserted
        if (!powered && blockEntity.hasLens(PropulsionItems.OPTICAL_LENS.get())) {
            //Get all lenses of type PropulsionItems.OPTICAL_LENS as item stacks and mix their colors in rgb space
            if (calculateFinalColor(blockEntity.getLenses())) {
                MODIFIED_START_COLOR.set(MODIFIED_TEMP_COLOR, START_ALPHA);
                MODIFIED_END_COLOR.set(MODIFIED_TEMP_COLOR, END_ALPHA);
                setBeamColors(beamData, MODIFIED_START_COLOR, MODIFIED_END_COLOR);
            } else { // No colored lens, default behaviour
                setBeamColors(beamData, powered);
            }
        } else {
            setBeamColors(beamData, powered);
        }

        beamData.worldToLocalTransform.set(poseStack.last().pose()).invert();
        beamData.poseSnapshot = poseStack.last();

        //Rendering setup
        //The first pass (behind translucent)
        poseStack.pushPose();
        VertexConsumer buffer = bufferSource.getBuffer(PropulsionRenderTypes.SOLID_TRANSLUCENT_BEAM);
        TranslucentBeamRenderer.drawBeam(buffer, beamData);
        poseStack.popPose();
        //Schedule the second 
        TranslucentBeamRenderer.scheduleBeamRender(beamData);
    }

    @Override
    public boolean shouldRender(@Nonnull AbstractOpticalSensorBlockEntity blockEntity, @Nonnull Vec3 cameraPos) {
        //Distance pre-check
        if (!super.shouldRender(blockEntity, cameraPos)) { 
            return false; 
        }
        //Frustum culling
        Frustum frustum = Minecraft.getInstance().levelRenderer.getFrustum();
        //Block AABB (for filters)
        BlockPos pos = blockEntity.getBlockPos();
        AABB blockAABB = new AABB(pos).inflate(2.0 / 16.0); //Expand AABB by 2 voxels to account for filtering
        if (frustum.isVisible(blockAABB)) return true;
        //Beam AABB
        AABB beamAABB = calculateBeamAABB(blockEntity);
        if (beamAABB == null) return false;
        return frustum == null || frustum.isVisible(beamAABB);
    }

    @Override
    public boolean shouldRenderOffScreen(@Nonnull AbstractOpticalSensorBlockEntity pBlockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    private void setBeamColors(BeamRenderData beamData, Vector4f startColor, Vector4f endColor) {
        beamData.startColor.set(startColor);
        beamData.endColor.set(endColor);
    }

    private void setBeamColors(BeamRenderData beamData, boolean powered) {
        beamData.startColor.set(powered ? START_POWERED_COLOR : START_COLOR);
        beamData.endColor.set(powered ? END_POWERED_COLOR : END_COLOR);
    }

    private AABB calculateBeamAABB(AbstractOpticalSensorBlockEntity blockEntity) {
        float distance = blockEntity.getRaycastDistance();
        if (distance <= 1e-6f) return null;
        BlockPos blockPos = blockEntity.getBlockPos();
        Direction facing = blockEntity.getBlockState().getValue(BlockStateProperties.FACING);
        this.directionVec.set(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        this.localStartPos.set(this.directionVec)
            .mul(blockEntity.getZAxisOffset())
            .add(0.5f, 0.5f, 0.5f);
        this.worldStart.set(blockPos.getX() + localStartPos.x(),
                           blockPos.getY() + localStartPos.y(),
                           blockPos.getZ() + localStartPos.z());
        this.worldEnd.set(this.worldStart)
                     .add(this.directionVec.x * distance,
                          this.directionVec.y * distance,
                          this.directionVec.z * distance);

        double minX = Math.min(worldStart.x, worldEnd.x);
        double minY = Math.min(worldStart.y, worldEnd.y);
        double minZ = Math.min(worldStart.z, worldEnd.z);
        double maxX = Math.max(worldStart.x, worldEnd.x);
        double maxY = Math.max(worldStart.y, worldEnd.y);
        double maxZ = Math.max(worldStart.z, worldEnd.z);

        double expansion = (double)HALF_THICKNESS;
        minX -= expansion;
        minY -= expansion;
        minZ -= expansion;
        maxX += expansion;
        maxY += expansion;
        maxZ += expansion;

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private boolean calculateFinalColor(NonNullList<ItemStack> lenses) {
        float totalRed = 0;
        float totalGreen = 0;
        float totalBlue = 0;
        int dyedLensCount = 0;

        Item opticalLensItem = PropulsionItems.OPTICAL_LENS.get();

        for (ItemStack stack : lenses) {
            if (stack.is(opticalLensItem)) {
                Item item = stack.getItem();
                if (item instanceof DyeableLeatherItem dyeableItem) {
                    if (dyeableItem.hasCustomColor(stack)) {
                        int color = dyeableItem.getColor(stack);
    
                        // Extract RGB values from hex
                        int r = (color >> 16) & 0xFF;
                        int g = (color >> 8) & 0xFF;
                        int b = color & 0xFF;
    
                        // Accumulate RGB values
                        totalRed += r;
                        totalGreen += g;
                        totalBlue += b;
                        dyedLensCount++;
                    }
                }
            }
        }

        if (dyedLensCount == 0) {
            return false;
        } else {
            float avgRed = totalRed / dyedLensCount;
            float avgGreen = totalGreen / dyedLensCount;
            float avgBlue = totalBlue / dyedLensCount;
            MODIFIED_TEMP_COLOR.set(avgRed / 255.0f, avgGreen / 255.0f, avgBlue / 255.0f);
            return true;
        }
    }
}