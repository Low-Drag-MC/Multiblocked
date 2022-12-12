package com.lowdragmc.multiblocked.rei.recipepage;

import com.lowdragmc.lowdraglib.rei.IGui2Renderer;
import com.lowdragmc.lowdraglib.rei.ModularUIDisplayCategory;
import com.lowdragmc.lowdraglib.utils.Size;
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

public class RecipeMapDisplayCategory extends ModularUIDisplayCategory<RecipeDisplay> {
    public static final Function<RecipeMap, CategoryIdentifier<RecipeDisplay>> CATEGORIES = Util.memoize(recipeMap -> CategoryIdentifier.of(new ResourceLocation(Multiblocked.MODID, recipeMap.name)));

    private final RecipeMap recipeMap;
    private Renderer icon;
    private Size size;

    public RecipeMapDisplayCategory(RecipeMap recipeMap) {
        this.recipeMap = recipeMap;
    }

    @Override
    public CategoryIdentifier<? extends RecipeDisplay> getCategoryIdentifier() {
        return CATEGORIES.apply(recipeMap);
    }

    public Size getSize() {
        if (size == null) {
            var ui = recipeMap.createLDLibUI(null);
            if (ui == null) {
                this.size = new Size(176 + 8, 84 + 8);
            } else {
                this.size = new Size(ui.getSize().width + 8, ui.getSize().height + 8);
            }
        }
        return size;
    }

    @Override
    public int getDisplayHeight() {
        return getSize().height;
    }

    @Override
    public int getDisplayWidth(RecipeDisplay display) {
        return getSize().width;
    }

    @Nonnull
    @Override
    public Component getTitle() {
        return new TranslatableComponent(recipeMap.getUnlocalizedName());
    }


    @Nonnull
    @Override
    public Renderer getIcon() {
        return icon == null ? (icon = IGui2Renderer.toDrawable(recipeMap.categoryTexture)) : icon;
    }

    public static void registerDisplays(DisplayRegistry registry) {
        for (RecipeMap recipeMap : RecipeMap.RECIPE_MAP_REGISTRY.values()) {
            if (recipeMap == RecipeMap.EMPTY || recipeMap.recipes.isEmpty()) continue;
            recipeMap.recipes.values()
                    .stream()
                    .map(recipe -> new RecipeDisplay(recipeMap, recipe))
                    .forEach(registry::add);
        }
    }

    public static void registerWorkStations(CategoryRegistry registry) {
        for (ControllerDefinition definition : MultiblockInfoDisplayCategory.REGISTER) {
            for (RecipeMap recipeMap : RecipeMap.RECIPE_MAP_REGISTRY.values()) {
                if (recipeMap == definition.getRecipeMap()) {
                    registry.addWorkstations(RecipeMapDisplayCategory.CATEGORIES.apply(recipeMap), EntryStacks.of(definition.getStackForm()));
                }
            }
        }
    }

}
