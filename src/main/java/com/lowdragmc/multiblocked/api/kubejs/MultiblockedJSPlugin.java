package com.lowdragmc.multiblocked.api.kubejs;

import com.lowdragmc.lowdraglib.client.particle.impl.ShaderBeamParticle;
import com.lowdragmc.lowdraglib.client.particle.impl.ShaderParticle;
import com.lowdragmc.lowdraglib.client.particle.impl.ShaderTrailParticle;
import com.lowdragmc.lowdraglib.client.particle.impl.TextureBeamParticle;
import com.lowdragmc.lowdraglib.client.particle.impl.TextureParticle;
import com.lowdragmc.lowdraglib.client.particle.impl.TextureTrailParticle;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.pattern.FactoryBlockPattern;
import com.lowdragmc.multiblocked.api.pattern.util.RelativeDirection;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import com.lowdragmc.multiblocked.api.recipe.serde.recipe.MultiBlockRecipe;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.fluid.FluidStackJS;
import dev.latvian.mods.kubejs.recipe.RegisterRecipeHandlersEvent;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.rhino.util.wrap.TypeWrappers;
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
        if (Multiblocked.isClient()) {
            event.add("TextureParticle", TextureParticle.class);
            event.add("ShaderParticle", ShaderParticle.class);

            event.add("TextureTrailParticle", TextureTrailParticle.class);
            event.add("ShaderTrailParticle", ShaderTrailParticle.class);

            event.add("TextureBeamParticle", TextureBeamParticle.class);
            event.add("ShaderBeamParticle", ShaderBeamParticle.class);
        }
    }

    @Override
    public void addTypeWrappers(ScriptType type, TypeWrappers typeWrappers) {
        typeWrappers.register(FluidStack.class, MultiblockedJSPlugin::FluidStackWrapper);
    }

    public static FluidStack FluidStackWrapper(Object o) {
        return FluidStackHooksForge.toForge(FluidStackJS.of(o).getFluidStack());
    }

    @Override
    public void addRecipes(RegisterRecipeHandlersEvent event) {
        event.register(MultiBlockRecipe.MultiBlockRecipeType.TYPE_ID, MultiblockRecipeJS::new);
    }
}
