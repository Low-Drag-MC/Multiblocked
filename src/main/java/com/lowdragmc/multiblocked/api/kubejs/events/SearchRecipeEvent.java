package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.pattern.util.PatternMatchContext;
import com.lowdragmc.multiblocked.api.recipe.DynamicRecipeHandler;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.RecipeLogic;
import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

import java.util.Map;

public class SearchRecipeEvent extends EventJS {
    public static final String ID = "mbd.search_recipe";

    private final RecipeLogic recipeLogic;
    private Recipe recipe;

    public SearchRecipeEvent(RecipeLogic logic) {
        this.recipeLogic = logic;
    }

    public DynamicRecipeHandler getHandler() {
        return DynamicRecipeHandler.create();
    }

    public PatternMatchContext getMatchContext() {
        return recipeLogic.controller.getMultiblockState().matchContext;
    }

    public Map<Block,Integer> getComponentData() {
        return getMatchContext().get("components");
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

}
