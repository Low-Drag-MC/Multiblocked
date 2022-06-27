package com.lowdragmc.multiblocked.api.kubejs;

import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.multiblocked.api.pattern.FactoryBlockPattern;
import com.lowdragmc.multiblocked.api.pattern.MultiblockShapeInfo;
import com.lowdragmc.multiblocked.api.pattern.Predicates;
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
        event.add("MbdPredicates", Predicates.class);
        event.add("MbdShapeInfo", MultiblockShapeInfo.class);
        event.add("MbdBlockInfo", BlockInfo.class);
    }

    @Override
    public void addTypeWrappers(ScriptType type, TypeWrappers typeWrappers) {
        typeWrappers.register(ItemsIngredient.class, MultiblockedJSPlugin::ItemsIngredientWrapper);
        if (typeWrappers.getWrapperFactory(FluidStack.class, null) == null) {
            typeWrappers.register(FluidStack.class, MultiblockedJSPlugin::FluidStackWrapper);
        }
    }

    public static FluidStack FluidStackWrapper(Object o) {
        return FluidStackHooksForge.toForge(FluidStackJS.of(o).getFluidStack());
    }

    public static ItemsIngredient ItemsIngredientWrapper(Object o) {
        IngredientJS ingredient = IngredientJS.of(o);
        return new ItemsIngredient(ingredient.createVanillaIngredient());
    }
}
