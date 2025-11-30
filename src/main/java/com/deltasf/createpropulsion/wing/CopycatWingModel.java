package com.deltasf.createpropulsion.wing;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.decoration.copycat.CopycatModel;
import com.simibubi.create.foundation.model.BakedModelHelper;
import com.simibubi.create.foundation.model.BakedQuadHelper;

import net.createmod.catnip.data.Iterate;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;
import com.tterrag.registrate.util.nullness.NonNullFunction;

public class CopycatWingModel extends CopycatModel {
    private final int width;
    private static final float PIXELS_PER_BLOCK = 16.0f;

    public CopycatWingModel(BakedModel originalModel, int width) {
        super(originalModel);
        this.width = width;
    }

    public static NonNullFunction<BakedModel, ? extends BakedModel> create(int width) {
        return bakedModel -> new CopycatWingModel(bakedModel, width);
    }

    @Override
    protected List<BakedQuad> getCroppedQuads(BlockState state, Direction side, RandomSource rand, BlockState material, ModelData wrappedData, RenderType renderType) {
        //Figure out facing
        Direction facing = state.getValue(CopycatWingBlock.FACING);
        if (facing.getAxisDirection() == Direction.AxisDirection.POSITIVE) facing = facing.getOpposite();
        Direction.Axis axis = facing.getAxis();

        //Get model and quads
        BakedModel model = getModelOf(material);
        List<BakedQuad> templateQuads = model.getQuads(material, side, rand, wrappedData, renderType);
        List<BakedQuad> quads = new ArrayList<>();

        float halfWidth = width / 2f;
        float offsetDistance = (PIXELS_PER_BLOCK - width) / (2 * PIXELS_PER_BLOCK);

        for (boolean isPositiveSide : Iterate.trueAndFalse) {

            Direction culledFace = isPositiveSide ? facing.getOpposite() : facing;
            Vec3i placementNormal = culledFace.getNormal();
            Vec3 placementOffset = new Vec3(
                placementNormal.getX() * offsetDistance,
                placementNormal.getY() * offsetDistance,
                placementNormal.getZ() * offsetDistance
            );

            float cropExtent = halfWidth / PIXELS_PER_BLOCK;
            float minX = 0, minY = 0, minZ = 0;
            float maxX = 1, maxY = 1, maxZ = 1;
            
            //Obtaion crop box
            switch (axis) {
                case X -> { if (!isPositiveSide) minX = 1 - cropExtent; else maxX = cropExtent; }
                case Y -> { if (!isPositiveSide) minY = 1 - cropExtent; else maxY = cropExtent; }
                case Z -> { if (!isPositiveSide) minZ = 1 - cropExtent; else maxZ = cropExtent; }
            }
            AABB croppingBox = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

            for (BakedQuad quad : templateQuads) {
                if (quad.getDirection() == culledFace) {
                    continue;
                }

                quads.add(BakedQuadHelper.cloneWithCustomGeometry(quad,
                    BakedModelHelper.cropAndMove(quad.getVertices(), quad.getSprite(), croppingBox, placementOffset)));
            }
        }
        return quads;
    }
}