package com.lowdragmc.multiblocked.api.capability;

import com.google.common.collect.Table;
import com.lowdragmc.multiblocked.api.capability.proxy.CapabilityProxy;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;


public interface ICapabilityProxyHolder {

    default boolean hasProxies() {
        return getCapabilitiesProxy() != null && !getCapabilitiesProxy().isEmpty();
    }

    default boolean hasProxy(IO io, MultiblockCapability<?> capability) {
        return hasProxies() && getCapabilitiesProxy().contains(io, capability);
    }

    default ICapabilityProxyHolder mergeWith(ICapabilityProxyHolder otherHolder) {
        return new CommonCapabilityProxyHolder(getCapabilitiesProxy()).mergeWith(otherHolder);
    }

    Table<IO, MultiblockCapability<?>, Long2ObjectOpenHashMap<CapabilityProxy<?>>> getCapabilitiesProxy();

    static ICapabilityProxyHolder fromWorldPos(Level world, BlockPos pos, MultiblockCapability<?>... capabilities) {
        return new CommonCapabilityProxyHolder(world, pos, capabilities);
    }

}
