package com.lowdragmc.multiblocked.common.capability.trait;

import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.trait.ProgressCapabilityTrait;
import com.lowdragmc.multiblocked.api.recipe.RecipeLogic;
import com.lowdragmc.multiblocked.api.tile.IControllerComponent;

/**
 * @author KilaBash
 * @date 2022/11/15
 * @implNote RecipeProgressTrait
 */
public class RecipeProgressTrait extends ProgressCapabilityTrait {

    public RecipeProgressTrait(MultiblockCapability<?> capability) {
        super(capability);
    }

    @Override
    protected String dynamicHoverTips(double progress) {
        return LocalizationUtils.format("multiblocked.top.recipe_progress", ((int)(progress * 100)) + "%");
    }

    @Override
    protected double getProgress() {
        if (component instanceof IControllerComponent component) {
            RecipeLogic recipeLogic = component.getRecipeLogic();
            return recipeLogic == null ? 0 : (recipeLogic.progress * 1. / recipeLogic.duration);
        }
        return 0;
    }

}
