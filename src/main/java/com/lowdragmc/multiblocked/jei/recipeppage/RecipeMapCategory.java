package com.lowdragmc.multiblocked.jei.recipeppage;

import com.lowdragmc.lowdraglib.jei.IGui2IDrawable;
import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.recipe.Content;
import com.lowdragmc.multiblocked.api.recipe.ItemsIngredient;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import com.lowdragmc.multiblocked.common.capability.ChemicalMekanismCapability;
import com.lowdragmc.multiblocked.common.capability.FluidMultiblockCapability;
import com.lowdragmc.multiblocked.common.capability.ItemMultiblockCapability;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.SlurryStack;
import mekanism.client.jei.MekanismJEI;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.IIngredients;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.Arrays;
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

    @Nonnull
    @Override
    public ResourceLocation getUid() {
        return new ResourceLocation(Multiblocked.MODID, recipeMap.name);
    }

    @Nonnull
    @Override
    public Class<? extends RecipeWrapper> getRecipeClass() {
        return RecipeWrapper.class;
    }

    @Nonnull
    @Override
    public String getTitle() {
        return I18n.get(recipeMap.getUnlocalizedName());
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
    public void setIngredients(@Nonnull RecipeWrapper wrapper, @Nonnull IIngredients ingredients) {
        Recipe recipe = wrapper.recipe;
        if (recipe.inputs.containsKey(ItemMultiblockCapability.CAP)) {
            ingredients.setInputs(VanillaTypes.ITEM, recipe.inputs.get(ItemMultiblockCapability.CAP).stream()
                    .map(Content::getContent)
                    .map(ItemsIngredient.class::cast)
                    .flatMap(r-> Arrays.stream(r.ingredient.getItems()))
                    .collect(Collectors.toList()));
        }
        if (recipe.outputs.containsKey(ItemMultiblockCapability.CAP)) {
            ingredients.setOutputs(VanillaTypes.ITEM, recipe.outputs.get(ItemMultiblockCapability.CAP).stream()
                    .map(Content::getContent)
                    .map(ItemsIngredient.class::cast)
                    .flatMap(r -> Arrays.stream(r.ingredient.getItems()))
                    .collect(Collectors.toList()));
        }

        checkCommonIngredients(recipe, FluidMultiblockCapability.CAP, ingredients, VanillaTypes.FLUID, FluidStack.class);

        if (Multiblocked.isMekLoaded()) {
            checkCommonIngredients(recipe, ChemicalMekanismCapability.CAP_GAS, ingredients, MekanismJEI.TYPE_GAS, GasStack.class);
            checkCommonIngredients(recipe, ChemicalMekanismCapability.CAP_INFUSE, ingredients, MekanismJEI.TYPE_INFUSION, InfusionStack.class);
            checkCommonIngredients(recipe, ChemicalMekanismCapability.CAP_PIGMENT, ingredients, MekanismJEI.TYPE_PIGMENT, PigmentStack.class);
            checkCommonIngredients(recipe, ChemicalMekanismCapability.CAP_SLURRY, ingredients, MekanismJEI.TYPE_SLURRY, SlurryStack.class);
        }

    }

    private <T> void checkCommonIngredients(Recipe recipe, MultiblockCapability<T> CAP, IIngredients ingredients, IIngredientType<T> type, Class<T> clazz) {
        if (recipe.inputs.containsKey(CAP)) {
            ingredients.setInputs(type, recipe.inputs.get(CAP).stream()
                    .map(Content::getContent)
                    .map(clazz::cast)
                    .collect(Collectors.toList()));
        }
        if (recipe.outputs.containsKey(CAP)) {
            ingredients.setOutputs(type, recipe.outputs.get(CAP).stream()
                    .map(Content::getContent)
                    .map(clazz::cast)
                    .collect(Collectors.toList()));
        }
    }

}
