package com.deltasf.createpropulsion.lodestone_tracker;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.IItemHandler;

public class LodestoneTrackerItemHandler implements IItemHandler {
    private final LodestoneTrackerBlockEntity blockEntity;
    private static final int SLOT = 0;

    public LodestoneTrackerItemHandler(LodestoneTrackerBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot != SLOT) {
            return ItemStack.EMPTY;
        }
        return blockEntity.getCompass();
    }

    @NotNull
    @Override
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (slot != SLOT || stack.isEmpty() || stack.getItem() != Items.COMPASS) {
            return stack;
        }

        if (blockEntity.hasCompass()) {
            return stack;
        }

        ItemStack toInsert = stack.copy();
        toInsert.setCount(1);

        if (!simulate) {
            blockEntity.setCompass(toInsert, Direction.NORTH);
        }

        ItemStack remainder = stack.copy();
        remainder.shrink(1);
        return remainder;
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot != SLOT || amount < 1 || !blockEntity.hasCompass()) {
            return ItemStack.EMPTY;
        }

        ItemStack currentlyStored = blockEntity.getCompass();
        if (currentlyStored.isEmpty()) {
             return ItemStack.EMPTY;
        }

        ItemStack extracted = currentlyStored.copy();

        if (!simulate) {
             blockEntity.removeCompass();
        }

        return extracted;
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot == SLOT ? 1 : 0;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return slot == SLOT && stack.getItem() == Items.COMPASS && !blockEntity.hasCompass();
    }
}   
