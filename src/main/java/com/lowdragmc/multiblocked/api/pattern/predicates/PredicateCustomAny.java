package com.lowdragmc.multiblocked.api.pattern.predicates;

public class PredicateCustomAny extends SimplePredicate {

    public PredicateCustomAny() {
        super("custom_any", x -> true, null);
    }

}
