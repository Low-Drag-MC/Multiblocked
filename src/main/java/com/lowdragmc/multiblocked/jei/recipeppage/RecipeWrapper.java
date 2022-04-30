package com.lowdragmc.multiblocked.jei.recipeppage;

import com.lowdragmc.lowdraglib.jei.ModularWrapper;
import com.lowdragmc.multiblocked.api.gui.recipe.RecipeWidget;
import com.lowdragmc.multiblocked.api.recipe.Recipe;

public class RecipeWrapper extends ModularWrapper<RecipeWidget> {

    public final Recipe recipe;

    public RecipeWrapper(RecipeWidget widget) {
        super(widget);
        recipe = widget.recipe;
    }
}
