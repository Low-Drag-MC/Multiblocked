package com.lowdragmc.multiblocked.api.capability;

import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

public interface IInnerCapabilityProvider extends ICapabilityProvider {

    /**
     * inner capability used for recipe logic handling.
     */
    default <T> LazyOptional<T> getInnerCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        return getCapability(capability, facing);
    }

    /**
     * inner capability used for recipe logic handling with slotName.
     */
    default <T> LazyOptional<T> getInnerCapability(@Nonnull Capability<T> capability, @Nullable Direction facing, @Nullable String slotName) {
        return getInnerCapability(capability, facing);
    }

    /**
     * additional slot names
     */
    default Set<String> getSlotNames() {
        return Collections.emptySet();
    }
}
