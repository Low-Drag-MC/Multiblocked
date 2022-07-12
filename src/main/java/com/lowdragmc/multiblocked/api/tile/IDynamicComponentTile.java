package com.lowdragmc.multiblocked.api.tile;

import com.lowdragmc.multiblocked.api.capability.trait.CapabilityTrait;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author youyihj
 */
public interface IDynamicComponentTile<T> {
    Map<String, BiConsumer<T, CapabilityTrait>> getTraitSetters();
}
