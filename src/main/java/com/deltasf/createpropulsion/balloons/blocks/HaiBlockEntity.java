package com.deltasf.createpropulsion.balloons.blocks;

import java.util.List;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;

public class HaiBlockEntity extends AbstractHotAirInjectorBlockEntity implements IHaveGoggleInformation {
    public HaiBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    public double getInjectionAmount() {
        return 1;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        //Temp, for debugging purposes
        Ship ship = VSGameUtilsKt.getShipManagingPos(getLevel(), worldPosition);
        if (ship == null) return false;
        Balloon balloon = BalloonShipRegistry.forShip(ship.getId()).getBalloonOf(this.haiId);
        if (balloon == null) {
            Lang.builder().text("Balloon not found, hai is not active").forGoggles(tooltip);
            return true;
        }

        Lang.builder().text("Balloon").forGoggles(tooltip);
        Lang.builder().text("Hot air: ")
            .add(Lang.number(balloon.hotAir))
            .text(" / ")
            .add(Lang.number(balloon.getVolumeSize())).forGoggles(tooltip);
        
        double percentage = balloon.hotAir / balloon.getVolumeSize();
        Lang.builder().add(Lang.number(percentage * 100)).text("%").forGoggles(tooltip);
        
        Lang.builder().text("Holes: " + balloon.holes.size()).forGoggles(tooltip);
        return true;
    }
}
