package com.deltasf.createpropulsion.physics_assembler;

import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;

public class AssemblyUtility {

    public static final int PASSIVE_COLOR = 0xaf68c5;
    public static final int HIGHLIGHT_COLOR = 0xda97f0;
    public static final int CANCEL_COLOR = 0xFF5555;

    public static final int MAX_ASSEMBLY_SIZE = 32;
    public static final int MAX_RENDERED_OUTLINE_SIZE = 1024;

    public static void renderOutline(String key, AABB boundingBox, int color, float lineWidth, boolean showFace) {
        if (isAABBLarger(boundingBox, MAX_RENDERED_OUTLINE_SIZE)) {
            return; 
        }

        var outline = CreateClient.OUTLINER.showAABB(key, boundingBox)
            .colored(color)
            .lineWidth(lineWidth)
            .disableLineNormals();

        if (showFace) {
            outline.withFaceTexture(AllSpecialTextures.SELECTION);
        }
    }

    public static int lerpColor(float progress, int color1, int color2) {
        int r1 = (color1 >> 16) & 0xFF, g1 = (color1 >> 8) & 0xFF, b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF, g2 = (color2 >> 8) & 0xFF, b2 = color2 & 0xFF;
        int r = (int) Mth.lerp(progress, r1, r2), g = (int) Mth.lerp(progress, g1, g2), b = (int) Mth.lerp(progress, b1, b2);
        return (r << 16) | (g << 8) | b;
    }

    public static boolean isAssemblyGauge(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof AssemblyGaugeItem;
    }

    public static BlockPos getTargetedPosition(BlockPos pos, net.minecraft.core.Direction face) {
        return pos.relative(face);
    }


    public static AABB fromBlockVolumes(BlockPos posA, BlockPos posB) {
        return new AABB(posA).minmax(new AABB(posB));
    }

    @SuppressWarnings("null")
    public static boolean isPlayerLookingAtAABB(Player player, AABB box, float partialTicks, double reachBonus, double inflation) {
        Level level = player.level();
        if (level == null) {
            return false;
        }

        Vec3 boxCenter = box.getCenter();
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, boxCenter.x, boxCenter.y, boxCenter.z);

        Vec3 eyePos = player.getEyePosition(partialTicks);
        Attribute reachAttr = ForgeMod.BLOCK_REACH.get();
        if (reachAttr == null) return false;
        double reach = player.getAttribute(reachAttr).getValue() + reachBonus;
        Vec3 lookVec = player.getViewVector(partialTicks);
        Vec3 endPos = eyePos.add(lookVec.scale(reach));
        
        AABB inflatedBox = box.inflate(inflation);

        if (ship == null) {
            return inflatedBox.contains(eyePos) || inflatedBox.clip(eyePos, endPos).isPresent();
        } else {
            Matrix4dc worldToShip = ship.getTransform().getWorldToShip();
            Vector3d worldEyePos = new Vector3d(eyePos.x, eyePos.y, eyePos.z);
            Vector3d worldEndPos = new Vector3d(endPos.x, endPos.y, endPos.z);

            Vector3d shipEyePos = worldToShip.transformPosition(worldEyePos, new Vector3d());
            Vector3d shipEndPos = worldToShip.transformPosition(worldEndPos, new Vector3d());

            Vec3 shipRayStart = new Vec3(shipEyePos.x, shipEyePos.y, shipEyePos.z);
            Vec3 shipRayEnd = new Vec3(shipEndPos.x, shipEndPos.y, shipEndPos.z);
            return inflatedBox.contains(shipRayStart) || inflatedBox.clip(shipRayStart, shipRayEnd).isPresent();
        }
    }

    public static boolean isAABBLarger(AABB aabb, int maxSize) {
        return (aabb.getXsize() > maxSize ||
                aabb.getYsize() > maxSize ||
                aabb.getZsize() > maxSize);
    }
}
