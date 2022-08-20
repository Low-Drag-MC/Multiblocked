package com.lowdragmc.multiblocked.jei.recipeppage;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.jei.IGui2IDrawable;
import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import com.lowdragmc.multiblocked.api.recipe.Content;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import com.lowdragmc.multiblocked.common.capability.ChemicalMekanismCapability;
import com.lowdragmc.multiblocked.common.capability.EntityMultiblockCapability;
import com.lowdragmc.multiblocked.common.capability.FluidMultiblockCapability;
import com.lowdragmc.multiblocked.common.capability.ItemMultiblockCapability;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.SlurryStack;
import mekanism.client.jei.MekanismJEI;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RecipeMapCategory extends ModularUIRecipeCategory<RecipeWrapper> {
    private final RecipeMap recipeMap;
    private final IDrawable background;
    private IDrawable icon;

    public RecipeMapCategory(IJeiHelpers helpers, RecipeMap recipeMap) {
        IGuiHelper guiHelper = helpers.getGuiHelper();
        this.background = guiHelper.createBlankDrawable(176, 84);
        this.recipeMap = recipeMap;
    }

    @SuppressWarnings("removal")
    @Override
    public @NotNull ResourceLocation getUid() {
        return new ResourceLocation(Multiblocked.MODID, recipeMap.name);
    }

    @SuppressWarnings("removal")
    @Override
    public @NotNull Class<? extends RecipeWrapper> getRecipeClass() {
        return RecipeWrapper.class;
    }

    @Override
    public @NotNull RecipeType<RecipeWrapper> getRecipeType() {
        return RecipeType.create(Multiblocked.MODID, recipeMap.name, RecipeWrapper.class);
    }

    @Nonnull
    @Override
    public Component getTitle() {
        return new TranslatableComponent(recipeMap.getUnlocalizedName());
    }

    @Nonnull
    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Nonnull
    @Override
    public IDrawable getIcon() {
        return icon == null ? (icon = IGui2IDrawable.toDrawable(recipeMap.categoryTexture, 18, 18)) : icon;
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull RecipeWrapper wrapper, @NotNull IFocusGroup focuses) {
        Recipe recipe = wrapper.recipe;
        List<Widget> allWidgets = wrapper.modularUI.getFlatWidgetCollection();
        List<? extends ContentWidget<?>> inputContents =
                wrapper.getWidget().inputs.widgets
                        .stream()
                        .filter(widget -> widget instanceof ContentWidget<?>)
                        .map(widget -> (ContentWidget<?>) widget)
                        .toList();
        List<? extends ContentWidget<?>> outputContents =
                wrapper.getWidget().outputs.widgets
                        .stream()
                        .filter(widget -> widget instanceof ContentWidget<?>)
                        .map(widget -> (ContentWidget<?>) widget)
                        .toList();

        if (recipe.inputs.containsKey(ItemMultiblockCapability.CAP)) {
            List<ContentWidget<?>> contentWidgets = getContentWidgets(true, inputContents, recipe, ItemMultiblockCapability.CAP);
            for (ContentWidget<?> widget : contentWidgets) {
                builder.addSlot(RecipeIngredientRole.INPUT, allWidgets.indexOf(widget), -1)
                        .addIngredients((Ingredient) widget.getContent());
            }
        }
        if (recipe.outputs.containsKey(ItemMultiblockCapability.CAP)) {
            List<ContentWidget<?>> contentWidgets = getContentWidgets(false, outputContents, recipe, ItemMultiblockCapability.CAP);
            for (ContentWidget<?> widget : contentWidgets) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, allWidgets.indexOf(widget), -1)
                        .addIngredients((Ingredient) widget.getContent());
            }
        }

        if (recipe.inputs.containsKey(EntityMultiblockCapability.CAP)) {
            List<ContentWidget<?>> contentWidgets = getContentWidgets(true, inputContents, recipe, EntityMultiblockCapability.CAP);
            for (ContentWidget<?> widget : contentWidgets) {
                IRecipeSlotBuilder slotBuilder = builder.addSlot(RecipeIngredientRole.INPUT, allWidgets.indexOf(widget), -1);
                SpawnEggItem eggItem = ForgeSpawnEggItem.fromEntityType((EntityType<?>) widget.getContent());
                if (eggItem != null) {
                    slotBuilder.addItemStack(new ItemStack(eggItem));
                }
            }
        }
        if (recipe.outputs.containsKey(EntityMultiblockCapability.CAP)) {
            List<ContentWidget<?>> contentWidgets = getContentWidgets(false, outputContents, recipe, EntityMultiblockCapability.CAP);
            for (ContentWidget<?> widget : contentWidgets) {
                IRecipeSlotBuilder slotBuilder = builder.addSlot(RecipeIngredientRole.OUTPUT, allWidgets.indexOf(widget), -1);
                SpawnEggItem eggItem = ForgeSpawnEggItem.fromEntityType((EntityType<?>) widget.getContent());
                if (eggItem != null) {
                    slotBuilder.addItemStack(new ItemStack(eggItem));
                }
            }
        }

        checkCommonIngredients(allWidgets, inputContents, outputContents, recipe, FluidMultiblockCapability.CAP, ForgeTypes.FLUID_STACK, FluidStack.class, builder);

        if (Multiblocked.isMekLoaded()) {
            checkCommonIngredients(allWidgets, inputContents, outputContents, recipe, ChemicalMekanismCapability.CAP_GAS, MekanismJEI.TYPE_GAS, GasStack.class, builder);
            checkCommonIngredients(allWidgets, inputContents, outputContents, recipe, ChemicalMekanismCapability.CAP_INFUSE, MekanismJEI.TYPE_INFUSION, InfusionStack.class, builder);
            checkCommonIngredients(allWidgets, inputContents, outputContents, recipe, ChemicalMekanismCapability.CAP_PIGMENT, MekanismJEI.TYPE_PIGMENT, PigmentStack.class, builder);
            checkCommonIngredients(allWidgets, inputContents, outputContents, recipe, ChemicalMekanismCapability.CAP_SLURRY, MekanismJEI.TYPE_SLURRY, SlurryStack.class, builder);
        }

    }

    private <T> void checkCommonIngredients(
            List<Widget> allWidgets,
            List<? extends ContentWidget<?>> inputContents,
            List<? extends ContentWidget<?>> outputContents,
            Recipe recipe,
            MultiblockCapability<T> cap,
            IIngredientType<T> type,
            Class<T> clazz,
            IRecipeLayoutBuilder builder
    ) {
        if (recipe.inputs.containsKey(cap)) {
            List<ContentWidget<?>> contentWidgets = getContentWidgets(true, inputContents, recipe, cap);
            for (ContentWidget<?> widget : contentWidgets) {
                builder.addSlot(RecipeIngredientRole.INPUT, allWidgets.indexOf(widget), -1)
                        .addIngredient(type, clazz.cast(widget.getContent()));
            }
        }
        if (recipe.outputs.containsKey(cap)) {
            List<ContentWidget<?>> contentWidgets = getContentWidgets(false, outputContents, recipe, cap);
            for (ContentWidget<?> widget : contentWidgets) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, allWidgets.indexOf(widget), -1)
                        .addIngredient(type, clazz.cast(widget.getContent()));
            }
        }
    }

    private <T> List<ContentWidget<?>> getContentWidgets(boolean input, List<? extends ContentWidget<?>> widgets, Recipe recipe, MultiblockCapability<T> cap) {
        var target = input ? recipe.inputs : recipe.outputs;
        List<Object> contents = Objects.requireNonNull(target.get(cap)).stream().map(Content::getContent).toList();
        return widgets.stream()
                .filter(widget -> contents.contains(widget.getContent()))
                .collect(Collectors.toList());
    }

}
