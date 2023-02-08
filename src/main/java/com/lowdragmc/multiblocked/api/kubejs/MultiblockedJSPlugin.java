package com.lowdragmc.multiblocked.api.kubejs;

import com.lowdragmc.lowdraglib.client.particle.impl.ShaderBeamParticle;
import com.lowdragmc.lowdraglib.client.particle.impl.ShaderParticle;
import com.lowdragmc.lowdraglib.client.particle.impl.ShaderTrailParticle;
import com.lowdragmc.lowdraglib.client.particle.impl.TextureBeamParticle;
import com.lowdragmc.lowdraglib.client.particle.impl.TextureParticle;
import com.lowdragmc.lowdraglib.client.particle.impl.TextureTrailParticle;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.*;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.ICapabilityProxyHolder;
import com.lowdragmc.multiblocked.api.gui.controller.IOPageWidget;
import com.lowdragmc.multiblocked.api.gui.controller.PageWidget;
import com.lowdragmc.multiblocked.api.gui.controller.RecipePage;
import com.lowdragmc.multiblocked.api.gui.recipe.RecipeWidget;
import com.lowdragmc.multiblocked.api.pattern.FactoryBlockPattern;
import com.lowdragmc.multiblocked.api.pattern.MultiblockShapeInfo;
import com.lowdragmc.multiblocked.api.pattern.Predicates;
import com.lowdragmc.multiblocked.api.recipe.ContentModifier;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import com.lowdragmc.multiblocked.api.recipe.ingredient.EntityIngredient;
import com.lowdragmc.multiblocked.api.recipe.serde.recipe.MultiBlockRecipe;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.fluid.FluidStackJS;
import dev.latvian.mods.kubejs.recipe.RegisterRecipeHandlersEvent;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;
import dev.latvian.mods.rhino.util.wrap.TypeWrappers;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.fluids.FluidStack;

/**
 * @author KilaBash
 * @date 2022/5/23
 * @implNote MultiblockedJSPlugin
 */
public class MultiblockedJSPlugin extends KubeJSPlugin {

    @Override
    public void addClasses(ScriptType type, ClassFilter filter) {
        filter.allow("com.lowdragmc");
    }

    @Override
    public void addBindings(BindingsEvent event) {
        event.add("MbdRecipeMap", RecipeMap.class);
        event.add("MbdRegistry", RegistryWrapper.class);
        event.add("MbdFactoryBlockPattern", FactoryBlockPattern.class);
        event.add("MbdPredicates", Predicates.class);
        event.add("MbdShapeInfo", MultiblockShapeInfo.class);
        event.add("MbdBlockInfo", BlockInfo.class);
        event.add("Shapes", Shapes.class);
        event.add("MbdContentModifier", ContentModifier.class);
        event.add("MbdEntityIngredient", EntityIngredient.class);
        event.add("ICapabilityProxyHolder", ICapabilityProxyHolder.class);
        if (Multiblocked.isClient()) {
            event.add("TextureParticle", TextureParticle.class);
            event.add("ShaderParticle", ShaderParticle.class);

            event.add("TextureTrailParticle", TextureTrailParticle.class);
            event.add("ShaderTrailParticle", ShaderTrailParticle.class);

            event.add("TextureBeamParticle", TextureBeamParticle.class);
            event.add("ShaderBeamParticle", ShaderBeamParticle.class);
        }
        // LDLib Widget
        event.add("ModularUI", ModularUI.class);
        event.add("BlockSelectorWidget", BlockSelectorWidget.class);
        event.add("ButtonWidget", ButtonWidget.class);
        event.add("DialogWidget", DialogWidget.class);
        event.add("DraggableScrollableWidgetGroup", DraggableScrollableWidgetGroup.class);
        event.add("DraggableWidgetGroup", DraggableWidgetGroup.class);
        event.add("ImageWidget", ImageWidget.class);
        event.add("LabelWidget", LabelWidget.class);
        event.add("PhantomFluidWidget", PhantomFluidWidget.class);
        event.add("PhantomSlotWidget", PhantomSlotWidget.class);
        event.add("SceneWidget", SceneWidget.class);
        event.add("SelectableWidgetGroup", SelectableWidgetGroup.class);
        event.add("SlotWidget", SlotWidget.class);
        event.add("SwitchWidget", SwitchWidget.class);
        event.add("TabButton", TabButton.class);
        event.add("TabContainer", TabContainer.class);
        event.add("TankWidget", TankWidget.class);
        event.add("TextBoxWidget", TextBoxWidget.class);
        event.add("TextFieldWidget", TextFieldWidget.class);
        event.add("TreeListWidget", TreeListWidget.class);
        event.add("WidgetGroup", WidgetGroup.class);
        event.add("ColorBorderTexture", ColorBorderTexture.class);
        event.add("ColorRectTexture", ColorRectTexture.class);
        event.add("GuiTextureGroup", GuiTextureGroup.class);
        event.add("ItemStackTexture", ItemStackTexture.class);
        event.add("ResourceBorderTexture", ResourceBorderTexture.class);
        event.add("ResourceTexture", ResourceTexture.class);
        event.add("ShaderTexture", ShaderTexture.class);
        event.add("TextTexture", TextTexture.class);
        //mbd widget
        event.add("IOPageWidget", IOPageWidget.class);
        event.add("PageWidget", PageWidget.class);
        event.add("RecipePage", RecipePage.class);
        event.add("ProgressWidget", ProgressWidget.class);
        event.add("RecipeWidget", RecipeWidget.class);
        // math
        event.add("Vector3", Vector3.class);
        event.add("GuiSize", Size.class);
        event.add("GuiPos", Position.class);
        //utils
        event.add("ArrayUtils", ArrayUtils.class);
        event.add("ContentModifier", ContentModifier.class);
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
