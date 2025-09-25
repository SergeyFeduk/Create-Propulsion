package com.deltasf.createpropulsion.events;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.network.PropulsionPackets;
import com.deltasf.createpropulsion.physics_assembler.AssemblyGaugeItem;
import com.deltasf.createpropulsion.physics_assembler.AssemblyGaugeOverlayRenderer;
import com.deltasf.createpropulsion.physics_assembler.packets.ResetGaugePacket;
//import com.deltasf.createpropulsion.design_goggles.DesignGogglesOverlayRenderer;
import com.deltasf.createpropulsion.registries.PropulsionItems;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreatePropulsion.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (tintIndex != 0) return -1;
            DyeableLeatherItem dyeableItem = (DyeableLeatherItem) stack.getItem();
            //Lerp 0.5 to white to make texture look more natural
            if (dyeableItem.hasCustomColor(stack)) {
                int color = dyeableItem.getColor(stack);

                float lerpCoefficient = 0.5f;
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;

                r = (int) (r + (255 - r) * lerpCoefficient);
                g = (int) (g + (255 - g) * lerpCoefficient);
                b = (int) (b + (255 - b) * lerpCoefficient);

                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));

                int newColor = (r << 16) | (g << 8) | b;
                return newColor;
            } else {
                return 0x00FFFFFF;
            }

        }, PropulsionItems.OPTICAL_LENS.get());
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "assembly_gauge", AssemblyGaugeOverlayRenderer.OVERLAY);
        //event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "goggle_info", DesignGogglesOverlayRenderer.OVERLAY);
    }
}
