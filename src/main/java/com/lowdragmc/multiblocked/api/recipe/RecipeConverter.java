package com.lowdragmc.multiblocked.api.recipe;

import com.lowdragmc.multiblocked.api.recipe.serde.recipe.MBDRecipeReloadListener;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * A tool for converting recipes from other sources into a particular multiblock recipe.
 */
public class RecipeConverter {

    public static final List<RecipeConverter> converters = new ArrayList<>();
    private final ResourceLocation sourceType;
    private final RecipeMap target;
    private final BiConsumer<Recipe<?>, RecipeMap> converter;

    public RecipeConverter(String sourceType, String target, BiConsumer<Recipe<?>, RecipeMap> converter) {
        this.sourceType = new ResourceLocation(sourceType);
        this.target = RecipeMap.RECIPE_MAP_REGISTRY.get(target);
        if (this.target == null) {
            throw new IllegalArgumentException("No recipe map found for " + target);
        }
        this.converter = converter;
    }

    @SuppressWarnings("unchecked")
    public <C extends Container, T extends Recipe<C>> void apply() {
        RecipeManager manager = MBDRecipeReloadListener.INSTANCE.server.getRecipeManager();
        RecipeType<T> recipeType = (RecipeType<T>) Registry.RECIPE_TYPE.get(sourceType);
        if (recipeType == null) {
            throw new IllegalArgumentException("No recipe type found for " + sourceType);
        }
        for (Recipe<?> recipe : manager.getAllRecipesFor(recipeType)) {
            converter.accept(recipe, target);
        }
    }

}
