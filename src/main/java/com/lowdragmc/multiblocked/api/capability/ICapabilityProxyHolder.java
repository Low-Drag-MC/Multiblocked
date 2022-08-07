package com.lowdragmc.multiblocked.api.capability;

import com.google.common.collect.Table;
import com.lowdragmc.multiblocked.api.capability.proxy.CapabilityProxy;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;


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

    default Map<BlockPos, CapabilityProxy<?>> getProxies(IO io, MultiblockCapability<?> capability){
        Long2ObjectOpenHashMap<CapabilityProxy<?>> map = getCapabilitiesProxy().get(io, capability);
        Map<BlockPos, CapabilityProxy<?>> result = new HashMap<>();
        if (map != null) {
            map.forEach((p, p2) -> result.put(BlockPos.of(p), p2));
        }
        return result;
    }

    static ICapabilityProxyHolder fromWorldPos(World world, BlockPos pos, MultiblockCapability<?>... capabilities) {
        return new CommonCapabilityProxyHolder(world, pos, capabilities);
    }

}
