package com.deltasf.createpropulsion.mixin;

import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.mod.common.VSClientGameUtils;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.deltasf.createpropulsion.mixin.plugin.MixinIf;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.world.phys.Vec3;

@MixinIf("is_vsaddition_not_loaded") //If loaded - similar mixin is applied by vs additions
@Pseudo
@Mixin(CompassItemPropertyFunction.class)
public abstract class CompassItemPropertyFunctionMixin {

    /// This mixin fixes the model selection of the compass item in inventories
    /// If target position is on client ship - ship transform is applied to the original ship-space position to convert it to world-space with respect to the ship
    
    @ModifyExpressionValue(
        method = "getAngleFromEntityToPos",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/phys/Vec3;atCenterOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private Vec3 transformToWorldSpace(Vec3 pos) {
        Vector3d position = VectorConversionsMCKt.toJOML(pos);
        ClientShip ship = VSClientGameUtils.getClientShip(position.x, position.y, position.z);
        if (ship == null) {
            return pos;
        } 
        return VectorConversionsMCKt.toMinecraft(ship.getRenderTransform().getShipToWorld().transformPosition(position));
    }
}
