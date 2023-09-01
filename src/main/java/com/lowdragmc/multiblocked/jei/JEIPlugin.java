package com.lowdragmc.multiblocked.jei;

import com.lowdragmc.lowdraglib.LDLMod;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import com.lowdragmc.multiblocked.api.recipe.serde.recipe.MBDRecipeType;
import com.lowdragmc.multiblocked.jei.multipage.MultiblockInfoCategory;
import com.lowdragmc.multiblocked.jei.recipepage.RecipeMapCategory;
import com.lowdragmc.multiblocked.jei.recipepage.RecipeMapFuelCategory;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

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
        if (LDLMod.isReiLoaded()) return;
        Multiblocked.LOGGER.info("JEI register categories");
        IJeiHelpers jeiHelpers = registry.getJeiHelpers();
        registry.addRecipeCategories(new MultiblockInfoCategory(jeiHelpers));
        for (RecipeMap recipeMap : RecipeMap.RECIPE_MAP_REGISTRY.values()) {
            if (recipeMap == RecipeMap.EMPTY) continue;
            registry.addRecipeCategories(new RecipeMapCategory(jeiHelpers, recipeMap));
            registry.addRecipeCategories(new RecipeMapFuelCategory(jeiHelpers, recipeMap));
        }
    }

    @Override
    public void registerRecipeCatalysts(@Nonnull IRecipeCatalystRegistration registration) {
        if (LDLMod.isReiLoaded()) return;
        MultiblockInfoCategory.registerRecipeCatalysts(registration);
        RecipeMapCategory.registerRecipeCatalysts(registration);
        RecipeMapFuelCategory.registerRecipeCatalysts(registration);
    }

    @Override
    public void registerRecipes(@Nonnull IRecipeRegistration registration) {
        if (LDLMod.isReiLoaded()) return;
        Multiblocked.LOGGER.info("JEI register");
        MBDRecipeType.loadRecipes(Minecraft.getInstance().getConnection().getRecipeManager(), true);
        RecipeMapCategory.registerRecipes(registration);
        RecipeMapFuelCategory.registerRecipes(registration);
        MultiblockInfoCategory.registerRecipes(registration);
    }

    @Override
    public void registerIngredients(@Nonnull IModIngredientRegistration registry) {
        if (LDLMod.isReiLoaded()) return;
        Multiblocked.LOGGER.info("JEI register ingredients");
    }

    @Override
    @Nonnull
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(Multiblocked.MODID, "jei_plugin");
    }
}
