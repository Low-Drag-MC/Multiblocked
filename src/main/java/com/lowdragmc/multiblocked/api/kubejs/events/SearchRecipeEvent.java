package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.pattern.util.PatternMatchContext;
import com.lowdragmc.multiblocked.api.recipe.DynamicRecipeHandler;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.RecipeLogic;
import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.stream.Collectors;

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

    public Map<BlockState,Integer> getComponentData() {
        Level level = recipeLogic.controller.self().getLevel();
        return recipeLogic.controller.getMultiblockState()
                .getCache()
                .stream()
                .map(pos -> level.getBlockState(pos))
                .collect(Collectors.toMap(state -> state, state -> 1, Integer::sum));
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
