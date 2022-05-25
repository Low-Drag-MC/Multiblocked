package com.lowdragmc.multiblocked.api.pattern;

import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.pattern.predicates.PredicateAnyCapability;
import com.lowdragmc.multiblocked.api.pattern.predicates.PredicateBlocks;
import com.lowdragmc.multiblocked.api.pattern.predicates.PredicateComponent;
import com.lowdragmc.multiblocked.api.pattern.predicates.PredicateStates;
import com.lowdragmc.multiblocked.api.pattern.predicates.SimplePredicate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class Predicates {

    public static TraceabilityPredicate states(BlockState... allowedStates) {
        return new TraceabilityPredicate(new PredicateStates(allowedStates));
    }

    public static TraceabilityPredicate blocks(Block... blocks) {
        return new TraceabilityPredicate(new PredicateBlocks(blocks));
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

    public static TraceabilityPredicate any() {
        return new TraceabilityPredicate(SimplePredicate.ANY);
    }

    public static TraceabilityPredicate air() {
        return new TraceabilityPredicate(SimplePredicate.AIR);

    }
}
