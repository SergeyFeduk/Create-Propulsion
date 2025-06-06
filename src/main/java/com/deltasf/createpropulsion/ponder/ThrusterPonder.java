package com.deltasf.createpropulsion.ponder;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;


public class ThrusterPonder {
    public static void ponder(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("thruster", "Setting up a Thruster");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        scene.idle(15);
        scene.addKeyframe();
        scene.world().showSection(util.select().fromTo(0,1,0, 5, 5, 5), Direction.DOWN);
        BlockPos leverPos = util.grid().at(1,1,1);
        scene.effects().indicateRedstone(leverPos);
        scene.world().toggleRedstonePower(util.select().fromTo(leverPos, leverPos));
        scene.idle(15);
    }
}
