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

public class CopycatWingModel extends CopycatModel {
    private final int width;
    protected static final AABB CUBE_AABB = new AABB(0, 0, 0, 1, 1, 1);


    public CopycatWingModel(BakedModel originalModel, int width) {
        super(originalModel);
        this.width = width;
    }

    @Override
    protected List<BakedQuad> getCroppedQuads(BlockState state, Direction side, RandomSource rand, BlockState material, ModelData wrappedData, RenderType renderType) {
        
        Direction facing = state.getValue(CopycatWingBlock.FACING);
        if (facing == Direction.UP || facing == Direction.EAST || facing == Direction.SOUTH) facing = facing.getOpposite();
        Direction.Axis axis = facing.getAxis();

        BakedModel model = getModelOf(material);
        List<BakedQuad> templateQuads = model.getQuads(material, side, rand, wrappedData, renderType);
        
        List<BakedQuad> quads = new ArrayList<>();
        float halfWidth = width / 2f;

        for (boolean isPositiveSide : Iterate.trueAndFalse) {
            
            AABB croppingBox;
            Vec3 placementOffset;

            if (isPositiveSide) {
                float cropMax = halfWidth / 16f;
                croppingBox = new AABB(
                    0, 0, 0,
                    axis == Direction.Axis.X ? cropMax : 1, axis == Direction.Axis.Y ? cropMax : 1, axis == Direction.Axis.Z ? cropMax : 1
                );
                Vec3i n = facing.getNormal().multiply(-1);
                float offset = 0;
                switch (width) {
                    case 4:
                        offset = 6/16.0f;
                        break;
                    case 8:
                        offset = 4/16.0f;
                        break;
                    case 12:
                        offset = 2/16.0f;
                        break;
                    default:
                        break;
                }

                placementOffset = new Vec3(n.getX() * offset, n.getY() * offset, n.getZ() * offset);

            } else { 
                float cropMin = 1f - (halfWidth / 16f);
                croppingBox = new AABB(
                    axis == Direction.Axis.X ? cropMin : 0, axis == Direction.Axis.Y ? cropMin : 0, axis == Direction.Axis.Z ? cropMin : 0,
                    1, 1, 1
                );
                
                Vec3i n = facing.getNormal();
                float offset = 0;
                switch (width) {
                    case 4:
                        offset = 6/16.0f;
                        break;
                    case 8:
                        offset = 4/16.0f;
                        break;
                    case 12:
                        offset = 2/16.0f;
                        break;
                
                    default:
                        break;
                }
                placementOffset = new Vec3(n.getX() * offset, n.getY() * offset, n.getZ() * offset);
            }

            for (BakedQuad quad : templateQuads) {
                Direction direction = quad.getDirection();

                if (isPositiveSide && direction == facing.getOpposite())
                    continue;
                if (!isPositiveSide && direction == facing)
                    continue;

                quads.add(BakedQuadHelper.cloneWithCustomGeometry(quad,
                    BakedModelHelper.cropAndMove(quad.getVertices(), quad.getSprite(), croppingBox, placementOffset)));
            }
        }

        return quads;
    }
}
