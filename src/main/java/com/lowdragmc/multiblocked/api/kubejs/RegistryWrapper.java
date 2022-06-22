package com.lowdragmc.multiblocked.api.kubejs;

import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import com.lowdragmc.multiblocked.api.registry.MbdCapabilities;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import net.minecraft.resources.ResourceLocation;

/**
 * @author KilaBash
 * @date 2022/5/23
 * @implNote RegistryWrapper
 */
public class RegistryWrapper {

    public static ComponentDefinition getDefinition(String name) {
        return MbdComponents.DEFINITION_REGISTRY.get(new ResourceLocation(name));
    }

    public static MultiblockCapability<?> getCapability(String name) {
        return MbdCapabilities.get(name);
    }

    public static RecipeMap getRecipeMap(String name) {
        return RecipeMap.RECIPE_MAP_REGISTRY.get(name);
    }

}
