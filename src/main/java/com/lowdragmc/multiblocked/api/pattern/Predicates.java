package com.lowdragmc.multiblocked.api.pattern;

import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.pattern.predicates.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class Predicates {

    public static TraceabilityPredicate states(BlockState... allowedStates) {
        return new TraceabilityPredicate(new PredicateStates(allowedStates));
    }

    public static TraceabilityPredicate blocks(Block... blocks) {
        return new TraceabilityPredicate(new PredicateBlocks(blocks));
    }

    public static TraceabilityPredicate fluids(Fluid... fluids) {
        return new TraceabilityPredicate(new PredicateFluids(fluids));
    }

    /**
     * Use it when you require that a position must have a specific capability.
     */
    public static TraceabilityPredicate anyCapability(MultiblockCapability<?> capability) {
        return new TraceabilityPredicate(new PredicateAnyCapability(capability));
    }

    public static TraceabilityPredicate component(ComponentDefinition definition) {
        TraceabilityPredicate predicate = new TraceabilityPredicate(new PredicateComponent(definition));
        return definition instanceof ControllerDefinition ? predicate.setCenter() : predicate;
    }

    public static TraceabilityPredicate customAny() {
        return new TraceabilityPredicate(new PredicateCustomAny());
    }

    public static TraceabilityPredicate custom(Predicate<MultiblockState> predicate, Supplier<BlockInfo[]> candidates) {
        return new TraceabilityPredicate(predicate, candidates);
    }

    public static TraceabilityPredicate any() {
        return new TraceabilityPredicate(SimplePredicate.ANY);
    }

    public static TraceabilityPredicate air() {
        return new TraceabilityPredicate(SimplePredicate.AIR);

    }
}
