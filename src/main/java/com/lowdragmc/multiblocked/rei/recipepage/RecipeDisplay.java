package com.lowdragmc.multiblocked.rei.recipepage;

import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.rei.ModularDisplay;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.gui.recipe.RecipeWidget;
import com.lowdragmc.multiblocked.api.kubejs.events.RecipeUIEvent;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import dev.latvian.mods.kubejs.script.ScriptType;

public class RecipeDisplay extends ModularDisplay<RecipeWidget> {

    public RecipeDisplay(RecipeMap recipeMap, Recipe recipe) {
        super(() -> {
            RecipeWidget recipeWidget = new RecipeWidget(
                    recipeMap,
                    recipe,
                    ProgressWidget.JEIProgress,
                    ProgressWidget.JEIProgress);
            if (Multiblocked.isKubeJSLoaded()) {
                new RecipeUIEvent(recipeWidget).post(ScriptType.CLIENT, RecipeUIEvent.ID, recipeMap.name);
            }
            return recipeWidget;
        }, RecipeMapDisplayCategory.CATEGORIES.apply(recipeMap));
    }
}
