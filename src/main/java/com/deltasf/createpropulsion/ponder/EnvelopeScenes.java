package com.deltasf.createpropulsion.ponder;

import java.util.ArrayList;
import java.util.List;

import com.deltasf.createpropulsion.balloons.injectors.hot_air_burner.HotAirBurnerBlock;
import com.deltasf.createpropulsion.balloons.injectors.hot_air_burner.HotAirBurnerBlockEntity;
import com.deltasf.createpropulsion.ponder.instructions.AnimatedLineInstruction;
import com.deltasf.createpropulsion.ponder.instructions.ClusterInstruction;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class EnvelopeScenes {
    public static void makingBalloon(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("making_balloon", "Making a Hot Air Balloon");
        scene.configureBasePlate(0, 0, 9);
        scene.showBasePlate();

        scene.scaleSceneView(0.8f);
        scene.setSceneOffsetY(-3.5f);

        BlockPos burnerPos = util.grid().at(4, 2, 4);
        Selection burnerSelection = util.select().position(burnerPos);

        Selection balloonBase = util.select().fromTo(2, 1, 2, 6, 1, 6);

        Selection fences = util.select().fromTo(4, 2, 2, 4, 4, 2)
            .add(util.select().fromTo(2, 2, 4, 2, 4, 4))
            .add(util.select().fromTo(4, 2, 6, 4, 4, 6))
            .add(util.select().fromTo(6, 2, 4, 6, 4, 4));
        
        Selection[] envelopeLayers = new Selection[9];
        for (int y = 5; y <= 13; y++) {
            envelopeLayers[y - 5] = util.select().fromTo(0, y, 0, 8, y, 8);
        }

        Selection[] removedSections = new Selection[] {
            util.select().fromTo(2, 7, 0, 6, 11, 0),
            util.select().fromTo(1, 6, 1, 7, 12, 1),
            util.select().fromTo(0, 5, 2, 8, 13, 2)
        };

        scene.world().showSection(balloonBase, Direction.DOWN);
        scene.idle(7);

        scene.world().showSection(burnerSelection, Direction.DOWN);
        scene.idle(7);

        scene.world().showSection(fences, Direction.DOWN);
        scene.idle(7);

        for (Selection layer : envelopeLayers) {
            scene.world().showSection(layer, Direction.DOWN);
            scene.idle(5);
        }

        scene.idle(5);
        scene.addKeyframe();
        scene.idle(15);
        
        //Part 2

        scene.overlay().showText(70)
            .text("Envelopes encase volume that can be filled with hot air.")
            .pointAt(util.vector().blockSurface(util.grid().at(2, 9, 2), Direction.DOWN))
            .placeNearTarget();
        scene.idle(80);

        scene.overlay().showControls(util.vector().blockSurface(burnerPos, Direction.WEST), Pointing.LEFT, 40)
            .rightClick()
            .withItem(new ItemStack(Items.COAL));

        scene.idle(5);
        scene.world().modifyBlock(burnerPos, s -> s.setValue(HotAirBurnerBlock.LIT, true), false);
        
        scene.effects().emitParticles(util.vector().centerOf(burnerPos), (world, x, y, z) -> {
            BlockPos pos = BlockPos.containing(x, y, z);
            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof HotAirBurnerBlock burner) {
                burner.animateTick(state, world, pos, world.random);
            }
        }, 1.0f, 1000);
        scene.idle(50);

        scene.overlay().showText(80)
            .attachKeyFrame()
            .text("Hot Air Burners fill hot air balloons above them, when fueled.")
            .pointAt(util.vector().blockSurface(burnerPos, Direction.WEST))
            .placeNearTarget();
        scene.idle(90);

        for (Selection selection : removedSections) {
            scene.world().hideSection(selection, Direction.NORTH);
            scene.idle(10);
        }
        scene.idle(10);

        Vec3 lineStart = util.vector().centerOf(burnerPos);
        Vec3 lineEnd = lineStart.add(0, 11, 0);
        scene.addInstruction(new AnimatedLineInstruction(PonderPalette.RED, lineStart, lineEnd, 12, 5, true));
        
        scene.idle(12 + 5);

        //Volume cluster
        List<List<BlockPos>> slices = generateInternalSlices();
        List<BlockPos> currentCluster = new ArrayList<>();
        String clusterKey = "balloon_volume";
        int clusterDuration = 15 + 5 + 65;

        for (List<BlockPos> slice : slices) {
            currentCluster.addAll(slice);
            scene.addInstruction(new ClusterInstruction(clusterKey, PonderPalette.RED, 10, new ArrayList<>(currentCluster)));
            scene.idle(5);
        }

        //3x3 open bottom
        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 5; z++) {
                currentCluster.add(new BlockPos(x, 5, z));
            }
        }
        
        scene.addInstruction(new ClusterInstruction(clusterKey, PonderPalette.RED, clusterDuration, new ArrayList<>(currentCluster)));
        scene.idle(15);
        
        scene.addKeyframe();

        //Distance line

        Vec3 distLineStart = util.vector().blockSurface(burnerPos, Direction.UP);
        Vec3 distLineEnd = util.vector().blockSurface(util.grid().at(4, 5, 4), Direction.DOWN);
        
        scene.addInstruction(new AnimatedLineInstruction(PonderPalette.RED, distLineStart, distLineEnd, 10, 70, true));
        
        scene.idle(5);

        scene.overlay().showText(70)
            .text("Hot Air Burners must not be further than 5 blocks from the open bottom of the balloon")
            .pointAt(distLineStart.lerp(distLineEnd, 0.5))
            .placeNearTarget();

        scene.idle(65);

        //Cluster dies here

        scene.idle(5);

        //Undo removal of removedSections
        for (int i = removedSections.length - 1; i >= 0; i--) {
            scene.world().showSection(removedSections[i], Direction.SOUTH);
            scene.idle(10);
        }

        scene.idle(10);
        scene.addKeyframe();
        scene.idle(10);

        //Rotate 90, show lever

        scene.rotateCameraY(90);

        scene.idle(35);

        scene.overlay().showText(60)
            .text("Hot air output can be controlled by clicking on the lever.")
            .pointAt(util.vector().blockSurface(burnerPos, Direction.NORTH))
            .placeNearTarget();
        scene.idle(70);

        scene.overlay().showText(40)
            .text("Hot Air Burner has 3 power levels.")
            .pointAt(util.vector().blockSurface(burnerPos, Direction.NORTH))
            .placeNearTarget();
        scene.idle(50);

        scene.overlay().showControls(util.vector().blockSurface(burnerPos, Direction.EAST).add(new Vec3(0,0,0.6f)), Pointing.RIGHT, 40).rightClick();
        scene.idle(10);
        
        scene.world().modifyBlockEntityNBT(burnerSelection, HotAirBurnerBlockEntity.class, nbt -> nbt.putInt("leverPosition", 1));
        scene.idle(10);
        
        scene.world().modifyBlockEntityNBT(burnerSelection, HotAirBurnerBlockEntity.class, nbt -> nbt.putInt("leverPosition", 2));
        scene.idle(10);

        scene.overlay().showText(80)
            .text("The higher is power level - the higher is hot air output")
            .pointAt(util.vector().blockSurface(burnerPos, Direction.NORTH))
            .placeNearTarget();
        scene.idle(90);

        scene.markAsFinished();
    }

    //TODO: Move in ponder utility
    private static List<List<BlockPos>> generateInternalSlices() {
        List<List<BlockPos>> slices = new ArrayList<>();
        int centerX = 4;
        int centerY = 9;
        int centerZ = 4;
        double radius = 4.0;
        double radiusSq = radius * radius;

        for (int y = 12; y >= 6; y--) {
            List<BlockPos> slice = new ArrayList<>();
            for (int x = 0; x <= 8; x++) {
                if (x == 0 || x == 8) continue;

                for (int z = 0; z <= 8; z++) {
                    if (z == 0 || z == 8) continue;
                    if (z <= 2) continue;
                    double distSq = Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2) + Math.pow(z - centerZ, 2);
                    if (distSq <= radiusSq) {
                        slice.add(new BlockPos(x, y, z));
                    }
                }
            }
            if (!slice.isEmpty()) {
                slices.add(slice);
            }
        }
        return slices;
    }
}

//TODO: Hole/hot air amount related ponder? 
//TODO: Holes do not invalidate balloons, instead they leak hot air. Too many holes may lead to balloon loosing all its hot air