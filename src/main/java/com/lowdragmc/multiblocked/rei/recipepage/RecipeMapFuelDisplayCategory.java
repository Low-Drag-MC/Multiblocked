package com.lowdragmc.multiblocked.rei.recipepage;

import com.lowdragmc.lowdraglib.rei.IGui2Renderer;
import com.lowdragmc.lowdraglib.rei.ModularUIDisplayCategory;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import com.lowdragmc.multiblocked.rei.multipage.MultiblockInfoDisplayCategory;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.function.Function;

public class RecipeMapFuelDisplayCategory extends ModularUIDisplayCategory<FuelDisplay> {
    public static final Function<RecipeMap, CategoryIdentifier<FuelDisplay>> CATEGORIES = Util.memoize(recipeMap -> CategoryIdentifier.of(new ResourceLocation(Multiblocked.MODID, recipeMap.name + ".fuel")));

    private final RecipeMap recipeMap;
    private Renderer icon;

    public RecipeMapFuelDisplayCategory(RecipeMap recipeMap) {
        this.recipeMap = recipeMap;
    }

    @Override
    public CategoryIdentifier<? extends FuelDisplay> getCategoryIdentifier() {
        return CATEGORIES.apply(recipeMap);
    }

    @Override
    public int getDisplayHeight() {
        return 44 + 8;
    }

    @Override
    public int getDisplayWidth(FuelDisplay display) {
        return 176 + 8;
    }

    @Nonnull
    @Override
    public Component getTitle() {
        return new TranslatableComponent(recipeMap.getUnlocalizedName()).append(" ").append(new TranslatableComponent("multiblocked.gui.dialogs.recipe_map.fuel_recipe"));
    }


    @Nonnull
    @Override
    public Renderer getIcon() {
        return icon == null ? (icon = IGui2Renderer.toDrawable(recipeMap.fuelTexture.getSubTexture(0.0, 0.5, 1.0, 0.5))) : icon;
    }

    public static void registerDisplays(DisplayRegistry registry) {
        for (RecipeMap recipeMap : RecipeMap.RECIPE_MAP_REGISTRY.values()) {
            if (recipeMap.isFuelRecipeMap()) {
                recipeMap.fuelRecipes.stream().map(recipe -> new FuelDisplay(recipeMap, recipe)).forEach(registry::add);
            }
        }
    }

    public static void registerWorkStations(CategoryRegistry registry) {
        for (ControllerDefinition definition : MultiblockInfoDisplayCategory.REGISTER) {
            for (RecipeMap recipeMap : RecipeMap.RECIPE_MAP_REGISTRY.values()) {
                if (recipeMap == definition.getRecipeMap() && recipeMap.isFuelRecipeMap()) {
                    registry.addWorkstations(RecipeMapFuelDisplayCategory.CATEGORIES.apply(recipeMap), EntryStacks.of(definition.getStackForm()));
                }
            }
        }
    }

}
