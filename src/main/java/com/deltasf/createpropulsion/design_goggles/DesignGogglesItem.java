package com.deltasf.createpropulsion.design_goggles;

import com.deltasf.createpropulsion.registries.PropulsionItems;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;

public class DesignGogglesItem extends Item implements Equipable {

    public DesignGogglesItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    public static boolean isPlayerWearingGoggles(Player player) {
        return PropulsionItems.DESIGN_GOGGLES.isIn(player.getItemBySlot(EquipmentSlot.HEAD));
    }
}
