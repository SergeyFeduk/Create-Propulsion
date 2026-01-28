package com.deltasf.createpropulsion.utility.tooltips;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.registries.PropulsionBlocks;
import com.deltasf.createpropulsion.thruster.thruster.ThrusterBlockEntity;
import com.simibubi.create.foundation.item.TooltipHelper;

import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

//TODO: Technically modifiers are only applied to GenericSummary, so perhaps I should move this into GenericSummaryProvider class? 
public class TooltipModifiers {
    private static final HashMap<Item, Function<SummaryPayload, String>> tooltipModificationLookup = new HashMap<Item, Function<SummaryPayload, String>>();

    static {
        //Thruster
        tooltipModificationLookup.put(PropulsionBlocks.THRUSTER_BLOCK.asItem(), (payload) -> {
            float thrustMultiplier = PropulsionConfig.THRUSTER_THRUST_MULTIPLIER.get().floatValue();
            int thrusterStrength = Math.round(ThrusterBlockEntity.BASE_MAX_THRUST / 1000.0f * thrustMultiplier);
            return Component.translatable(payload.path + ".tooltip.summary").getString().replace("{}", String.valueOf(thrusterStrength));
        });
        //Creative thruster
        tooltipModificationLookup.put(PropulsionBlocks.CREATIVE_THRUSTER_BLOCK.asItem(), (payload) -> {
            float thrustMultiplier = PropulsionConfig.CREATIVE_THRUSTER_THRUST_MULTIPLIER.get().floatValue();
            int thrusterStrength = Math.round(1000 * thrustMultiplier);
            return Component.translatable(payload.path + ".tooltip.summary").getString().replace("{}", String.valueOf(thrusterStrength));
        });
        //Inline optical sensor
        tooltipModificationLookup.put(PropulsionBlocks.INLINE_OPTICAL_SENSOR_BLOCK.asItem(), (payload) -> {
            int raycastDistance = PropulsionConfig.INLINE_OPTICAL_SENSOR_MAX_DISTANCE.get();
            return Component.translatable(payload.path + ".tooltip.summary").getString().replace("{}", String.valueOf(raycastDistance));
        });
    }

    public static boolean apply(Item item, List<Component> tooltipList) {
        Function<SummaryPayload, String> summarySupplier = tooltipModificationLookup.get(item);
        if (summarySupplier != null) {
            String path = CreatePropulsion.ID + "." + ForgeRegistries.ITEMS.getKey(item).getPath();
            String summary = summarySupplier.apply(new SummaryPayload(item, path));
            tooltipList.addAll(TooltipHelper.cutStringTextComponent(summary, Palette.STANDARD_CREATE));
            return true;
        }
        return false;
    }

    private record SummaryPayload (Item item, String path) {};
}
