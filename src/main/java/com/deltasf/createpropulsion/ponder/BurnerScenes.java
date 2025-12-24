package com.deltasf.createpropulsion.ponder;

import com.deltasf.createpropulsion.heat.burners.AbstractBurnerBlock;
import com.deltasf.createpropulsion.heat.burners.solid.SolidBurnerBlock;
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

public class BurnerScenes {
    //Solid burner
    public static void solidBurner(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("solid_burner", "Generating heat with Solid Burner");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        BlockPos burnerPos = util.grid().at(2, 1, 2);
        BlockPos enginePos = util.grid().at(2, 2, 2);
        BlockPos leverPos = util.grid().at(1, 1, 2);

        Selection burnerSel = util.select().position(burnerPos);
        Selection engineSel = util.select().position(enginePos);
        Selection leverSel = util.select().position(leverPos);

        scene.world().showSection(burnerSel, Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(engineSel, Direction.DOWN);
        scene.idle(20);

        scene.overlay().showText(70)
            .text("createpropulsion.ponder.solid_burner.text_1")
            .pointAt(util.vector().blockSurface(burnerPos, Direction.WEST))
            .placeNearTarget();
        scene.idle(80);

        scene.overlay().showControls(util.vector().blockSurface(burnerPos, Direction.WEST), Pointing.LEFT, 40)
            .rightClick()
            .withItem(new ItemStack(Items.COAL));
        scene.idle(20);

        scene.world().modifyBlock(burnerPos, s -> s.setValue(SolidBurnerBlock.LIT, true), false);
        scene.world().modifyBlock(burnerPos, s -> s.setValue(AbstractBurnerBlock.HEAT, HeatLevel.KINDLED), false);

        scene.effects().emitParticles(util.vector().centerOf(burnerPos), (world, x, y, z) -> {
            BlockPos pos = BlockPos.containing(x, y, z);
            net.minecraft.world.level.block.state.BlockState state = world.getBlockState(pos);
            PropulsionBlocks.SOLID_BURNER.get().animateTick(state, world, pos, world.random);
        }, 1, 1000);
        scene.idle(20);

        scene.world().setKineticSpeed(engineSel, 64);
        scene.effects().indicateSuccess(enginePos);
        scene.idle(10);

        scene.overlay().showText(70)
            .text("createpropulsion.ponder.solid_burner.text_2")
            .pointAt(util.vector().centerOf(enginePos))
            .placeNearTarget();
        scene.idle(80);

        scene.overlay().showText(80)
            .colored(PonderPalette.GREEN)
            .text("createpropulsion.ponder.solid_burner.text_3")
            .pointAt(util.vector().blockSurface(burnerPos, Direction.WEST))
            .placeNearTarget();
        scene.idle(100);

        scene.world().hideSection(engineSel, Direction.UP);
        scene.world().setKineticSpeed(engineSel, 0); 
        scene.idle(25);
        
        scene.world().showSection(leverSel, Direction.EAST);
        scene.idle(20);

        scene.overlay().showText(70)
            .attachKeyFrame()
            .text("createpropulsion.ponder.solid_burner.text_4")
            .pointAt(util.vector().blockSurface(burnerPos, Direction.WEST))
            .placeNearTarget();
        scene.idle(80);

        scene.overlay().showText(80)
            .text("createpropulsion.ponder.solid_burner.text_5")
            .pointAt(util.vector().blockSurface(burnerPos, Direction.WEST))
            .placeNearTarget();
        scene.idle(90);
        
        scene.world().toggleRedstonePower(leverSel);
        scene.effects().indicateRedstone(leverPos);
        scene.idle(20);

        scene.overlay().showText(100)
            .colored(PonderPalette.RED)
            .text("createpropulsion.ponder.solid_burner.text_6")
            .pointAt(util.vector().blockSurface(burnerPos, Direction.WEST))
            .placeNearTarget();
        scene.idle(110);

        scene.world().modifyBlock(burnerPos, s -> s.setValue(AbstractBurnerBlock.HEAT, HeatLevel.KINDLED), false);
        scene.overlay().showText(60)
            .text("createpropulsion.ponder.solid_burner.text_7") 
            .pointAt(util.vector().topOf(burnerPos))
            .placeNearTarget();
        scene.idle(60);

        scene.world().modifyBlock(burnerPos, s -> s.setValue(AbstractBurnerBlock.HEAT, HeatLevel.SEETHING), false);
        scene.overlay().showText(80)
            .text("createpropulsion.ponder.solid_burner.text_8") 
            .pointAt(util.vector().topOf(burnerPos))
            .placeNearTarget();
        scene.idle(90);
    }

    //TODO: Liquid burner
    //TODO: Create burner usage (show that burners can be used to power boilers and heated mixers)
}
