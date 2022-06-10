package com.lowdragmc.multiblocked.api.kubejs;

import com.lowdragmc.multiblocked.api.kubejs.events.RecipeEvent;
import com.lowdragmc.multiblocked.api.kubejs.recipes.RecipeMapJS;
import com.lowdragmc.multiblocked.api.pattern.FactoryBlockPattern;
import com.lowdragmc.multiblocked.api.pattern.util.RelativeDirection;
import com.lowdragmc.multiblocked.api.recipe.ItemsIngredient;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.fluid.FluidStackJS;
import dev.latvian.mods.kubejs.item.ingredient.IngredientJS;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.rhino.util.wrap.TypeWrappers;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

/**
 * @author KilaBash
 * @date 2022/5/23
 * @implNote MultiblockedJSPlugin
 */
@Mod.EventBusSubscriber
public class MultiblockedJSPlugin extends KubeJSPlugin {
    public static final Set<String> ADDED_RECIPES = new HashSet<>();

    @Override
    public void addBindings(BindingsEvent event) {
        event.add("MbdFactoryBlockPattern", FactoryBlockPattern.class);
        event.add("MbdRelativeDirection", RelativeDirection.class);
    }

    //Fire MBD's own event when things are good
    @SubscribeEvent
    public static void onServerLoad(ServerStartedEvent event) {
        ADDED_RECIPES.clear();
        var recipeEvent = new RecipeEvent();
        recipeEvent.post(ScriptType.SERVER, RecipeEvent.ID);
        recipeEvent.recipeMaps.forEach(RecipeMapJS::build);
    }

    //Remove added recipes when server stopped so we won't get dupes
    @SubscribeEvent
    public static void onServerUnload(ServerStoppedEvent event) {
        RecipeMap.RECIPE_MAP_REGISTRY.values().forEach(map -> {
            ADDED_RECIPES.forEach(uid -> map.recipes.remove(uid));
        });
    }
}
