package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.RecipeLogic;
import dev.latvian.kubejs.event.EventJS;

public class SetupRecipeEvent extends EventJS {
    public static final String ID = "mbd.setup_recipe";
    private final RecipeLogic recipeLogic;
    private Recipe recipe;

    public SetupRecipeEvent(RecipeLogic recipeLogic, Recipe recipe) {
        this.recipeLogic = recipeLogic;
        this.recipe = recipe;
    }

    public RecipeLogic getRecipeLogic() {
        return recipeLogic;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    @Override
    public boolean canCancel() {
        return true;
    }
}
