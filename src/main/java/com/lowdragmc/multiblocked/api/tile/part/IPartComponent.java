package com.lowdragmc.multiblocked.api.tile.part;

import com.lowdragmc.multiblocked.api.definition.StatusProperties;
import com.lowdragmc.multiblocked.api.tile.IComponent;
import com.lowdragmc.multiblocked.api.tile.IControllerComponent;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author KilaBash
 * @date 2022/06/02
 * @implNote IPartComponent
 */
public interface IPartComponent extends IComponent {
    List<? extends IControllerComponent> getControllers();

    @Override
    default boolean isFormed() {
        for (IControllerComponent controller : getControllers()) {
            if (controller.isFormed()) {
                return true;
            }
        }
        return false;
    }

    @Override
    default String getStatus() {
        boolean isIdle = false;
        for (IControllerComponent controller : getControllers()) {
            if (controller.getStatus().equals(StatusProperties.IDLE)) {
                isIdle = true;
            }
            if (controller.getStatus().equals(StatusProperties.WORKING)) {
                return StatusProperties.WORKING;
            }
        }
        return isIdle ? StatusProperties.IDLE : StatusProperties.UNFORMED;
    }

    void addedToController(@Nonnull IControllerComponent controller);

    void removedFromController(@Nonnull IControllerComponent controller);

    boolean canShared();

    boolean hasController(BlockPos controllerPos);
}
