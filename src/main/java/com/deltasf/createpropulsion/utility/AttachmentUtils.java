package com.deltasf.createpropulsion.utility;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class AttachmentUtils {
    private AttachmentUtils() {}

    @Nullable
    public static ServerShip getShipAt(ServerLevel serverLevel, BlockPos pos) {
        ServerShip ship = VSGameUtilsKt.getLoadedShipManagingPos(serverLevel, pos);
        if (ship == null) {
            ship = VSGameUtilsKt.getShipManagingPos(serverLevel, pos);
        }
        return ship;
    }

    public static <T> T getOrCreate(LoadedServerShip ship, Class<T> attachmentClass, Supplier<T> factory) {
        T attachment = ship.getAttachment(attachmentClass);
        if (attachment == null) {
            attachment = factory.get();
            ship.setAttachment(attachment);
        }
        return attachment;
    }

    @Nullable
    public static <T> T get(Level level, BlockPos pos, Class<T> attachmentClass, Supplier<T> factory) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null; //Can only get ships on the server
        }
        ServerShip ship = getShipAt(serverLevel, pos);
        return ship instanceof LoadedServerShip lship ? getOrCreate(lship, attachmentClass, factory) : null;
    }

}
