package com.lowdragmc.multiblocked.jei;

import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.gui.recipe.RecipeWidget;
import com.lowdragmc.multiblocked.api.kubejs.events.RecipeUIEvent;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import com.lowdragmc.multiblocked.jei.multipage.MultiblockInfoCategory;
import com.lowdragmc.multiblocked.jei.recipeppage.RecipeMapCategory;
import com.lowdragmc.multiblocked.jei.recipeppage.RecipeWrapper;
import dev.latvian.mods.kubejs.script.ScriptType;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.stream.Collectors;

/**
 * @author KilaBash
 * @date 2022/04/30
 * @implNote jei plugin
 */
@JeiPlugin
public class JEIPlugin implements IModPlugin {

    public JEIPlugin() {
        Multiblocked.LOGGER.debug("Multiblocked JEI Plugin created");
    }

    @Override
    public void registerCategories(@Nonnull IRecipeCategoryRegistration registry) {
        Multiblocked.LOGGER.info("JEI register categories");
        IJeiHelpers jeiHelpers = registry.getJeiHelpers();
        registry.addRecipeCategories(new MultiblockInfoCategory(jeiHelpers));
        for (RecipeMap recipeMap : RecipeMap.RECIPE_MAP_REGISTRY.values()) {
            if (recipeMap == RecipeMap.EMPTY) continue;
            registry.addRecipeCategories(new RecipeMapCategory(jeiHelpers, recipeMap));
        }
    }

    @Override
    public void registerRecipeCatalysts(@Nonnull IRecipeCatalystRegistration registration) {
        MultiblockInfoCategory.registerRecipeCatalysts(registration);
    }

    @Override
    public void registerRecipes(@Nonnull IRecipeRegistration registration) {
        Multiblocked.LOGGER.info("JEI register");
        for (RecipeMap recipeMap : RecipeMap.RECIPE_MAP_REGISTRY.values()) {
            if (recipeMap == RecipeMap.EMPTY) continue;
            registration.addRecipes(recipeMap.recipes.values()
                            .stream()
                            .map(recipe -> {
                                RecipeWidget recipeWidget = new RecipeWidget(recipe, recipeMap.progressTexture);
                                if (Multiblocked.isKubeJSLoaded()) {
                                    new RecipeUIEvent(recipeWidget).post(ScriptType.CLIENT, RecipeUIEvent.ID, recipeMap.name);
                                }
                                return recipeWidget;
                            })
                            .map(RecipeWrapper::new)
                            .collect(Collectors.toList()), 
                    new ResourceLocation(Multiblocked.MODID, recipeMap.name));
        }
        MultiblockInfoCategory.registerRecipes(registration);
    }

    @Override
    public void registerIngredients(@Nonnull IModIngredientRegistration registry) {
        Multiblocked.LOGGER.info("JEI register ingredients");
    }

    @Override
    @Nonnull
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(Multiblocked.MODID, "jei_plugin");
    }
}
