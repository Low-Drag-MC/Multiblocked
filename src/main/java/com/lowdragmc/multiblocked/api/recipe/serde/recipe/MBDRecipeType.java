package com.lowdragmc.multiblocked.api.recipe.serde.recipe;

import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.kubejs.events.RecipeConverterRegisterEvent;
import com.lowdragmc.multiblocked.api.recipe.RecipeConverter;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import com.lowdragmc.multiblocked.core.mixins.RecipeManagerMixin;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber
public class MBDRecipeType {
    public static final RecipeType<MultiBlockRecipe> MULTIBLOCK_RECIPE_TYPE = new MultiBlockRecipe.MultiBlockRecipeType();
    public static final RecipeSerializer<MultiBlockRecipe> MULTIBLOCK_RECIPE_SERIALIZER = new MultiBlockRecipe.Serializer();
    public static final Map<String, Set<String>> addedRecipes = new HashMap<>();

    public static void loadRecipes(RecipeManager recipeManager) {
        Map<ResourceLocation, Recipe<Container>> recipes = ((RecipeManagerMixin) recipeManager).mbd_byType(MULTIBLOCK_RECIPE_TYPE);
        for (Recipe<Container> recipe : recipes.values()) {
            if (recipe instanceof MultiBlockRecipe multiBlockRecipe) {
                com.lowdragmc.multiblocked.api.recipe.Recipe mbdRecipe = multiBlockRecipe.getMBDRecipe();
                addedRecipes.computeIfAbsent(multiBlockRecipe.getMachineType(), s -> new HashSet<>()).add(mbdRecipe.uid);
                var recipeMap = RecipeMap.RECIPE_MAP_REGISTRY.computeIfAbsent(multiBlockRecipe.getMachineType(), RecipeMap::new);
                if (multiBlockRecipe.isFuel()) {
                    recipeMap.addFuelRecipe(mbdRecipe);
                } else {
                    recipeMap.addRecipe(mbdRecipe);
                }
            }
        }
        if (Multiblocked.isKubeJSLoaded()) {
            RecipeConverter.converters.clear();
            new RecipeConverterRegisterEvent().post(ScriptType.getCurrent(ScriptType.SERVER), RecipeConverterRegisterEvent.ID);
            for (RecipeConverter converter : RecipeConverter.converters) {
                converter.apply();
            }
        }
    }

    public static void unloadRecipes() {
        addedRecipes.forEach((machineType, ids) -> {
            RecipeMap recipeMap = RecipeMap.RECIPE_MAP_REGISTRY.get(machineType);
            for (String id : ids) {
                recipeMap.recipes.remove(id);
            }
        });
        addedRecipes.clear();
    }

    @SubscribeEvent
    public static void onServerLoadRecipes(ServerStartedEvent event) {
        MBDRecipeReloadListener.INSTANCE.server = event.getServer();
        loadRecipes(MBDRecipeReloadListener.INSTANCE.server.getRecipeManager());
    }

    @SubscribeEvent
    public static void onServerUnloadRecipes(ServerStoppedEvent event) {
        unloadRecipes();
    }
}
