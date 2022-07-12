package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.gui.recipe.RecipeWidget;
import dev.latvian.mods.kubejs.event.EventJS;

public class RecipeUIEvent extends EventJS {
    public static final String ID = "mbd.recipe_ui";

    private final RecipeWidget recipeWidget;

    public RecipeUIEvent(RecipeWidget recipeWidget) {
        this.recipeWidget = recipeWidget;
    }

    public RecipeWidget getRecipeWidget() {
        return recipeWidget;
    }
}
