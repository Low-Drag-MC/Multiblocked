package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import dev.latvian.mods.kubejs.event.EventJS;

public class RecipeUIEvent extends EventJS {
    public static final String ID = "mbd.recipe_ui";

    private final WidgetGroup recipeWidget;

    public RecipeUIEvent(WidgetGroup recipeWidget) {
        this.recipeWidget = recipeWidget;
    }

    public WidgetGroup getRecipeWidget() {
        return recipeWidget;
    }
}
