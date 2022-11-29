package com.lowdragmc.multiblocked.jei.recipepage;

import com.lowdragmc.lowdraglib.jei.ModularWrapper;
import com.lowdragmc.multiblocked.api.gui.recipe.FuelWidget;
import com.lowdragmc.multiblocked.api.recipe.Recipe;

public class FuelWrapper extends ModularWrapper<FuelWidget> {

    public final Recipe recipe;

    public FuelWrapper(FuelWidget widget) {
        super(widget);
        recipe = widget.recipe;
    }
}
