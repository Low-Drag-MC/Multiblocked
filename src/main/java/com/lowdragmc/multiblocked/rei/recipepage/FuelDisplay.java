package com.lowdragmc.multiblocked.rei.recipepage;

import com.lowdragmc.lowdraglib.rei.ModularDisplay;
import com.lowdragmc.multiblocked.api.gui.recipe.FuelWidget;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;

/**
 * @author KilaBash
 * @date 2022/11/29
 * @implNote FuelDisplay
 */
public class FuelDisplay extends ModularDisplay<FuelWidget> {
    public FuelDisplay(RecipeMap recipeMap, Recipe recipe) {
        super(() -> new FuelWidget(recipeMap, recipe), RecipeMapFuelDisplayCategory.CATEGORIES.apply(recipeMap));
    }
}
