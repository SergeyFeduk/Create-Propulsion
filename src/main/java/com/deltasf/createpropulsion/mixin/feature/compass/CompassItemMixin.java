package com.deltasf.createpropulsion.mixin.feature.compass;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.mixin.plugin.MixinIf;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.level.Level;

@MixinIf("is_vsaddition_not_loaded") //If loaded - similar mixin is applied by vs additions
@Mixin(CompassItem.class)
public abstract class CompassItemMixin {

    /// This mixin fixes binding compass to lodestone located on ship
    /// If lodestone block is in shipyard - ship managing this pos in shipyard must actuall exist
    /// If it does not - do not bind to the lodestone, as it is located on dead ship
    
    @WrapOperation(
        method = "inventoryTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/village/poi/PoiManager;existsAtPosition(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/BlockPos;)Z"
        )
    )
    private boolean checkDoesShipExist(PoiManager instance, ResourceKey<PoiType> type, BlockPos pos, Operation<Boolean> original, @Local(argsOnly = true) Level level) {
        boolean doesBlockExist = original.call(instance, type, pos);
        if (!doesBlockExist) return false;
        if (VSGameUtilsKt.isBlockInShipyard(level, pos)) {
            return VSGameUtilsKt.getShipManagingPos(level, pos) != null;
        }
        
        return true; 
    }
}