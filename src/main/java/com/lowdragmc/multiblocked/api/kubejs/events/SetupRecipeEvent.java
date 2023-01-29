package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.pattern.util.PatternMatchContext;
import com.lowdragmc.multiblocked.api.recipe.*;
import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.world.level.block.Block;

import java.util.Map;

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

    public DynamicRecipeHandler getHandlerFromRecipe() {
        return DynamicRecipeHandler.from(recipe);
    }

    public PatternMatchContext getMatchContext() {
        return recipeLogic.controller.getMultiblockState().matchContext;
    }

    public Map<Block,Integer> getComponentData() {
        return getMatchContext().get("components");
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    @Override
    public boolean canCancel() {
        return true;
    }
}
