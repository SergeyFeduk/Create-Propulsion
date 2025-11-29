package com.deltasf.createpropulsion.design_goggles;

import java.util.ArrayList;
import java.util.List;

import net.createmod.catnip.outliner.Outliner;
import org.joml.Quaterniond;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.deltasf.createpropulsion.balloons.Balloon;
import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.balloons.registries.BalloonRegistry;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.deltasf.createpropulsion.physics_assembler.AssemblyUtility;
import com.deltasf.createpropulsion.utility.OBBEntityFinder;
import com.deltasf.createpropulsion.utility.math.MathUtility;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.foundation.collision.Matrix3d;
import com.simibubi.create.foundation.collision.OrientedBB;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class DesignGogglesOverlayRenderer {
    public static final IGuiOverlay OVERLAY = DesignGogglesOverlayRenderer::renderOverlay;
    public static final double GLOBAL_INFLATION_RATE = 8;

    //private static BlockPos lastHovered = null;
    //private static int hoverTicks = 0;

    @SuppressWarnings("null")
    public static void renderOverlay(ForgeGui gui, GuiGraphics graphics, float partialTicks, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.gameMode.getPlayerMode() == GameType.SPECTATOR)
            return;

        boolean wearingDesignGoggles = DesignGogglesItem.isPlayerWearingGoggles(mc.player);
        if (!wearingDesignGoggles) return;
        //TODO: If near balloon - display holes with outliner
        List<Balloon> intersecting = new ArrayList<>();

        for(BalloonRegistry registry : BalloonShipRegistry.get().getRegistries()) {
            for(HaiGroup group : registry.getHaiGroups()) {
                ServerShip ship = group.getShip();
                for(Balloon balloon : group.balloons) {
                    
                    Quaterniond worldOrientation = OBBEntityFinder.calculateWorldOrientation(ship, Direction.NORTH, Direction.UP);
                    Matrix3d rotationMatrix = MathUtility.createMatrixFromQuaternion(worldOrientation);

                    AABB aabb = balloon.getAABB().inflate(GLOBAL_INFLATION_RATE);
                    Vec3 halfExtents = new Vec3(aabb.getXsize() * 0.5f, aabb.getYsize()* 0.5f ,aabb.getZsize()* 0.5f );

                    Vec3 centerInShipSpace = aabb.getCenter();
                    org.joml.Vector3d centerInShipJOML = VectorConversionsMCKt.toJOML(centerInShipSpace);
                    org.joml.Vector3d centerInWorldJOML = ship.getShipToWorld()
                                                            .transformPosition(centerInShipJOML, new org.joml.Vector3d());
                    Vec3 centerInWorldSpace = VectorConversionsMCKt.toMinecraft(centerInWorldJOML);


                    OrientedBB obb = new OrientedBB(centerInWorldSpace, halfExtents, rotationMatrix);

                    if (obb.intersect(mc.player.getBoundingBox()) != null) {
                        intersecting.add(balloon);
                    }
                }
            }
        }
        System.out.println(intersecting.size());

        List<BlockPos> holesPoses = new ArrayList<>();
        for(Balloon balloon : intersecting) {
            holesPoses.addAll(balloon.holes);
        }
        
        var f = Outliner.getInstance().showCluster("DesignHoles", holesPoses);
        f.colored(AssemblyUtility.CANCEL_COLOR);
        f.lineWidth(1/16f);
        f.withFaceTexture(AllSpecialTextures.CHECKERED);
        f.disableLineNormals();


        //TODO: If shifting - display COM
        //TODO: If looking at sphere located at COM - display tooltip with ship mass
        /*
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
        }*/
    }
}
