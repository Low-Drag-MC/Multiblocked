package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.RecipeLogic;
import dev.latvian.mods.kubejs.event.EventJS;

public class RecipeFinishEvent extends EventJS {
    public static final String ID = "mbd.recipe_finish";
    private final RecipeLogic recipeLogic;
    private Recipe recipe;

    public RecipeFinishEvent(RecipeLogic recipeLogic) {
        this.recipeLogic = recipeLogic;
        this.recipe = recipeLogic.lastRecipe;
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
