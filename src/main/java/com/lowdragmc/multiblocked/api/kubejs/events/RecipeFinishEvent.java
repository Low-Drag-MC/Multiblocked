package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.RecipeLogic;
import dev.latvian.mods.kubejs.event.EventJS;

public class RecipeFinishEvent extends EventJS {
    public static final String ID = "mbd.recipe_finish";
    private final RecipeLogic recipeLogic;

    public RecipeFinishEvent(RecipeLogic recipeLogic) {
        this.recipeLogic = recipeLogic;
    }

    public RecipeLogic getRecipeLogic() {
        return recipeLogic;
    }

    public Recipe getRecipe() {
        return recipeLogic.lastRecipe;
    }

    public void setRecipe(Recipe recipe) {
        recipeLogic.lastRecipe = recipe;
    }

    @Override
    public boolean canCancel() {
        return super.canCancel();
    }
}
