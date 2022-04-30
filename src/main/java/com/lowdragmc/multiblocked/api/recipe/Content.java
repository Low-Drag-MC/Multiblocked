package com.lowdragmc.multiblocked.api.recipe;

import net.minecraft.util.Tuple;

public class Content {
    public final Object content;
    public final float chance;
    
    public Content(Tuple<Object, Float> tuple) {
        content = tuple.getA();
        chance = tuple.getB();
    }
    
}
