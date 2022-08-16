package com.lowdragmc.multiblocked.api.tile;

import net.minecraft.util.Direction;

/**
 * @author KilaBash
 * @date 2022/06/02
 * @implNote IControllerComponent
 */
public interface IControllerComponent extends IComponent{
    boolean isWorking();

    @Override
    default boolean isValidFrontFacing(Direction up) {
        return IComponent.super.isValidFrontFacing(up) && up.getAxis() != Direction.Axis.Y;
    }
}
