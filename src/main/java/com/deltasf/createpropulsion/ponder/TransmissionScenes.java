package com.deltasf.createpropulsion.ponder;

import com.deltasf.createpropulsion.redstone_transmission.RedstoneTransmissionBlockEntity;
import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.Direction;

public class TransmissionScenes {
    public static void directControl(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("redstone_transmission_direct", "Redstone Transmission Direct Control");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        Selection transmission = util.select().fromTo(2, 2, 2, 2, 2, 2);
        Selection bottom = util.select().fromTo(2, 1, 2, 2, 1, 2);
        Selection up = util.select().fromTo(2, 3, 2, 2, 3, 2);
        Selection lever = util.select().fromTo(1, 2, 2, 1, 2, 2);

        scene.world().setKineticSpeed(bottom, 32.0f);
        scene.world().setKineticSpeed(up, 0.0f);
        scene.world().setKineticSpeed(transmission, 0.0f);
        scene.world().showSection(util.select().everywhere(), Direction.DOWN);
        scene.idle(30);

        scene.world().modifyBlockEntityNBT(lever, AnalogLeverBlockEntity.class,
                nbt -> nbt.putInt("State", 7));
        scene.effects().indicateRedstone(lever.iterator().next());
        scene.world().modifyBlockEntityNBT(transmission, RedstoneTransmissionBlockEntity.class,
                nbt -> nbt.putInt("transmission_shift", 119));
        scene.world().setKineticSpeed(up, 32.0f * 7.0f / 15.0f);
        scene.idle(40);

        scene.world().modifyBlockEntityNBT(lever, AnalogLeverBlockEntity.class,
                nbt -> nbt.putInt("State", 15));
        scene.effects().indicateRedstone(lever.iterator().next());
        scene.world().modifyBlockEntityNBT(transmission, RedstoneTransmissionBlockEntity.class,
                nbt -> nbt.putInt("transmission_shift", 255));
        scene.world().setKineticSpeed(up, 32.0f);
        scene.idle(40);
    }

    public static void incrementalControl(SceneBuilder builder, SceneBuildingUtil util) {

    }
}
