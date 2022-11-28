package com.lowdragmc.multiblocked.rei;

import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import com.lowdragmc.multiblocked.rei.multipage.MultiblockInfoDisplayCategory;
import com.lowdragmc.multiblocked.rei.recipeppage.RecipeMapDisplayCategory;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.forge.REIPluginClient;

/**
 * @author KilaBash
 * @date 2022/11/27
 * @implNote REIPlugin
 */
@REIPluginClient
public class REIClientPlugin implements me.shedaniel.rei.api.client.plugins.REIClientPlugin {
    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new MultiblockInfoDisplayCategory());
        for (RecipeMap recipeMap : RecipeMap.RECIPE_MAP_REGISTRY.values()) {
            if (recipeMap == RecipeMap.EMPTY) continue;
            registry.add(new RecipeMapDisplayCategory(recipeMap));
        }
        // workstations
        MultiblockInfoDisplayCategory.registerWorkStations(registry);
        RecipeMapDisplayCategory.registerWorkStations(registry);
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        MultiblockInfoDisplayCategory.registerDisplays(registry);
        RecipeMapDisplayCategory.registerDisplays(registry);
    }

}
