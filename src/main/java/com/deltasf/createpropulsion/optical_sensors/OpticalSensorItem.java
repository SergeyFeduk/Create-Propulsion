package com.deltasf.createpropulsion.optical_sensors;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public class OpticalSensorItem extends BlockItem {
    public OpticalSensorItem(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }

    /*@Override
    public ItemStack getDefaultInstance() {
        // start with a fresh stack of THIS item
        ItemStack stack = new ItemStack(this);

        // build the BE-tag that Forge will look for
        CompoundTag beTag = new CompoundTag();
        ListTag lensesList = new ListTag();

        ItemStack defaultLens = new ItemStack(PropulsionItems.OPTICAL_LENS.get());
        // serialize the lens into its own tag
        lensesList.add(defaultLens.save(new CompoundTag()));

        // put *your* key (the one your BlockEntity reads) inside that BE-tag
        beTag.put(AbstractOpticalSensorBlockEntity.NBT_LENSES_KEY, lensesList);

        // now stick the whole thing under the vanilla key
        CompoundTag root = new CompoundTag();
        root.put(BlockItem.BLOCK_ENTITY_TAG, beTag);

        // apply to the stack
        stack.setTag(root);
        return stack;
    }*/
}
