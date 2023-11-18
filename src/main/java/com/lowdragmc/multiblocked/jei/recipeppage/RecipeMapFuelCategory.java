package com.lowdragmc.multiblocked.jei.recipeppage;

import com.lowdragmc.lowdraglib.jei.IGui2IDrawable;
import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.recipe.*;
import com.lowdragmc.multiblocked.common.capability.ChemicalMekanismCapability;
import com.lowdragmc.multiblocked.common.capability.EntityMultiblockCapability;
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
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.stream.Collectors;

public class RecipeMapFuelCategory extends ModularUIRecipeCategory<FuelWrapper> {
    private final RecipeMap recipeMap;
    private final IDrawable background;
    private IDrawable icon;

    public RecipeMapFuelCategory(IJeiHelpers helpers, RecipeMap recipeMap) {
        IGuiHelper guiHelper = helpers.getGuiHelper();
        this.background = guiHelper.createBlankDrawable(176, 44);
        this.recipeMap = recipeMap;
    }

    @Nonnull
    @Override
    public ResourceLocation getUid() {
        return new ResourceLocation(Multiblocked.MODID, recipeMap.name + ".fuel");
    }

    @Nonnull
    @Override
    public Class<? extends FuelWrapper> getRecipeClass() {
        return FuelWrapper.class;
    }

    @Nonnull
    @Override
    public String getTitle() {
        return I18n.get(recipeMap.getUnlocalizedName()) + " " + I18n.get("multiblocked.gui.dialogs.recipe_map.fuel_recipe");
    }

    @Nonnull
    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Nonnull
    @Override
    public IDrawable getIcon() {
        return icon == null ? (icon = IGui2IDrawable.toDrawable(recipeMap.fuelTexture.getSubTexture(0.0, 0.5, 1.0, 0.5), 18, 18)) : icon;
    }

    @Override
    public void setIngredients(@Nonnull FuelWrapper wrapper, @Nonnull IIngredients ingredients) {
        Recipe recipe = wrapper.recipe;
        if (recipe.inputs.containsKey(ItemMultiblockCapability.CAP)) {
            ingredients.setInputs(VanillaTypes.ITEM, recipe.inputs.get(ItemMultiblockCapability.CAP).stream()
                    .map(Content::getContent)
                    .map(ItemsIngredient.class::cast)
                    .flatMap(r-> Arrays.stream(r.getIngredient().getItems()))
                    .collect(Collectors.toList()));
        }
        if (recipe.outputs.containsKey(ItemMultiblockCapability.CAP)) {
            ingredients.setOutputs(VanillaTypes.ITEM, recipe.outputs.get(ItemMultiblockCapability.CAP).stream()
                    .map(Content::getContent)
                    .map(ItemsIngredient.class::cast)
                    .flatMap(r -> Arrays.stream(r.getIngredient().getItems()))
                    .collect(Collectors.toList()));
        }

        if (recipe.inputs.containsKey(EntityMultiblockCapability.CAP)) {
            ingredients.setInputIngredients(recipe.inputs.get(EntityMultiblockCapability.CAP).stream()
                    .map(Content::getContent)
                    .map(EntityIngredient.class::cast)
                    .map(content -> {
                        if (content.isEntityItem()) {
                            return content.getEntityItem();
                        } else {
                            SpawnEggItem item = ForgeSpawnEggItem.fromEntityType(content.type);
                            return item == null ? ItemStack.EMPTY : item.getDefaultInstance();
                        }
                    })
                    .filter(itemStack -> !itemStack.isEmpty())
                    .map(Ingredient::of)
                    .collect(Collectors.toList()));
        }
        if (recipe.outputs.containsKey(EntityMultiblockCapability.CAP)) {
            ingredients.setOutputs(VanillaTypes.ITEM, recipe.outputs.get(EntityMultiblockCapability.CAP).stream()
                    .map(Content::getContent)
                    .map(EntityIngredient.class::cast)
                    .map(content -> {
                        if (content.isEntityItem()) {
                            return content.getEntityItem();
                        } else {
                            SpawnEggItem item = ForgeSpawnEggItem.fromEntityType(content.type);
                            return item == null ? ItemStack.EMPTY : item.getDefaultInstance();
                        }
                    })
                    .filter(itemStack -> !itemStack.isEmpty())
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