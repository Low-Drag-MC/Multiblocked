package com.lowdragmc.multiblocked.jei.recipepage;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.jei.ModularWrapper;
import com.lowdragmc.multiblocked.api.recipe.Recipe;

public class RecipeWrapper extends ModularWrapper<Widget> {

    public final Recipe recipe;

    public RecipeWrapper(Widget widget, Recipe recipe) {
        super(widget);
        this.recipe = recipe;
    }

    @Override
    public String getUid() {
        return recipe.uid;
    }
}
