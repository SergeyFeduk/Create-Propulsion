package com.deltasf.createpropulsion.physics_assembler;

import java.awt.Color;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.outliner.AABBOutline;

import net.minecraft.ChatFormatting;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.common.ForgeMod; 

public class AssemblyGaugeOverlayRenderer {
    public static final IGuiOverlay OVERLAY = AssemblyGaugeOverlayRenderer::renderOverlay;

    private static AABB lastSelectionAABB;
    private static BlockPos lastPosA;

    private static final int PASSIVE_COLOR = 0xaf68c5;
    private static final int HIGHLIGHT_COLOR = 0xc782de;
    private static final int CANCEL_COLOR = 0xFF5555;

    private static double flashStartGameTime = 0.0;
    private static final double FLASH_ANIMATION_DURATION_S = 0.3;
    private static final double FLASH_HOLD_DURATION_S = 2.0;
    private static final double FLASH_TOTAL_DURATION_S = FLASH_ANIMATION_DURATION_S + FLASH_HOLD_DURATION_S;

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
        if (!(stack.getItem() instanceof AssemblyGaugeItem)) {
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
            lookingAtPos = AssemblyGaugeItem.getTargetedPosition(blockHitResult.getBlockPos(), blockHitResult.getDirection());
        }

        boolean selectingSecond = posA != null && posB == null;
        Component statusText = null;
        boolean isTooLarge = false;
        if (selectingSecond && lookingAtPos != null) {
            if (Math.abs(posA.getX() - lookingAtPos.getX()) > AssemblyGaugeItem.MAX_SIZE
                || Math.abs(posA.getY() - lookingAtPos.getY()) > AssemblyGaugeItem.MAX_SIZE
                || Math.abs(posA.getZ() - lookingAtPos.getZ()) > AssemblyGaugeItem.MAX_SIZE) {
                isTooLarge = true;
            }
        }
        if (posA == null) lastPosA = null;
        if (selectingSecond) {
            if (isTooLarge) {
                statusText = Component.literal("Selection is too big").withStyle(ChatFormatting.RED);
            } else if (lastPosA == null) {
                statusText = Component.literal("First position selected").withStyle(ChatFormatting.WHITE);
                lastPosA = posA;
            } else {
                if (player.isShiftKeyDown()) {
                    statusText = Component.literal("Click again to cancel").withStyle(s -> s.withColor(CANCEL_COLOR));
                } else {
                    statusText = Component.literal("Click again to confirm").withStyle(s -> s.withColor(HIGHLIGHT_COLOR));
                }
            }
        }
        if (statusText != null) {
            gui.setOverlayMessage(statusText, false);
        }
        AABB currentSelectionBox = null;
        if (posA != null && posB != null) {
            currentSelectionBox = new AABB(posA).minmax(new AABB(posB));
        } else if (selectingSecond && lookingAtPos != null) {
            currentSelectionBox = new AABB(posA).minmax(new AABB(lookingAtPos));
        }

        if (currentSelectionBox == null) return;

        if (flashStartGameTime > 0.0 && currentSelectionBox.equals(lastSelectionAABB)) {
            double elapsedTime = currentTimeInSeconds - flashStartGameTime;
            float flashLineWidth;
            int color;

            if (elapsedTime < FLASH_ANIMATION_DURATION_S) {
                float progress = (float) (elapsedTime / FLASH_ANIMATION_DURATION_S);
                flashLineWidth = Mth.lerp(progress, 1/16f, 1/64f);
                color = lerpColor(progress, HIGHLIGHT_COLOR, PASSIVE_COLOR);
            } else {
                flashLineWidth = 1/64f;
                color = PASSIVE_COLOR;
            }

            CreateClient.OUTLINER.showAABB("gauge_flash", lastSelectionAABB)
                .colored(color)
                .lineWidth(flashLineWidth)
                .withFaceTexture(AllSpecialTextures.SELECTION)
                .disableLineNormals();
        } else if (posB != null) {
            Vec3 eyePos = player.getEyePosition(partialTicks);

            boolean isHovering = currentSelectionBox.contains(eyePos);
            if (!isHovering) {
                double reach = player.getAttribute(ForgeMod.BLOCK_REACH.get()).getValue() + 1;
                Vec3 lookVec = player.getViewVector(partialTicks);
                Vec3 endPos = eyePos.add(lookVec.scale(reach));
                isHovering = currentSelectionBox.clip(eyePos, endPos).isPresent();
            }

            float lineWidth = isHovering ? 1/16f : 1/64f;

            var outline = CreateClient.OUTLINER.showAABB("gauge_selection", currentSelectionBox)
                .colored(PASSIVE_COLOR)
                .lineWidth(lineWidth)
                .disableLineNormals();

            outline.withFaceTexture(isHovering ? AllSpecialTextures.SELECTION : null);

        } else {
            int color = isTooLarge || (player.isShiftKeyDown()) ? CANCEL_COLOR : PASSIVE_COLOR;
            CreateClient.OUTLINER.showAABB("gauge_selection", currentSelectionBox)
                .colored(color)
                .lineWidth(1/16f)
                .withFaceTexture(AllSpecialTextures.SELECTION)
                .disableLineNormals();
        }
    }

    private static int lerpColor(float progress, int color1, int color2) {
        int r1 = (color1 >> 16) & 0xFF, g1 = (color1 >> 8) & 0xFF, b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF, g2 = (color2 >> 8) & 0xFF, b2 = color2 & 0xFF;
        int r = (int) Mth.lerp(progress, r1, r2), g = (int) Mth.lerp(progress, g1, g2), b = (int) Mth.lerp(progress, b1, b2);
        return (r << 16) | (g << 8) | b;
    }
}
