package com.lowdragmc.multiblocked.api.capability;

import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.lowdragmc.multiblocked.api.capability.proxy.CapabilityProxy;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.EnumMap;

public class CommonCapabilityProxyHolder implements ICapabilityProxyHolder{
    protected Table<IO, MultiblockCapability<?>, Long2ObjectOpenHashMap<CapabilityProxy<?>>> capabilities;

    public CommonCapabilityProxyHolder(Level world, BlockPos pos, MultiblockCapability<?>... capability) {
        BlockEntity te = world.getBlockEntity(pos);
        if (te != null) {
            capabilities = Tables.newCustomTable(new EnumMap<>(IO.class), Object2ObjectOpenHashMap::new);
            for (MultiblockCapability<?> cap : capability) {
                if (cap.isBlockHasCapability(IO.BOTH, te)) {
                    capabilities.put(IO.BOTH, cap, new Long2ObjectOpenHashMap<>());
                    capabilities.get(IO.BOTH, cap).put(te.getBlockPos().asLong(), cap.createProxy(IO.BOTH, te));
                    continue;
                }
                if (cap.isBlockHasCapability(IO.IN, te)) {
                    capabilities.put(IO.IN, cap, new Long2ObjectOpenHashMap<>());
                    capabilities.get(IO.IN, cap).put(te.getBlockPos().asLong(), cap.createProxy(IO.IN, te));
                } else if (cap.isBlockHasCapability(IO.OUT, te)) {
                    capabilities.put(IO.OUT, cap, new Long2ObjectOpenHashMap<>());
                    capabilities.get(IO.OUT, cap).put(te.getBlockPos().asLong(), cap.createProxy(IO.OUT, te));
                }
            }
        }
    }

    public CommonCapabilityProxyHolder(Table<IO, MultiblockCapability<?>, Long2ObjectOpenHashMap<CapabilityProxy<?>>> capabilities) {
        this.capabilities = capabilities;
    }

    public ICapabilityProxyHolder mergeWith(ICapabilityProxyHolder capabilityProxyHolder) {
        if (hasProxies() && capabilityProxyHolder.hasProxies()) {
            Table<IO, MultiblockCapability<?>, Long2ObjectOpenHashMap<CapabilityProxy<?>>> capabilities = Tables.newCustomTable(new EnumMap<>(IO.class), Object2ObjectOpenHashMap::new);
            capabilities.putAll(capabilities);
            capabilities.putAll(capabilityProxyHolder.getCapabilitiesProxy());
            return new CommonCapabilityProxyHolder(capabilities);
        } else {
            return hasProxies() ? this : capabilityProxyHolder;
        }
    }

    @Override
    public Table<IO, MultiblockCapability<?>, Long2ObjectOpenHashMap<CapabilityProxy<?>>> getCapabilitiesProxy() {
        return capabilities;
    }
}
