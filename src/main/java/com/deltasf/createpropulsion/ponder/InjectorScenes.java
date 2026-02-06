package com.deltasf.createpropulsion.ponder;

import java.util.ArrayList;
import java.util.List;

import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlock;
import com.deltasf.createpropulsion.heat.burners.solid.SolidBurnerBlock;
import com.deltasf.createpropulsion.ponder.instructions.AnimatedLineInstruction;
import com.deltasf.createpropulsion.ponder.instructions.ClusterInstruction;
import com.deltasf.createpropulsion.registries.PropulsionBlocks;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
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
import net.minecraft.world.phys.Vec3;

public class InjectorScenes {
    public static void hotAirPump(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("hot_air_pump", "Using Hot Air Pump");
        scene.configureBasePlate(1, 0, 9);
        scene.showBasePlate();

        scene.scaleSceneView(0.8f);
        scene.setSceneOffsetY(-3.5f);

        BlockPos pumpPos = util.grid().at(5, 3, 4);
        Selection pump = util.select().position(pumpPos);

        BlockPos burnerPos = util.grid().at(5, 2, 4);
        Selection burner = util.select().position(burnerPos);

        Selection airshipPlate = util.select().fromTo(1, 1, 2, 9, 1, 6);

        // Fences
        Selection fencesBack = util.select().fromTo(2, 2, 6, 2, 3, 6).add(util.select().fromTo(8, 2, 6, 8, 3, 6));
        Selection fencesFront = util.select().fromTo(2, 2, 2, 2, 3, 2).add(util.select().fromTo(8, 2, 2, 8, 3, 2));
        Selection allFences = fencesBack.add(fencesFront);

        //Shafts
        Selection inputRegion = util.select().fromTo(10, 0, 4, 10, 2, 5).add(util.select().fromTo(8, 2, 4, 9, 2, 4));
        Selection speedometer = util.select().position(7, 2, 4);
        Selection inputVertical = util.select().fromTo(6, 2, 4, 6, 3, 4);

        //Envelopes
        Selection[] balloonSlices = new Selection[6];
        for (int i = 0; i < 6; i++) {
            int y = 4 + i;
            balloonSlices[i] = util.select().fromTo(0, y, 0, 10, y, 8);
        }

        //Cutaway layers
        Selection cutawayZ0 = util.select().fromTo(0, 4, 0, 10, 9, 0);
        Selection cutawayZ1 = util.select().fromTo(0, 4, 1, 10, 9, 1);
        Selection cutawayZ2 = util.select().fromTo(0, 4, 2, 10, 9, 2).add(fencesFront);

        //Reveal everything
        scene.world().showSection(airshipPlate, Direction.DOWN);
        scene.idle(7);

        scene.world().showSection(burner, Direction.DOWN);
        scene.idle(7);

        scene.world().showSection(pump, Direction.DOWN);
        scene.idle(7);

        scene.world().showSection(inputRegion, Direction.WEST);
        scene.idle(7);

        scene.world().showSection(speedometer, Direction.DOWN);
        scene.idle(7);

        scene.world().showSection(inputVertical, Direction.DOWN);
        scene.idle(7);

        scene.world().showSection(allFences, Direction.DOWN);
        scene.idle(7);

        for (Selection slice : balloonSlices) {
            scene.world().showSection(slice, Direction.DOWN);
            scene.idle(5);
        }

        scene.idle(15);
        
        //Remove frontal section
        scene.world().hideSection(cutawayZ0, Direction.NORTH);
        scene.idle(5);
        
        scene.world().hideSection(cutawayZ1, Direction.NORTH);
        scene.idle(5);
        
        scene.world().hideSection(cutawayZ2, Direction.NORTH);
        scene.idle(20);

        //Yap
        scene.overlay().showText(70)
            .text("Hot air pumps can be used to create and fill balloons with hot air.")
            .pointAt(util.vector().blockSurface(pumpPos, Direction.WEST))
            .placeNearTarget()
            .attachKeyFrame();
        
        scene.idle(80);

        //Line to top
        Vec3 lineStart = util.vector().centerOf(pumpPos);
        Vec3 lineEnd = util.vector().topOf(util.grid().at(5, 8, 4));
        scene.addInstruction(new AnimatedLineInstruction(PonderPalette.RED, lineStart, lineEnd, 12, 5, true));
        
        scene.idle(12 + 5);

        //Filling animation
        List<List<BlockPos>> fillSlices = generateInternalSlices();
        String clusterKey = "balloon_volume";
        List<BlockPos> currentCluster = new ArrayList<>();

        for (List<BlockPos> slice : fillSlices) {
            currentCluster.addAll(slice);
            scene.addInstruction(new ClusterInstruction(clusterKey, PonderPalette.RED, 10, new ArrayList<>(currentCluster)));
            scene.idle(7);
        }
        
        scene.addInstruction(new ClusterInstruction(clusterKey, PonderPalette.RED, 30, new ArrayList<>(currentCluster)));

        scene.idle(45);

        //Heat req
        scene.overlay().showText(70)
            .text("Hot air pump must be placed on active heat source...")
            .pointAt(util.vector().blockSurface(burnerPos, Direction.WEST))
            .placeNearTarget()
            .attachKeyFrame();
        
        scene.idle(80);

        scene.overlay().showControls(util.vector().blockSurface(burnerPos, Direction.WEST), Pointing.LEFT, 30)
            .rightClick()
            .withItem(new ItemStack(Items.COAL));
        scene.idle(20);

        //Burner
        scene.world().modifyBlock(burnerPos, s -> s.setValue(SolidBurnerBlock.LIT, true), false);
        scene.world().modifyBlock(burnerPos, s -> s.setValue(AbstractBurnerBlock.HEAT, HeatLevel.KINDLED), false);

        scene.effects().emitParticles(util.vector().centerOf(burnerPos), (world, x, y, z) -> {
            BlockPos pos = BlockPos.containing(x, y, z);
            net.minecraft.world.level.block.state.BlockState state = world.getBlockState(pos);
            PropulsionBlocks.SOLID_BURNER.get().animateTick(state, world, pos, world.random);
        }, 1, 1000);
        
        scene.idle(30);

        //Rotation req
        scene.overlay().showText(70)
            .text("... and rotation must be provided for it to function.")
            .pointAt(util.vector().blockSurface(pumpPos, Direction.WEST))
            
            .placeNearTarget();
        
        scene.idle(25);

        //Set speed
        scene.world().setKineticSpeed(pump, 32);
        scene.world().setKineticSpeed(speedometer, 32);
        scene.world().setKineticSpeed(inputVertical, 32);
        scene.world().setKineticSpeed(inputRegion, 32);

        scene.idle(60);

        scene.overlay().showText(70)
            .text("Amount of hot air produced depends on RPM.")
            .pointAt(util.vector().blockSurface(pumpPos, Direction.WEST))
            .placeNearTarget()
            .attachKeyFrame();
        
        scene.idle(80);

        scene.overlay().showText(80)
            .text("At 256 RPM Hot Air Pump outputs the most amount of hot air.")
            .colored(PonderPalette.GREEN)
            .pointAt(util.vector().blockSurface(pumpPos, Direction.WEST))
            .placeNearTarget();
        
        scene.idle(50);

        scene.world().setKineticSpeed(pump, 256);
        scene.world().setKineticSpeed(speedometer, 256);
        scene.world().setKineticSpeed(inputVertical, 256);
        scene.world().setKineticSpeed(inputRegion, 256);

        scene.idle(60);
        scene.markAsFinished();
    }

    //TODO: Move in ponder utility
    private static List<List<BlockPos>> generateInternalSlices() {
        List<List<BlockPos>> slices = new ArrayList<>();
        
        int[] yLevels = {8, 7, 6, 5, 4};

        for (int y : yLevels) {
            List<BlockPos> slice = new ArrayList<>();
            boolean isTypeA = (y == 4 || y == 8); //ABBBA

            if (isTypeA) {
                addBlockRange(slice, 2, y, 3, 8, y, 5);
                addBlockRange(slice, 3, y, 6, 7, y, 6);
                
            } else {
                addBlockRange(slice, 1, y, 3, 9, y, 5);
                addBlockRange(slice, 2, y, 6, 8, y, 6);
                addBlockRange(slice, 3, y, 7, 7, y, 7);
            }
            slices.add(slice);
        }
        return slices;
    }

    private static void addBlockRange(List<BlockPos> list, int x1, int y, int z1, int x2, int y2, int z2) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                list.add(new BlockPos(x, y, z));
            }
        }
    }
}