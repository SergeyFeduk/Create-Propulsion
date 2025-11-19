package com.deltasf.createpropulsion.heat.engine;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A BlockEntityBehaviour that manages an integer value selected from a two-row
 * UI board.
 * This behaviour is distinct from ScrollValueBehaviour and implements its own logic.
 * <p>
 * The value system is divided into two ranges:
 * - Row 0: Negative values from -256 to -1.
 * - Row 1: Positive values from 1 to 256.
 * The value 0 is never used or selectable.
 */
public class DualRowValueBehaviour extends BlockEntityBehaviour implements ValueSettingsBehaviour {

	public static final BehaviourType<DualRowValueBehaviour> TYPE = new BehaviourType<>();

	ValueBoxTransform slotPositioning;
	public Component label;
	public int value;

	private Consumer<Integer> callback;
	private Supplier<Boolean> isActive;
	private boolean needsWrench;
	private Function<Integer, String> formatter;

	// Constants for the value range
	private static final int MAX_ABSOLUTE_VALUE = 256;
	private static final int OPTIONS_PER_ROW = 256;
	private static final int COLUMNS = 16; // Number of columns to display in the UI board

	public DualRowValueBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot) {
		super(be);
		this.label = label;
		this.slotPositioning = slot;
		this.callback = i -> {};
		this.isActive = () -> true;
		this.formatter = i -> Integer.toString(i);
		this.value = 1; // Default to a valid, non-zero value
	}

	@Override
	public BehaviourType<?> getType() {
		return TYPE;
	}
	
	@Override
	public boolean isSafeNBT() {
		return true;
	}

	@Override
	public void write(CompoundTag nbt, boolean clientPacket) {
		nbt.putInt("DualRowValue", value);
		super.write(nbt, clientPacket);
	}

	@Override
	public void read(CompoundTag nbt, boolean clientPacket) {
		value = nbt.getInt("DualRowValue");
		super.read(nbt, clientPacket);
	}

	// Builder-style methods for configuration
	
	public DualRowValueBehaviour withCallback(Consumer<Integer> valueCallback) {
		this.callback = valueCallback;
		return this;
	}

	public DualRowValueBehaviour requiresWrench() {
		this.needsWrench = true;
		return this;
	}

	public DualRowValueBehaviour withFormatter(Function<Integer, String> formatter) {
		this.formatter = formatter;
		return this;
	}

	public DualRowValueBehaviour onlyActiveWhen(Supplier<Boolean> condition) {
		this.isActive = condition;
		return this;
	}

	// Value management
	
	public void setValue(int newValue) {
		// Ensure value never becomes 0. If it does, default to 1.
		if (newValue == 0)
			newValue = 1;
		
		// Clamp the value to the valid ranges: [-256, -1] and [1, 256]
		newValue = Mth.clamp(newValue, -MAX_ABSOLUTE_VALUE, MAX_ABSOLUTE_VALUE);
		
		if (newValue == this.value)
			return;
			
		this.value = newValue;
		callback.accept(value);
		blockEntity.setChanged();
		blockEntity.sendData();
	}

	public int getValue() {
		return value;
	}
	
	public String formatValue() {
		return formatter.apply(value);
	}

	// Implementation of ValueSettingsBehaviour

	@Override
	public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
		// Define labels for the two rows
		Component negativeLabel = Components.translatable("create.behaviour.dual_row.negative_values");
		Component positiveLabel = Components.translatable("create.behaviour.dual_row.positive_values");
		List<Component> rowLabels = ImmutableList.of(negativeLabel, positiveLabel);

		// Custom formatter to display the correct value in the UI board
		// It translates the board's (row, index) into the actual value
		ValueSettingsFormatter boardFormatter = new ValueSettingsFormatter((settings) -> {
			if (settings.row() == 0) { // Negative row
				// Index 0 -> -1, Index 1 -> -2, ...
				return Lang.number(-(settings.value() + 1)).component();
			}
			// Positive row (row == 1)
			// Index 0 -> 1, Index 1 -> 2, ...
			return Lang.number(settings.value() + 1).component();
		});

		return new ValueSettingsBoard(label, OPTIONS_PER_ROW, COLUMNS, rowLabels, boardFormatter);
	}

	@Override
	public void setValueSettings(Player player, ValueSettings valueSetting, boolean ctrlDown) {
		int newValue;
		if (valueSetting.row() == 0) { // Player clicked on the negative row
			// Index 0 -> -1, Index 1 -> -2, ...
			newValue = -(valueSetting.value() + 1);
		} else { // Player clicked on the positive row
			// Index 0 -> 1, Index 1 -> 2, ...
			newValue = valueSetting.value() + 1;
		}

		if (this.value == newValue)
			return;

		setValue(newValue);
		playFeedbackSound(this);
	}

	/**
	 * Translates the internal integer value into a {@link ValueSettings} object
	 * that the UI board can use to highlight the currently selected option.
	 */
	@Override
	public ValueSettings getValueSettings() {
		if (value < 0) {
			// Negative values belong to row 0.
			// Convert the value to a 0-based index.
			// e.g., -1 -> index 0, -2 -> index 1, ..., -256 -> index 255
			int index = -value - 1;
			return new ValueSettings(0, index);
		}
		
		// Positive values belong to row 1.
		// Convert the value to a 0-based index.
		// e.g., 1 -> index 0, 2 -> index 1, ..., 256 -> index 255
		int index = value - 1;
		return new ValueSettings(1, index);
	}

	@Override
	public boolean isActive() {
		return isActive.get();
	}
	
	@Override
	public boolean onlyVisibleWithWrench() {
		return needsWrench;
	}

	@Override
	public ValueBoxTransform getSlotPositioning() {
		return slotPositioning;
	}

	@Override
	public boolean testHit(Vec3 hit) {
		BlockState state = blockEntity.getBlockState();
		Vec3 localHit = hit.subtract(Vec3.atLowerCornerOf(blockEntity.getBlockPos()));
		return slotPositioning.testHit(state, localHit);
	}

	@Override
	public void onShortInteract(Player player, InteractionHand hand, Direction side) {
		// Not used for direct value manipulation in this behaviour, but kept for interface compliance
	}
}
