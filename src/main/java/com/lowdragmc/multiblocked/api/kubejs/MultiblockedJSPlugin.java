package com.lowdragmc.multiblocked.api.kubejs;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.lowdragmc.multiblocked.api.capability.ICapabilityProxyHolder;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.gui.controller.IOPageWidget;
import com.lowdragmc.multiblocked.api.gui.controller.PageWidget;
import com.lowdragmc.multiblocked.api.gui.controller.RecipePage;
import com.lowdragmc.multiblocked.api.gui.recipe.ProgressWidget;
import com.lowdragmc.multiblocked.api.gui.recipe.RecipeWidget;
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
import net.minecraft.util.math.shapes.VoxelShapes;
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
        event.add("MbdIO", IO.class);
        event.add("Shapes", VoxelShapes.class);
        event.add("ICapabilityProxyHolder", ICapabilityProxyHolder.class);

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
        event.add("GuiSize", Size.class);
        event.add("GuiPos", Position.class);
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
