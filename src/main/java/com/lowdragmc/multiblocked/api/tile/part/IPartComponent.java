package com.lowdragmc.multiblocked.api.tile.part;

import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.kubejs.events.PartAddedEvent;
import com.lowdragmc.multiblocked.api.kubejs.events.PartRemovedEvent;
import com.lowdragmc.multiblocked.api.tile.IComponent;
import com.lowdragmc.multiblocked.api.tile.IControllerComponent;
import dev.latvian.kubejs.script.ScriptType;
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


    void addedToController(@Nonnull IControllerComponent controller);

    void removedFromController(@Nonnull IControllerComponent controller);

    boolean canShared();

    boolean hasController(BlockPos controllerPos);
}
