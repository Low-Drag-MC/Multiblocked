package com.lowdragmc.multiblocked.api.kubejs.recipes;

import com.google.common.collect.ImmutableList;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.kubejs.MultiblockedJSPlugin;
import com.lowdragmc.multiblocked.api.recipe.Content;
import com.lowdragmc.multiblocked.api.recipe.ItemsIngredient;
import com.lowdragmc.multiblocked.api.recipe.RecipeBuilder;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import com.lowdragmc.multiblocked.api.registry.MbdCapabilities;
import com.lowdragmc.multiblocked.common.capability.*;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import dev.latvian.mods.kubejs.fluid.FluidStackJS;
import dev.latvian.mods.kubejs.item.ItemStackJS;
import dev.latvian.mods.kubejs.item.ingredient.IngredientJS;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.stream.Stream;

public class RecipeBuilderJS {
    private final RecipeMap recipeMap;
    private final RecipeBuilder builder;

    public RecipeBuilderJS(RecipeMap map) {
        this.recipeMap = map;
        this.builder = new RecipeBuilder(this.recipeMap);
    }

    private RecipeBuilderJS(RecipeBuilderJS map) {
        this.recipeMap = map.recipeMap;
        this.builder = map.builder.copy();
    }

    public RecipeBuilderJS copy() {
        return new RecipeBuilderJS(this);
    }

    public RecipeBuilderJS duration(int duration) {
        this.builder.duration(duration);
        return this;
    }

    public RecipeBuilderJS data(String key, Object object) {
        this.builder.data(key, object);
        return this;
    }

    public RecipeBuilderJS text(Component text) {
        this.builder.text(text);
        return this;
    }

    public RecipeBuilderJS name(String name) {
        this.builder.name(name);
        return this;
    }

    //Directly manipulating recipe properties since using var is ambiguous.
    //Guard obj with capability::of since type is erased at runtime.
    public <T> RecipeBuilderJS input(MultiblockCapability<T> capability, T obj, boolean perTick, float chance, String slotName) {
        (perTick ? builder.tickInputBuilder : builder.inputBuilder)
                .computeIfAbsent(capability, c -> ImmutableList.builder())
                .addAll(Stream.of(obj).map(capability::of).map(o -> new Content(o, chance, slotName)).iterator());
        return this;
    }

    public <T> RecipeBuilderJS input(MultiblockCapability<T> capability, T obj, boolean perTick) {
        return input(capability, obj, perTick, 1, null);
    }

    public <T> RecipeBuilderJS input(MultiblockCapability<T> capability, T obj, float chance) {
        return input(capability, obj, false, chance, null);
    }

    public <T> RecipeBuilderJS input(MultiblockCapability<T> capability, T obj) {
        return input(capability, obj, false);
    }

    public <T> RecipeBuilderJS output(MultiblockCapability<T> capability, T obj, boolean perTick, float chance, String slotName) {
        (perTick ? builder.tickOutputBuilder : builder.outputBuilder)
                .computeIfAbsent(capability, c -> ImmutableList.builder())
                .addAll(Stream.of(obj).map(capability::of).map(o -> new Content(o, chance, slotName)).iterator());
        return this;
    }

    public <T> RecipeBuilderJS output(MultiblockCapability<T> capability, T obj, boolean perTick) {
        return output(capability, obj, perTick, 1, null);
    }

    public <T> RecipeBuilderJS output(MultiblockCapability<T> capability, T obj, float chance) {
        return output(capability, obj, false, chance, null);
    }

    public <T> RecipeBuilderJS output(MultiblockCapability<T> capability, T obj) {
        return output(capability, obj, false);
    }

    //Still need an internal method to comply with Java's stupid type system.
    private <T> void objInput(MultiblockCapability<T> capability, Object obj, boolean perTick, float chance, String slotName) {
        (perTick ? builder.tickInputBuilder : builder.inputBuilder)
                .computeIfAbsent(capability, c -> ImmutableList.builder())
                .addAll(Stream.of(obj).map(capability::of).map(o -> new Content(o, chance, slotName)).iterator());
    }

    private <T> void objOutput(MultiblockCapability<T> capability, Object obj, boolean perTick, float chance, String slotName) {
        (perTick ? builder.tickOutputBuilder : builder.outputBuilder)
                .computeIfAbsent(capability, c -> ImmutableList.builder())
                .addAll(Stream.of(obj).map(capability::of).map(o -> new Content(o, chance, slotName)).iterator());
    }

    public RecipeBuilderJS inputFE(int forgeEnergy) {
        return inputFE(forgeEnergy, false);
    }

    public RecipeBuilderJS inputFE(int forgeEnergy, boolean perTick) {
        return inputFE(forgeEnergy, perTick, 1);
    }

    public RecipeBuilderJS inputFE(int forgeEnergy, boolean perTick, float chance) {
        return inputFE(forgeEnergy, perTick, chance, null);
    }

    public RecipeBuilderJS inputFE(int forgeEnergy, boolean perTick, float chance, String slotName) {
        return input(FEMultiblockCapability.CAP, forgeEnergy, perTick, chance, slotName);
    }


    public RecipeBuilderJS outputFE(int forgeEnergy) {
        return outputFE(forgeEnergy, false);
    }

    public RecipeBuilderJS outputFE(int forgeEnergy, boolean perTick) {
        return outputFE(forgeEnergy, perTick, 1);
    }

    public RecipeBuilderJS outputFE(int forgeEnergy, boolean perTick, float chance) {
        return outputFE(forgeEnergy, perTick, chance, null);
    }

    public RecipeBuilderJS outputFE(int forgeEnergy, boolean perTick, float chance, String slotName) {
        return output(FEMultiblockCapability.CAP, forgeEnergy, perTick, chance, slotName);
    }


    public RecipeBuilderJS inputItems(IngredientJS... ingredients) {
        for (IngredientJS ingredient : ingredients) {
            boolean perTick = ((IMBDRecipeProperty) ingredient).isPerTick();
            float chance = 1;
            if (ingredient instanceof ItemStackJS itemStack) {
                chance = Double.isNaN(itemStack.getChance()) ? 1 : (float) itemStack.getChance();
            }
            input(ItemMultiblockCapability.CAP, new ItemsIngredient(ingredient.createVanillaIngredient()), perTick, chance, ((IMBDRecipeProperty) ingredient).atSlot());
        }
        return this;
    }

    public RecipeBuilderJS outputItems(IngredientJS... ingredients) {
        for (IngredientJS ingredient : ingredients) {
            boolean perTick = ((IMBDRecipeProperty) ingredient).isPerTick();
            float chance = 1;
            if (ingredient instanceof ItemStackJS itemStack) {
                chance = Double.isNaN(itemStack.getChance()) ? 1 : (float) itemStack.getChance();
            }
            output(ItemMultiblockCapability.CAP, new ItemsIngredient(ingredient.createVanillaIngredient()), perTick, chance, ((IMBDRecipeProperty) ingredient).atSlot());
        }
        return this;
    }

    public RecipeBuilderJS inputFluids(FluidStackJS... fluidStacks) {
        for (FluidStackJS fluidStack : fluidStacks) {
            boolean perTick = ((IMBDRecipeProperty) fluidStack).isPerTick();
            float chance = Double.isNaN(fluidStack.getChance()) ? 1 : (float) fluidStack.getChance();
            input(FluidMultiblockCapability.CAP, FluidStackHooksForge.toForge(fluidStack.getFluidStack()), perTick, chance, ((IMBDRecipeProperty) fluidStack).atSlot());
        }
        return this;
    }

    public RecipeBuilderJS outputFluids(FluidStackJS... fluidStacks) {
        for (FluidStackJS fluidStack : fluidStacks) {
            boolean perTick = ((IMBDRecipeProperty) fluidStack).isPerTick();
            float chance = Double.isNaN(fluidStack.getChance()) ? 1 : (float) fluidStack.getChance();
            output(FluidMultiblockCapability.CAP, FluidStackHooksForge.toForge(fluidStack.getFluidStack()), perTick, chance, ((IMBDRecipeProperty) fluidStack).atSlot());
        }
        return this;
    }

    public RecipeBuilderJS inputGas(Object gasStack) {
        return inputGas(gasStack, false);
    }

    public RecipeBuilderJS inputGas(Object gasStack, boolean perTick) {
        return inputGas(gasStack, perTick, 1);
    }

    public RecipeBuilderJS inputGas(Object gasStack, boolean perTick, float chance) {
        return inputGas(gasStack, perTick, chance, null);
    }

    public RecipeBuilderJS inputGas(Object gasStack, boolean perTick, float chance, String slotName) {
        if (Multiblocked.isMekLoaded()) {
            objInput(ChemicalMekanismCapability.CAP_GAS, gasStack, perTick, chance, slotName);
        }
        return this;
    }


    public RecipeBuilderJS outputGas(Object gasStack) {
        return outputGas(gasStack, false);
    }

    public RecipeBuilderJS outputGas(Object gasStack, boolean perTick) {
        return outputGas(gasStack, perTick, 1);
    }

    public RecipeBuilderJS outputGas(Object gasStack, boolean perTick, float chance) {
        return outputGas(gasStack, perTick, chance, null);
    }

    public RecipeBuilderJS outputGas(Object gasStack, boolean perTick, float chance, String slotName) {
        if (Multiblocked.isMekLoaded()) {
            objOutput(ChemicalMekanismCapability.CAP_GAS, gasStack, perTick, chance, slotName);
        }
        return this;
    }

    public RecipeBuilderJS inputInfusion(Object infusionStack) {
        return inputInfusion(infusionStack, false);
    }

    public RecipeBuilderJS inputInfusion(Object infusionStack, boolean perTick) {
        return inputInfusion(infusionStack, perTick, 1);
    }

    public RecipeBuilderJS inputInfusion(Object infusionStack, boolean perTick, float chance) {
        return inputInfusion(infusionStack, perTick, chance, null);
    }

    public RecipeBuilderJS inputInfusion(Object infusionStack, boolean perTick, float chance, String slotName) {
        if (Multiblocked.isMekLoaded()) {
            objInput(ChemicalMekanismCapability.CAP_INFUSE, infusionStack, perTick, chance, slotName);
        }
        return this;
    }

    public RecipeBuilderJS outputInfusion(Object infusionStack) {
        return outputInfusion(infusionStack, false);
    }

    public RecipeBuilderJS outputInfusion(Object infusionStack, boolean perTick) {
        return outputInfusion(infusionStack, perTick, 1);
    }

    public RecipeBuilderJS outputInfusion(Object infusionStack, boolean perTick, float chance) {
        return outputInfusion(infusionStack, perTick, chance, null);
    }

    public RecipeBuilderJS outputInfusion(Object infusionStack, boolean perTick, float chance, String slotName) {
        if (Multiblocked.isMekLoaded()) {
            objOutput(ChemicalMekanismCapability.CAP_INFUSE, infusionStack, perTick, chance, slotName);
        }
        return this;
    }

    public RecipeBuilderJS inputPigment(Object pigmentStack) {
        return inputPigment(pigmentStack, false);
    }

    public RecipeBuilderJS inputPigment(Object pigmentStack, boolean perTick) {
        return inputPigment(pigmentStack, perTick, 1);
    }

    public RecipeBuilderJS inputPigment(Object pigmentStack, boolean perTick, float chance) {
        return inputPigment(pigmentStack, perTick, chance, null);
    }

    public RecipeBuilderJS inputPigment(Object pigmentStack, boolean perTick, float chance, String slotName) {
        if (Multiblocked.isMekLoaded()) {
            objInput(ChemicalMekanismCapability.CAP_PIGMENT, pigmentStack, perTick, chance, slotName);
        }
        return this;
    }

    public RecipeBuilderJS outputPigment(Object pigmentStack) {
        return outputPigment(pigmentStack, false);
    }

    public RecipeBuilderJS outputPigment(Object pigmentStack, boolean perTick) {
        return outputPigment(pigmentStack, perTick, 1);
    }

    public RecipeBuilderJS outputPigment(Object pigmentStack, boolean perTick, float chance) {
        return outputPigment(pigmentStack, perTick, chance, null);
    }

    public RecipeBuilderJS outputPigment(Object pigmentStack, boolean perTick, float chance, String slotName) {
        if (Multiblocked.isMekLoaded()) {
            objOutput(ChemicalMekanismCapability.CAP_PIGMENT, pigmentStack, perTick, chance, slotName);
        }
        return this;
    }

    public RecipeBuilderJS inputMana(int mana) {
        return inputMana(mana, false);
    }

    public RecipeBuilderJS inputMana(int mana, boolean perTick) {
        return inputMana(mana, perTick, 1);
    }

    public RecipeBuilderJS inputMana(int mana, boolean perTick, float chance) {
        return inputMana(mana, perTick, chance, null);
    }

    public RecipeBuilderJS inputMana(int mana, boolean perTick, float chance, String slotName) {
        if (Multiblocked.isBotLoaded())
            input(ManaBotaniaCapability.CAP, mana, perTick, chance, slotName);
        return this;
    }

    public RecipeBuilderJS outputMana(int mana) {
        return outputMana(mana, false);
    }

    public RecipeBuilderJS outputMana(int mana, boolean perTick) {
        return outputMana(mana, perTick, 1);
    }

    public RecipeBuilderJS outputMana(int mana, boolean perTick, float chance) {
        return outputMana(mana, perTick, chance, null);
    }

    public RecipeBuilderJS outputMana(int mana, boolean perTick, float chance, String slotName) {
        if (Multiblocked.isBotLoaded())
            output(ManaBotaniaCapability.CAP, mana, perTick, chance, slotName);
        return this;
    }

    public RecipeBuilderJS inputCreate(float capacity) {
        return inputCreate(capacity, false);
    }

    public RecipeBuilderJS inputCreate(float capacity, boolean perTick) {
        return inputCreate(capacity, perTick, 1);
    }

    public RecipeBuilderJS inputCreate(float capacity, boolean perTick, float chance) {
        return inputCreate(capacity, perTick, chance, null);
    }

    public RecipeBuilderJS inputCreate(float capacity, boolean perTick, float chance, String slotName) {
        if (Multiblocked.isCreateLoaded())
            input(CreateStressCapacityCapability.CAP, capacity, perTick, chance, slotName);
        return this;
    }

    public RecipeBuilderJS outputCreate(float capacity) {
        return outputCreate(capacity, false);
    }

    public RecipeBuilderJS outputCreate(float capacity, boolean perTick) {
        return outputCreate(capacity, perTick, 1);
    }

    public RecipeBuilderJS outputCreate(float capacity, boolean perTick, float chance) {
        return outputCreate(capacity, perTick, chance, null);
    }

    public RecipeBuilderJS outputCreate(float capacity, boolean perTick, float chance, String slotName) {
        if (Multiblocked.isCreateLoaded())
            output(CreateStressCapacityCapability.CAP, capacity, perTick, chance, slotName);
        return this;
    }


    //Conditions
    public RecipeBuilderJS conditions(Consumer<RecipeBuilder> conditionsCallback) {
        conditionsCallback.accept(this.builder);
        return this;
    }

    @HideFromJS
    public void buildAndRegister() {
        var recipe = builder.build();
        MultiblockedJSPlugin.ADDED_RECIPES.add(recipe.uid);
        recipeMap.addRecipe(recipe);
    }
}
