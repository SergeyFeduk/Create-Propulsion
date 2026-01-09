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
        scene.title("redstone_transmission_direct", "Direct Control Mode");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        Selection bottom = util.select().fromTo(2, 1, 2, 2, 2, 2)
                .add(util.select().fromTo(3,1,2,4,1,2))
                .add(util.select().fromTo(5,0,2,5,1,2));
        Selection up = util.select().fromTo(2, 3, 2, 2, 4, 2);
        Selection lever = util.select().fromTo(1, 2, 2, 1, 2, 2);

        scene.world().setKineticSpeed(bottom, 64.0f);
        scene.world().setKineticSpeed(util.select().fromTo(5,0,2,5,0,2), -64f);
        scene.world().setKineticSpeed(up, 0.0f);
        scene.world().showSection(bottom, Direction.DOWN);
        scene.world().showSection(up, Direction.DOWN);
        scene.idle(20);
        scene.overlay().showText(80)
                        .text("Transmission has 2 control mode, and direct mode is the default setting when placed.")
                        .pointAt(util.vector().blockSurface(util.grid().at(2, 2, 2), Direction.WEST));
        scene.idle(100);

        scene.idle(20);
        scene.overlay().showText(80)
                .text("Transmission in direct control mode will output speed proportional to the redstone input to either side.")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(util.grid().at(2, 2, 2), Direction.WEST));
        scene.idle(60);
        scene.world().showSection(lever, Direction.EAST);
        scene.idle(20);

        scene.world().modifyBlockEntityNBT(lever, AnalogLeverBlockEntity.class,
                nbt -> nbt.putInt("State", 7));
        scene.effects().indicateRedstone(lever.iterator().next());
        scene.world().modifyBlockEntityNBT(bottom, RedstoneTransmissionBlockEntity.class,
                nbt -> nbt.putInt("transmission_shift", 119));
        scene.world().setKineticSpeed(up, 64.0f * 7.0f / 15.0f);
        scene.effects().rotationSpeedIndicator(util.grid().at(2, 3, 2));
        scene.idle(40);
        scene.overlay().showText(80)
                .text("Note that the output speed will be scaled between 0 to input speed")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(util.grid().at(2, 3, 2), Direction.NORTH));
        scene.idle(100);

        scene.world().modifyBlockEntityNBT(lever, AnalogLeverBlockEntity.class,
                nbt -> nbt.putInt("State", 15));
        scene.effects().indicateRedstone(lever.iterator().next());
        scene.world().modifyBlockEntityNBT(bottom, RedstoneTransmissionBlockEntity.class,
                nbt -> nbt.putInt("transmission_shift", 256));
        scene.world().setKineticSpeed(up, 64.0f);
        scene.effects().rotationSpeedIndicator(util.grid().at(2, 3, 2));
        scene.idle(40);
    }

    public static void incrementalControl(SceneBuilder builder, SceneBuildingUtil util) {
        int shift = 0;

        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("redstone_transmission_incremental", "Incremental Control Mode");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();

        Selection bottom = util.select().fromTo(2, 1, 2, 2, 2, 2)
                .add(util.select().fromTo(3,1,2,4,1,2))
                .add(util.select().fromTo(5,0,2,5,1,2));
        Selection up = util.select().fromTo(2, 3, 2, 2, 4, 2);
        Selection lever_up = util.select().fromTo(1, 2, 2, 1, 2, 2);
        Selection lever_down = util.select().fromTo(3, 2, 2, 3, 2, 2);

        scene.world().setKineticSpeed(bottom, 128.0f);
        scene.world().setKineticSpeed(util.select().fromTo(5,0,2,5,0,2), -128.0f);
        scene.world().setKineticSpeed(up, 0.0f);
        scene.world().showSection(bottom, Direction.DOWN);
        scene.world().showSection(up, Direction.DOWN);
        scene.world().modifyBlockEntityNBT(bottom, RedstoneTransmissionBlockEntity.class,
                nbt -> nbt.putInt("ScrollValue", 1));
        scene.idle(20);
        scene.overlay().showText(80)
                .text("Transmission in incremental control mode will output speed proportional to the internal shift level.")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(util.grid().at(2, 2, 2), Direction.WEST));
        scene.idle(100);
        scene.rotateCameraY(180);
        scene.idle(20);
        scene.addKeyframe();

        scene.overlay().showScrollInput(util.vector().blockSurface(util.grid().at(2, 2, 2), Direction.SOUTH), Direction.SOUTH, 80);
        scene.overlay().showText(80)
                .text("You can change the control mode with a scroll selection box at the back of the transmission.")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(util.grid().at(2, 2, 2), Direction.SOUTH));
        scene.idle(100);
        scene.rotateCameraY(-180);
        scene.idle(20);
        scene.addKeyframe();

        scene.overlay().showText(80)
                .text("Redstone signal given to the side with + (plus) sign will increase the shift level.")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(util.grid().at(2, 2, 2), Direction.WEST));
        scene.idle(80);
        scene.world().showSection(lever_up, Direction.EAST);
        scene.idle(20);

        scene.world().modifyBlockEntityNBT(lever_up, AnalogLeverBlockEntity.class,
                nbt -> nbt.putInt("State", 8));
        scene.effects().indicateRedstone(lever_up.iterator().next());
        for (int i = 1; i <= 8; i++) {
            shift += 8;
            int finalShift = shift;
            scene.world().modifyBlockEntityNBT(bottom, RedstoneTransmissionBlockEntity.class,
                    nbt -> nbt.putInt("transmission_shift", finalShift));
            scene.world().setKineticSpeed(up, shift / 2.0f);
            scene.idle(10);
        }
        scene.world().modifyBlockEntityNBT(lever_up, AnalogLeverBlockEntity.class,
                nbt -> nbt.putInt("State", 0));
        scene.idle(20);
        scene.world().hideSection(lever_up, Direction.WEST);
        scene.idle(20);

        scene.rotateCameraY(90);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("Redstone signal given to the side with - (minus) sign will decrease the shift level.")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(util.grid().at(2, 2, 2), Direction.EAST));
        scene.idle(80);
        scene.world().showSection(lever_down, Direction.WEST);
        scene.idle(20);

        scene.world().modifyBlockEntityNBT(lever_down, AnalogLeverBlockEntity.class,
                nbt -> nbt.putInt("State", 4));
        scene.effects().indicateRedstone(lever_down.iterator().next());
        for (int i = 1; i <= 8; i++) {
            shift -= 4;
            int finalShift = shift;
            scene.world().modifyBlockEntityNBT(bottom, RedstoneTransmissionBlockEntity.class,
                    nbt -> nbt.putInt("transmission_shift", finalShift));
            scene.world().setKineticSpeed(up, shift / 2.0f);
            scene.idle(10);
        }
        scene.world().modifyBlockEntityNBT(lever_down, AnalogLeverBlockEntity.class,
                nbt -> nbt.putInt("State", 0));
        scene.idle(20);
        scene.world().hideSection(lever_down, Direction.EAST);
        scene.idle(20);
        scene.addKeyframe();

        scene.rotateCameraY(-60);
        scene.idle(20);

        scene.overlay().showText(80)
                .text("If the signal is given to the both side...")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(util.grid().at(2, 2, 2), Direction.NORTH));
        scene.idle(80);
        scene.world().showSection(lever_up, Direction.EAST);
        scene.world().showSection(lever_down, Direction.WEST);
        scene.idle(20);

        scene.world().modifyBlockEntityNBT(lever_up, AnalogLeverBlockEntity.class,
                nbt -> nbt.putInt("State", 12));
        scene.effects().indicateRedstone(lever_up.iterator().next());
        scene.world().modifyBlockEntityNBT(lever_down, AnalogLeverBlockEntity.class,
                nbt -> nbt.putInt("State", 4));
        scene.effects().indicateRedstone(lever_down.iterator().next());
        scene.overlay().showText(80)
                .text("...the transmission will combine the addition and subtraction.")
                .placeNearTarget()
                .pointAt(util.vector().blockSurface(util.grid().at(2, 2, 2), Direction.NORTH));
        for (int i = 1; i <= 8; i++) {
            shift += 8;
            int finalShift = shift;
            scene.world().modifyBlockEntityNBT(bottom, RedstoneTransmissionBlockEntity.class,
                    nbt -> nbt.putInt("transmission_shift", finalShift));
            scene.world().setKineticSpeed(up, shift / 2.0f);
            scene.idle(10);
        }
        scene.world().modifyBlockEntityNBT(lever_up, AnalogLeverBlockEntity.class,
                nbt -> nbt.putInt("State", 0));
        scene.world().modifyBlockEntityNBT(lever_down, AnalogLeverBlockEntity.class,
                nbt -> nbt.putInt("State", 0));
        scene.idle(20);

        scene.rotateCameraY(-30);
        scene.idle(40);

    }
}
