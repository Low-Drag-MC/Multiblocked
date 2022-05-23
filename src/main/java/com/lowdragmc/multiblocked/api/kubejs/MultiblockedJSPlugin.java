package com.lowdragmc.multiblocked.api.kubejs;

import com.lowdragmc.multiblocked.api.pattern.FactoryBlockPattern;
import com.lowdragmc.multiblocked.api.pattern.util.RelativeDirection;
import com.lowdragmc.multiblocked.api.recipe.ItemsIngredient;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import dev.latvian.kubejs.KubeJSPlugin;
import dev.latvian.kubejs.fluid.FluidStackJS;
import dev.latvian.kubejs.item.ingredient.IngredientJS;
import dev.latvian.kubejs.script.BindingsEvent;
import dev.latvian.kubejs.script.ScriptType;
import dev.latvian.mods.rhino.util.wrap.TypeWrappers;
import me.shedaniel.architectury.hooks.forge.FluidStackHooksForge;
import net.minecraftforge.fluids.FluidStack;

/**
 * @author KilaBash
 * @date 2022/5/23
 * @implNote MultiblockedJSPlugin
 */
public class MultiblockedJSPlugin extends KubeJSPlugin {
    @Override
    public void addBindings(BindingsEvent event) {
        event.add("MbdRecipeMap", RecipeMap.class);
        event.add("MbdRegistry", RegistryWrapper.class);
        event.add("MbdFactoryBlockPattern", FactoryBlockPattern.class);
        event.add("MbdRelativeDirection", RelativeDirection.class);
    }

    @Override
    public void addTypeWrappers(ScriptType type, TypeWrappers typeWrappers) {
        typeWrappers.register(ItemsIngredient.class, this::ItemsIngredientWrapper);
        typeWrappers.register(FluidStack.class, this::FluidStackWrapper);
    }

    private FluidStack FluidStackWrapper(Object o) {
        return FluidStackHooksForge.toForge(FluidStackJS.of(o).getFluidStack());
    }

    private ItemsIngredient ItemsIngredientWrapper(Object o) {
        IngredientJS ingredient = IngredientJS.of(o);
        return new ItemsIngredient(ingredient.createVanillaIngredient());
    }
}
