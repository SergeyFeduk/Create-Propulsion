package com.deltasf.createpropulsion.balloons.utils;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.balloons.HaiGroup;
import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class BalloonScanUtils {
    //Returns distance to the LAST met HAB block above the hai. If no block found - returns -1
    //This is the best compromise I found
    public static int initialVerticalProbe(Level level, BlockPos origin) {
        //Obtain a ship
        Ship ship = VSGameUtilsKt.getShipManagingPos(level, origin);
        if (ship == null) return -1;
        //Obtain the topmost hab block
        int maxY = ship.getShipAABB().maxY();
        int highestHabY = -1;
        for(int y = origin.getY(); y < maxY; y++) {
            BlockState currenBlockState = level.getBlockState(new BlockPos(origin.getX(), y, origin.getZ()));
            if (HaiGroup.isHab(currenBlockState)) {
                highestHabY = y;
            }
        }
        if (highestHabY == -1) return -1; //Did not find a block, vertical probe failed
        return highestHabY - origin.getY();
    }

    public static AABB getMaxAABB(int probeResult, BlockPos origin) {
        int halfExtents = BalloonShipRegistry.MAX_HORIZONTAL_SCAN / 2;
        int halfExtentsMod = BalloonShipRegistry.MAX_HORIZONTAL_SCAN % 2;
        BlockPos posStart = new BlockPos(origin.getX() - halfExtents, origin.getY() + 1, origin.getZ() - halfExtents);
        BlockPos posEnd = new BlockPos(origin.getX() + halfExtents + halfExtentsMod, origin.getY() + 1 + probeResult, origin.getZ() + halfExtents + halfExtentsMod);

        return new AABB(posStart, posEnd);
    }
}
