package com.deltasf.createpropulsion.design_goggles;

import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

/*import com.deltasf.createpropulsion.design_goggles.ClientShipDataCache.ShipData;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.outliner.Outliner;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class DesignGogglesOverlayRenderer {
    public static final IGuiOverlay OVERLAY = DesignGogglesOverlayRenderer::renderOverlay;

    private static BlockPos lastHovered = null;
    private static int hoverTicks = 0;

    public static void renderOverlay(ForgeGui gui, GuiGraphics graphics, float partialTicks, int width,
                                     int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.gameMode.getPlayerMode() == GameType.SPECTATOR)
            return;

        //TODO: Target not just reachable blocks, but blocks in larger range (3x base reach range?)
        // Or do the check with a very large distance and render stuff based on heuristics(shipAABBSize, distanceToShip) -> bool
        HitResult objectMouseOver = mc.hitResult;
        if (!(objectMouseOver instanceof BlockHitResult result)) {
            lastHovered = null;
            hoverTicks = 0;
            return;
        }

        boolean wearingDesignGoggles = DesignGogglesItem.isPlayerWearingGoggles(mc.player);
        if (!wearingDesignGoggles) return;

        if (VSGameUtilsKt.isBlockInShipyard(mc.level, result.getBlockPos())) {
            var ship = VSGameUtilsKt.getShipManagingPos(mc.level, result.getBlockPos());
            Vector3dc COMPos = ship.getTransform().getPositionInWorld();
            
            ShipData data = ClientShipDataCache.getShipData(ship.getId());
            if (data != null) {
                //Actually show
                int x = width / 2 + 15;
                int y = height / 2;

                Component comp = Component.literal("Mass: " + data.mass()).withStyle(ChatFormatting.GREEN);
                graphics.drawString(mc.font, comp, x, y, 0xFFFFFF, true);
            }
            
            //TODO: Replace with custom implementation that allows to:
            // Have custome render type (to skip depth check)
            // Render the whole outline in ship space (so it follows ship smoothly)
            // Rotate the aabb to follow ships rotation
            CreateClient.OUTLINER.showAABB(mc.player, AABB.ofSize(VectorConversionsMCKt.toMinecraft(COMPos), 5, 5, 5))
                .colored(0x68c586)
                .disableLineNormals()
                .lineWidth(1/16f);
        }
    }
}
*/