package com.deltasf.createpropulsion.utility.value_boxes;

import com.simibubi.create.AllItems;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox.TextValueBox;

import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class DualRowValueRenderer {

    @SuppressWarnings("null")
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        HitResult target = mc.hitResult;
        if (!(target instanceof BlockHitResult result))
            return;

        ClientLevel world = mc.level;
        if (world == null)
            return;
            
        BlockPos pos = result.getBlockPos();

        if (!(world.getBlockEntity(pos) instanceof SmartBlockEntity sbe))
            return;

        boolean highlightFound = false;

        for (BlockEntityBehaviour behaviour : sbe.getAllBehaviours()) {
            if (!(behaviour instanceof DualRowValueBehaviour dualRowBehaviour))
                continue;

            if (!dualRowBehaviour.isActive()) {
                Outliner.getInstance().remove(pos);
                continue;
            }

            ItemStack mainhandItem = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
            if (dualRowBehaviour.onlyVisibleWithWrench() && !AllItems.WRENCH.isIn(mainhandItem)) {
                Outliner.getInstance().remove(pos);
                continue;
            }
            
            boolean highlight = dualRowBehaviour.testHit(target.getLocation());
            if (highlightFound)
                highlight = false;

            renderValueBox(world, pos, result.getDirection(), dualRowBehaviour, highlight);

            if (!highlight)
                continue;

            highlightFound = true;
            List<MutableComponent> tip = new ArrayList<>();
            tip.add(dualRowBehaviour.label.copy());
            tip.add(CreateLang.translateDirect("gui.value_settings.hold_to_edit"));
            CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
        }
    }

    protected static void renderValueBox(ClientLevel world, BlockPos pos, Direction face, DualRowValueBehaviour behaviour, boolean highlight) {
        AABB bb = new AABB(Vec3.ZERO, Vec3.ZERO).inflate(.5f)
            .contract(0, 0, -.5f)
            .move(0, 0, -.125f);
            
        Component label = behaviour.label;
        ValueBox box = new TextValueBox(label, bb, pos, Component.literal(behaviour.formatValue()));

        box.passive(!highlight)
           .wideOutline();

        Outliner.getInstance().showOutline(pos, box.transform(behaviour.slotPositioning))
			.highlightFace(face);
    }
}
