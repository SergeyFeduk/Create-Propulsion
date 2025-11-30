package com.deltasf.createpropulsion.heat.engine;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.deltasf.createpropulsion.utility.value_boxes.DualRowValueBehaviour;
import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/// testHit was stolen from VS <3
/// https://github.com/ValkyrienSkies/Valkyrien-Skies-2/blob/1.20.1/main/common/src/main/java/org/valkyrienskies/mod/mixin/mod_compat/create/behaviour/MixinScrollValueBehaviour.java

public class StirlingScrollValueBehaviour extends DualRowValueBehaviour {
    protected static final int STEP = 64;
    protected static final int OPTIONS_PER_ROW = 3;

    public StirlingScrollValueBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot) {
        super(label, be, slot);
        this.withFormatter(v -> Integer.toString(getUnsignedRPM()));
    }

    public int getRPM() {
        return getValue() * STEP;
    }

    public int getUnsignedRPM() {
        return Math.abs(getValue()) * STEP;
    }

    private int getRpmFromBoardIndex(int boardIndex) {
        return (boardIndex + 1) * STEP;
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        ImmutableList<Component> rows = ImmutableList.of(
            Component.literal("\u27f3").withStyle(ChatFormatting.BOLD),
            Component.literal("\u27f2").withStyle(ChatFormatting.BOLD)
        );
        ValueSettingsFormatter formatter = new ValueSettingsFormatter(this::formatSettings);
        return new ValueSettingsBoard(label, OPTIONS_PER_ROW, 1, rows, formatter);
    }

    @Override
    public void setValueSettings(Player player, ValueSettings valueSetting, boolean ctrlHeld) {
        int internalValue = valueSetting.value() + 1;
        int newValue = valueSetting.row() == 0 ? -internalValue : internalValue;

        if (this.getValue() == newValue) return;

        setValue(newValue);
        playFeedbackSound(this);
    }

    @Override
    public ValueSettings getValueSettings() {
        int row = getValue() < 0 ? 0 : 1;
        int index = Math.abs(getValue()) - 1;
        return new ValueSettings(row, index);
    }

    @Override
    public boolean testHit(Vec3 hit) {
        BlockState state = blockEntity.getBlockState();
        Vec3 pos1 = hit;
        Vec3 pos2 = Vec3.atLowerCornerOf(blockEntity.getBlockPos());
        Level level = blockEntity.getLevel();
        if (level != null) {
            Ship ship1 = VSGameUtilsKt.getShipManagingPos(level, pos1.x, pos1.y, pos1.z);
            Ship ship2 = VSGameUtilsKt.getShipManagingPos(level, pos2.x, pos2.y, pos2.z);
            if (ship1 != null && ship2 == null) {
                pos2 = VectorConversionsMCKt.toMinecraft(ship1.getWorldToShip().transformPosition(VectorConversionsMCKt.toJOML(pos2)));
            } else if (ship1 == null && ship2 != null) {
                pos1 = VectorConversionsMCKt.toMinecraft(ship2.getWorldToShip().transformPosition(VectorConversionsMCKt.toJOML(pos1)));
            }
        }
        
        Vec3 localHit = pos1.subtract(pos2);
        return getSlotPositioning().testHit(blockEntity.getLevel(), blockEntity.getBlockPos(), state, localHit);
    }

    public MutableComponent formatSettings(ValueSettings settings) {
        return CreateLang.number(getRpmFromBoardIndex(settings.value()))
            .add(CreateLang.text(settings.row() == 0 ? "\u27f3" : "\u27f2")
            .style(ChatFormatting.BOLD))
            .component();
    }
}
