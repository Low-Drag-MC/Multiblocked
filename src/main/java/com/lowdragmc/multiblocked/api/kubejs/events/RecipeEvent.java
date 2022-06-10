package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.kubejs.recipes.RecipeMapJS;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import dev.latvian.mods.kubejs.event.EventJS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeEvent extends EventJS {
    public static final String ID = "mbd.recipe";
    public final transient List<RecipeMapJS> recipeMaps = new ArrayList<>();
    public final Map<String, RecipeMapJS> recipes;

    public RecipeEvent() {
        this.recipes = new HashMap<>();
        RecipeMap.RECIPE_MAP_REGISTRY.forEach((key, value) -> {
            var recipeMapJS = new RecipeMapJS(value);
            recipes.put(key, recipeMapJS);
            recipeMaps.add(recipeMapJS);
        });
    }

    public RecipeMapJS getRecipe(String recipeName) {
        return recipes.get(recipeName);
    }

    public RecipeMapJS createRecipe(String recipeName) {
        RecipeMap recipe = new RecipeMap(recipeName);
        RecipeMap.register(recipe);
        RecipeMapJS recipeMapJS = new RecipeMapJS(recipe);
        recipeMaps.add(recipeMapJS);
        recipes.put(recipeName, recipeMapJS);
        return recipeMapJS;
    }
}
