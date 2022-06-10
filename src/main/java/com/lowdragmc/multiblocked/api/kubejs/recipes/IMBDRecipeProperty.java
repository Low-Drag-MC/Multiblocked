package com.lowdragmc.multiblocked.api.kubejs.recipes;

import dev.latvian.mods.kubejs.item.ingredient.IngredientJS;

public interface IMBDRecipeProperty {
    boolean isPerTick();

    String atSlot();
}
