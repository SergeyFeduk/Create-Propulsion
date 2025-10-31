package com.deltasf.createpropulsion.physics_assembler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import com.simibubi.create.AllSpecialTextures;
import net.createmod.catnip.outliner.Outliner;

public class AssemblyGaugeOverlayRenderer {
    public static final IGuiOverlay OVERLAY = AssemblyGaugeOverlayRenderer::renderOverlay;

    private static final double FLASH_ANIMATION_DURATION_S = 0.3;
    private static final double FLASH_HOLD_DURATION_S = 2.0;
    private static final double FLASH_TOTAL_DURATION_S = FLASH_ANIMATION_DURATION_S + FLASH_HOLD_DURATION_S;
    
    private static AABB lastSelectionAABB;
    private static BlockPos lastPosA;
    private static double flashStartGameTime = 0.0;
    private static boolean flashQueued = false;

    public static void triggerFlash(AABB selection) {
        flashQueued = true;
        lastSelectionAABB = selection;
    }

    @SuppressWarnings("null")
    public static void renderOverlay(ForgeGui gui, GuiGraphics graphics, float partialTicks, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null || mc.gameMode.getPlayerMode() == GameType.SPECTATOR)
            return;

        Player player = mc.player;
        ItemStack stack = player.getMainHandItem();
        if (!AssemblyUtility.isAssemblyGauge(stack)) {
            flashStartGameTime = 0.0;
            return;
        }

        double currentTimeInSeconds = (mc.level.getGameTime() + partialTicks) / 20.0;
        if (flashQueued) {
            flashStartGameTime = currentTimeInSeconds;
            flashQueued = false;
        }

        if (flashStartGameTime > 0.0) {
            if (currentTimeInSeconds - flashStartGameTime > FLASH_TOTAL_DURATION_S) {
                flashStartGameTime = 0.0;
            }
        }

        BlockPos posA = AssemblyGaugeItem.getPosA(stack);
        BlockPos posB = AssemblyGaugeItem.getPosB(stack);

        HitResult result = mc.hitResult;
        BlockPos lookingAtPos = null;
        if (result != null && result.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult) result;
            lookingAtPos = AssemblyUtility.getTargetedPosition(blockHitResult.getBlockPos(), blockHitResult.getDirection(), player);
        }

        if (posA == null) {
            if (lookingAtPos != null) {
                AABB previewBox = new AABB(lookingAtPos);
                Outliner.getInstance()
                    .chaseAABB("gauge_preview", previewBox)
                    .colored(AssemblyUtility.PASSIVE_COLOR)
                    .lineWidth(1 / 16f)
                    .disableLineNormals()
                    .withFaceTexture(AllSpecialTextures.SELECTION);
            }
            return;
        }

        boolean selectingSecond = posA != null && posB == null;
        Component statusText = null;

        boolean isTooLarge = false;
        AABB selectionBox = null; 
        if (selectingSecond && lookingAtPos != null) {
            selectionBox = new AABB(posA, lookingAtPos);
            isTooLarge = AssemblyUtility.isAABBLarger(selectionBox, AssemblyUtility.MAX_ASSEMBLY_SIZE);
        }

        if (selectingSecond) {
            if (isTooLarge) {
                if (selectionBox != null && AssemblyUtility.isAABBLarger(selectionBox, AssemblyUtility.MAX_RENDERED_OUTLINE_SIZE)) {
                    statusText = Component.translatable("createpropulsion.gauge.selection.same_structure").withStyle(s -> s.withColor(AssemblyUtility.CANCEL_COLOR));
                } else {
                    statusText = Component.translatable("createpropulsion.gauge.selection.too_big").withStyle(s -> s.withColor(AssemblyUtility.CANCEL_COLOR));
                }
            } else if (lastPosA == null) {
                lastPosA = posA;
            } else {
                statusText = Component.translatable("createpropulsion.gauge.selection.confirm").withStyle(s -> s.withColor(AssemblyUtility.HIGHLIGHT_COLOR));
            }
        }
        if (statusText != null) {
            gui.setOverlayMessage(statusText, false);
        }
        AABB currentSelectionBox = null;
        if (posA != null && posB != null) {
            currentSelectionBox = AssemblyUtility.fromBlockVolumes(posA, posB);
        } else if (selectingSecond && lookingAtPos != null) {
            currentSelectionBox = AssemblyUtility.fromBlockVolumes(posA, lookingAtPos);
        }

        if (currentSelectionBox == null) return;

        if (flashStartGameTime > 0.0 && currentSelectionBox.equals(lastSelectionAABB)) {
            double elapsedTime = currentTimeInSeconds - flashStartGameTime;
            float flashLineWidth;
            int color;

            if (elapsedTime < FLASH_ANIMATION_DURATION_S) {
                float progress = (float) (elapsedTime / FLASH_ANIMATION_DURATION_S);
                flashLineWidth = Mth.lerp(progress, 1/16f, 1/64f);
                color = AssemblyUtility.lerpColor(progress, AssemblyUtility.HIGHLIGHT_COLOR, AssemblyUtility.PASSIVE_COLOR);
            } else {
                flashLineWidth = 1/64f;
                color = AssemblyUtility.PASSIVE_COLOR;
            }

            AssemblyUtility.renderOutline("gauge_flash", lastSelectionAABB, color, flashLineWidth, true);

        } else if (posB != null) {
            boolean isHovering = AssemblyUtility.isPlayerLookingAtAABB(player, currentSelectionBox, partialTicks, 1.0, 0.0);
            float lineWidth = isHovering ? 1/16f : 1/64f;

            AssemblyUtility.renderOutline("gauge_selection", currentSelectionBox, AssemblyUtility.PASSIVE_COLOR, lineWidth, isHovering);
        } else {
            int color = isTooLarge ? AssemblyUtility.CANCEL_COLOR : AssemblyUtility.PASSIVE_COLOR;
            AssemblyUtility.renderOutline("gauge_selection", currentSelectionBox, color, 1/16f, true);
        }
    }
}
