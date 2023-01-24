package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.recipe.RecipeConverter;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.world.item.crafting.Recipe;

import java.util.function.BiConsumer;

public class RecipeConverterRegisterEvent extends EventJS {

    public static final String ID = "mbd.recipe_converter_register";

    public void register(String sourceType, String recipeMap, BiConsumer<Recipe<?>, RecipeMap> converter) {
        RecipeConverter.converters.add(new RecipeConverter(sourceType, recipeMap, converter));
    }

}
