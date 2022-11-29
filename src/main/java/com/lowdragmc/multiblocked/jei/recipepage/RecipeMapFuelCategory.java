package com.lowdragmc.multiblocked.jei.recipepage;

import com.lowdragmc.lowdraglib.jei.IGui2IDrawable;
import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.gui.recipe.FuelWidget;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import com.lowdragmc.multiblocked.jei.multipage.MultiblockInfoCategory;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RecipeMapFuelCategory extends ModularUIRecipeCategory<FuelWrapper> {
    public static final Function<RecipeMap, RecipeType<FuelWrapper>> TYPES = Util.memoize(recipeMap -> new RecipeType<>(new ResourceLocation(Multiblocked.MODID, recipeMap.name + ".fuel"), FuelWrapper.class));

    private final RecipeMap recipeMap;
    private final IDrawable background;
    private IDrawable icon;

    public RecipeMapFuelCategory(IJeiHelpers helpers, RecipeMap recipeMap) {
        IGuiHelper guiHelper = helpers.getGuiHelper();
        this.background = guiHelper.createBlankDrawable(176, 44);
        this.recipeMap = recipeMap;
    }

    @Override
    @Nonnull
    public RecipeType<FuelWrapper> getRecipeType() {
        return TYPES.apply(recipeMap);
    }

    @Nonnull
    @Override
    public ResourceLocation getUid() {
        return getRecipeType().getUid();
    }

    @Nonnull
    @Override
    public Class<? extends FuelWrapper> getRecipeClass() {
        return getRecipeType().getRecipeClass();
    }

    @Nonnull
    @Override
    public Component getTitle() {
        return new TranslatableComponent(recipeMap.getUnlocalizedName()).append(" ").append(new TranslatableComponent("multiblocked.gui.dialogs.recipe_map.fuel_recipe"));
    }

    @Nonnull
    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Nonnull
    @Override
    public IDrawable getIcon() {
        return icon == null ? (icon = IGui2IDrawable.toDrawable(recipeMap.fuelTexture.getSubTexture(0.0, 0.5, 1.0, 0.5), 18, 18)) : icon;
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        for (RecipeMap recipeMap : RecipeMap.RECIPE_MAP_REGISTRY.values()) {
            if (recipeMap.isFuelRecipeMap()) {
                registration.addRecipes(RecipeMapFuelCategory.TYPES.apply(recipeMap), recipeMap.recipes.values()
                        .stream()
                        .map(recipe -> new FuelWidget(recipeMap, recipe))
                        .map(FuelWrapper::new)
                        .collect(Collectors.toList()));
            }
        }
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (ControllerDefinition definition : MultiblockInfoCategory.REGISTER) {
            for (RecipeMap recipeMap : RecipeMap.RECIPE_MAP_REGISTRY.values()) {
                if (recipeMap == definition.getRecipeMap() && recipeMap.isFuelRecipeMap()) {
                    registration.addRecipeCatalyst(definition.getStackForm(), RecipeMapFuelCategory.TYPES.apply(recipeMap));
                }
            }
        }
    }

}
