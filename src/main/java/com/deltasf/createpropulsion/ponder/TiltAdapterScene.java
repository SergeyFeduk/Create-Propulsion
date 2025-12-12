package com.deltasf.createpropulsion.ponder;

import com.deltasf.createpropulsion.tilt_adapter.TiltAdapterBlockEntity;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class TiltAdapterScene {

    public static void tiltAdapter(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("tilt_adapter", "Tilting wings with Tilt Adapter");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos rightLeverPos = util.grid().at(3, 1, 0);
        BlockPos leftLeverPos = util.grid().at(3, 1, 4);
        BlockPos adapterPos = util.grid().at(3, 1, 2);
        BlockPos bearingPos = util.grid().at(1, 1, 2);
        BlockPos rightDustPos = util.grid().at(3, 1, 1);
        BlockPos leftDustPos = util.grid().at(3, 1, 3);

        Selection inputGroup = util.select().position(5, 0, 1)
            .add(util.select().fromTo(5, 1, 2, 4, 1, 2));

        Selection redstoneLine = util.select().fromTo(3, 1, 0, 3, 1, 4);

        Selection inputNetwork = util.select().fromTo(5, 0, 1, 3, 1, 2); 
        Selection outputNetwork = util.select().fromTo(2, 1, 2, 1, 1, 2); 
        Selection wingSelection = util.select().position(0, 1, 2);

        scene.idle(5);
        scene.world().showSection(inputGroup, Direction.UP);
        scene.idle(5);
        scene.world().showSection(redstoneLine, Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(outputNetwork, Direction.EAST);
        scene.idle(10);

        ElementLink<WorldSectionElement> wingElement = 
            scene.world().showIndependentSection(wingSelection, Direction.EAST);
        scene.world().configureCenterOfRotation(wingElement, util.vector().centerOf(bearingPos));

        scene.world().setKineticSpeed(inputNetwork, -32);
        scene.world().setKineticSpeed(outputNetwork, 0);
        scene.world().modifyBlockEntityNBT(util.select().position(adapterPos), TiltAdapterBlockEntity.class, 
            nbt -> nbt.putInt("moveDirection", 0));
        
        scene.idle(20);

        scene.overlay().showText(70)
            .text("Tilt Adapter allows tilting the output shaft based on Redstone signals")
            .placeNearTarget()
            .pointAt(util.vector().topOf(adapterPos));
            
        scene.idle(90);
        scene.addKeyframe();
        scene.idle(10);

        scene.world().toggleRedstonePower(util.select().fromTo(3, 1, 0, 3, 1, 1));
        scene.effects().indicateRedstone(rightLeverPos);

        scene.world().modifyBlockEntityNBT(util.select().position(adapterPos), TiltAdapterBlockEntity.class, nbt -> {
            nbt.putInt("redstoneLeft", 15);
            nbt.putInt("moveDirection", 1); 
        });

        scene.world().setKineticSpeed(outputNetwork, -32); 
        scene.world().rotateBearing(bearingPos, -30, 20);
        scene.world().rotateSection(wingElement, -30, 0, 0, 20);
        scene.effects().rotationDirectionIndicator(bearingPos);
        
        scene.idle(20);

        scene.world().setKineticSpeed(outputNetwork, 0);
        scene.world().modifyBlockEntityNBT(util.select().position(adapterPos), TiltAdapterBlockEntity.class, 
            nbt -> nbt.putInt("moveDirection", 0));
        
        scene.idle(30);

        scene.world().toggleRedstonePower(util.select().fromTo(3, 1, 3, 3, 1, 4));
        scene.effects().indicateRedstone(leftLeverPos);
        
        scene.overlay().showText(80)
            .text("Connected bearings are guaranteed to return to their initial position")
            .colored(PonderPalette.GREEN)
            .placeNearTarget()
            .pointAt(util.vector().topOf(bearingPos));

        scene.world().modifyBlockEntityNBT(util.select().position(adapterPos), TiltAdapterBlockEntity.class, nbt -> {
            nbt.putInt("redstoneRight", 15); 
            nbt.putInt("moveDirection", -1); 
        });

        scene.world().setKineticSpeed(outputNetwork, 32);
        scene.world().rotateBearing(bearingPos, 30, 20);
        scene.world().rotateSection(wingElement, 30, 0, 0, 20);
        scene.effects().rotationDirectionIndicator(bearingPos);
        
        scene.idle(20);

        scene.world().setKineticSpeed(outputNetwork, 0);
        scene.world().modifyBlockEntityNBT(util.select().position(adapterPos), TiltAdapterBlockEntity.class, 
            nbt -> nbt.putInt("moveDirection", 0));

        scene.idle(40);

        scene.world().toggleRedstonePower(util.select().fromTo(3, 1, 0, 3, 1, 1));
        
        scene.world().modifyBlockEntityNBT(util.select().position(adapterPos), TiltAdapterBlockEntity.class, nbt -> {
            nbt.putInt("redstoneLeft", 0); 
            nbt.putInt("moveDirection", -1); 
        });

        scene.world().setKineticSpeed(outputNetwork, 32);
        scene.world().rotateBearing(bearingPos, 30, 20);
        scene.world().rotateSection(wingElement, 30, 0, 0, 20);
        scene.effects().rotationDirectionIndicator(bearingPos);
        
        scene.idle(20);

        scene.world().setKineticSpeed(outputNetwork, 0);
        scene.world().modifyBlockEntityNBT(util.select().position(adapterPos), TiltAdapterBlockEntity.class, 
            nbt -> nbt.putInt("moveDirection", 0));
        
        scene.idle(20);

        scene.world().toggleRedstonePower(util.select().fromTo(3, 1, 3, 3, 1, 4));
        
        scene.world().modifyBlockEntityNBT(util.select().position(adapterPos), TiltAdapterBlockEntity.class, nbt -> {
            nbt.putInt("redstoneRight", 0);
            nbt.putInt("moveDirection", 1); 
        });

        scene.world().setKineticSpeed(outputNetwork, -32);
        scene.world().rotateBearing(bearingPos, -30, 20);
        scene.world().rotateSection(wingElement, -30, 0, 0, 20);
        scene.effects().rotationDirectionIndicator(bearingPos);
        
        scene.idle(20);

        scene.world().setKineticSpeed(outputNetwork, 0);
        scene.world().modifyBlockEntityNBT(util.select().position(adapterPos), TiltAdapterBlockEntity.class, 
            nbt -> nbt.putInt("moveDirection", 0));

        scene.idle(20);

        scene.addKeyframe();
        scene.idle(10);

        scene.world().setBlock(leftLeverPos, Blocks.AIR.defaultBlockState(), true);
        scene.world().setBlock(leftDustPos, Blocks.AIR.defaultBlockState(), true);
        
        scene.world().setBlock(rightLeverPos, AllBlocks.ANALOG_LEVER.getDefaultState()
            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST), true);
        
        scene.idle(10);
        
        scene.overlay().showControls(util.vector().topOf(rightLeverPos), Pointing.DOWN, 40).rightClick();
        scene.idle(20);

        scene.world().modifyBlockEntityNBT(util.select().position(rightLeverPos), AnalogLeverBlockEntity.class, 
            nbt -> nbt.putInt("State", 8));
        scene.world().modifyBlock(rightDustPos, s -> s.setValue(RedStoneWireBlock.POWER, 8), false);
        scene.effects().indicateRedstone(rightLeverPos);
        
        scene.world().modifyBlockEntityNBT(util.select().position(adapterPos), TiltAdapterBlockEntity.class, nbt -> {
            nbt.putInt("redstoneLeft", 8);
            nbt.putInt("moveDirection", 1); 
        });

        scene.world().setKineticSpeed(outputNetwork, -32);
        scene.world().rotateBearing(bearingPos, -16, 20);
        scene.world().rotateSection(wingElement, -16, 0, 0, 20);
        scene.effects().rotationDirectionIndicator(bearingPos);

        scene.idle(20);
        scene.world().setKineticSpeed(outputNetwork, 0);
        scene.world().modifyBlockEntityNBT(util.select().position(adapterPos), TiltAdapterBlockEntity.class, 
            nbt -> nbt.putInt("moveDirection", 0));

        scene.overlay().showText(70)
            .text("Final angle scales proportionally with Redstone signal strength")
            .placeNearTarget()
            .pointAt(util.vector().topOf(bearingPos));
        
        scene.idle(50);

        scene.markAsFinished();
    }
}