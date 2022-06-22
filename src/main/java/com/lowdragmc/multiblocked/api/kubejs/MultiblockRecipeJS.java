package com.lowdragmc.multiblocked.api.kubejs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.recipe.Content;
import com.lowdragmc.multiblocked.api.recipe.RecipeBuilder;
import com.lowdragmc.multiblocked.api.recipe.RecipeCondition;
import com.lowdragmc.multiblocked.api.registry.MbdCapabilities;
import com.lowdragmc.multiblocked.api.registry.MbdRecipeConditions;
import com.lowdragmc.multiblocked.common.capability.*;
import com.lowdragmc.multiblocked.common.recipe.conditions.*;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.latvian.mods.kubejs.fluid.FluidStackJS;
import dev.latvian.mods.kubejs.item.ItemStackJS;
import dev.latvian.mods.kubejs.item.ingredient.IngredientJS;
import dev.latvian.mods.kubejs.item.ingredient.IngredientStackJS;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import dev.latvian.mods.kubejs.util.ListJS;
import dev.latvian.mods.kubejs.util.MapJS;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.ChemicalUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiblockRecipeJS extends RecipeJS {

    private String machineType;
    //Must serialize JsonObject before actually hand them in.
    private Map<MultiblockCapability<?>, List<JsonObject>> inputs = new HashMap<>();
    private Map<MultiblockCapability<?>, List<JsonObject>> outputs = new HashMap<>();
    private Map<MultiblockCapability<?>, List<JsonObject>> tickInputs = new HashMap<>();
    private Map<MultiblockCapability<?>, List<JsonObject>> tickOutputs = new HashMap<>();
    private List<RecipeCondition> conditions = new ArrayList<>();
    private CompoundTag data = new CompoundTag();
    private String text;
    private int duration = 200;
    private float chance = 1;
    private boolean perTick = false;

    public MultiblockRecipeJS setChance(float chance) {
        this.chance = chance;
        return this;
    }

    public MultiblockRecipeJS setPerTick(boolean perTick) {
        this.perTick = perTick;
        return this;
    }

    @Override
    public void create(ListJS args) {
        machineType = args.get(0).toString();
        currentRecipe = this;
    }

    //TODO: Figure a way to (de)serialize components
    public MultiblockRecipeJS text(String text) {
        this.text = text;
        return this;
    }

    public MultiblockRecipeJS duration(int duration) {
        this.duration = duration;
        return this;
    }

    public MultiblockRecipeJS data(CompoundTag data) {
        this.data = data;
        return this;
    }

    public <T> MultiblockRecipeJS input(MultiblockCapability<T> capability, T input, String slotName) {
        (perTick ? tickInputs : inputs).computeIfAbsent(capability, c -> new ArrayList<>()).add((JsonObject) capability.serializer.toJsonContent(new Content(input, chance, slotName)));
        return this;
    }

    public <T> MultiblockRecipeJS output(MultiblockCapability<T> capability, T output, String slotName) {
        (perTick ? tickOutputs : outputs).computeIfAbsent(capability, c -> new ArrayList<>()).add((JsonObject) capability.serializer.toJsonContent(new Content(output, chance, slotName)));
        return this;
    }

    //CAUTION: special handling is needed for certain objects, especially KubeJS wrapped ones (Ingredient, FluidStack, etc)
    public MultiblockRecipeJS inputFluids(FluidStackJS... fluidStacks) {
        for (FluidStackJS fluidStack : fluidStacks) {
            inputFluid(fluidStack);
        }
        return this;
    }

    public MultiblockRecipeJS inputFluid(FluidStackJS fluidStack) {
        return inputFluid(fluidStack, null);
    }

    public MultiblockRecipeJS inputFluid(FluidStackJS fluidStack, String slotName) {
        inputItems.add(ItemStackJS.of(fluidStack));
        FluidStackJS fluid = fluidStack.copy();
        fluid.removeChance();
        JsonObject contentJson = new JsonObject();
        contentJson.add("content", fluid.toJson());
        contentJson.addProperty("chance", chance);
        if (slotName != null)
            contentJson.addProperty("slotName", slotName);
        (perTick ? tickInputs : inputs).computeIfAbsent(FluidMultiblockCapability.CAP, c -> new ArrayList<>())
                .add(contentJson);
        return this;
    }

    public MultiblockRecipeJS outputFluids(FluidStackJS... fluidStacks) {
        for (FluidStackJS fluidStack : fluidStacks) {
            outputFluid(fluidStack);
        }
        return this;
    }

    public MultiblockRecipeJS outputFluid(FluidStackJS fluidStack) {
        return outputFluid(fluidStack, null);
    }

    public MultiblockRecipeJS outputFluid(FluidStackJS fluidStack, String slotName) {
        outputItems.add(ItemStackJS.of(fluidStack));
        FluidStackJS fluid = fluidStack.copy();
        fluid.removeChance();
        JsonObject contentJson = new JsonObject();
        contentJson.add("content", fluid.toJson());
        contentJson.addProperty("chance", chance);
        if (slotName != null)
            contentJson.addProperty("slotName", slotName);
        (perTick ? tickOutputs : outputs).computeIfAbsent(FluidMultiblockCapability.CAP, c -> new ArrayList<>())
                .add(contentJson);
        return this;
    }

    //CAUTION: special handling is needed for certain objects, especially KubeJS wrapped ones (Ingredient, FluidStack, etc)
    public MultiblockRecipeJS inputItems(IngredientJS... ingredients) {
        for (IngredientJS ingredient : ingredients)
            inputItem(ingredient);
        return this;
    }

    public MultiblockRecipeJS inputItem(IngredientJS ingredient) {
        return inputItem(ingredient, null);
    }

    public MultiblockRecipeJS inputItem(IngredientJS ingredient, String slotName) {
        JsonObject contentJson = new JsonObject();
        inputItems.add(ingredient);
        if (ingredient instanceof ItemStackJS itemStackJS) {
            ItemStackJS stackJS = itemStackJS.copy();
            stackJS.removeChance();
            contentJson.add("content", stackJS.toJson());
        } else
            contentJson.add("content", ingredient.toJson());
        contentJson.addProperty("chance", chance);
        if (slotName != null)
            contentJson.addProperty("slotName", slotName);
        (perTick ? tickInputs : inputs).computeIfAbsent(ItemMultiblockCapability.CAP, c -> new ArrayList<>())
                .add(contentJson);
        return this;
    }

    public MultiblockRecipeJS outputItems(IngredientJS... ingredients) {
        for (IngredientJS ingredient : ingredients) {
            outputItem(ingredient);
        }
        return this;
    }

    public MultiblockRecipeJS outputItem(IngredientJS ingredient) {
        return outputItem(ingredient, null);
    }

    public MultiblockRecipeJS outputItem(IngredientJS ingredient, String slotName) {
        JsonObject contentJson = new JsonObject();
        if (ingredient instanceof ItemStackJS itemStackJS) {
            ItemStackJS stackJS = itemStackJS.copy();
            stackJS.removeChance();
            contentJson.add("content", stackJS.toJson());
            outputItems.add(itemStackJS);
        } else
            contentJson.add("content", ingredient.toJson());
        contentJson.addProperty("chance", chance);
        if (slotName != null)
            contentJson.addProperty("slotName", slotName);
        (perTick ? tickOutputs : outputs).computeIfAbsent(ItemMultiblockCapability.CAP, c -> new ArrayList<>())
                .add(contentJson);
        return this;
    }

    //Numbers
    public MultiblockRecipeJS inputFE(int fe) {
        return inputFE(fe, null);
    }

    public MultiblockRecipeJS inputFE(int fe, String slotName) {
        return input(FEMultiblockCapability.CAP, fe, slotName);
    }

    public MultiblockRecipeJS outputFE(int fe) {
        return outputFE(fe, null);
    }

    public MultiblockRecipeJS outputFE(int fe, String slotName) {
        return output(FEMultiblockCapability.CAP, fe, slotName);
    }

    public MultiblockRecipeJS inputMana(int mana) {
        return inputMana(mana, null);
    }

    public MultiblockRecipeJS inputMana(int mana, String slotName) {
        if (Multiblocked.isBotLoaded())
            input(ManaBotaniaCapability.CAP, mana, slotName);
        return this;
    }

    public MultiblockRecipeJS outputMana(int mana) {
        return outputMana(mana, null);
    }

    public MultiblockRecipeJS outputMana(int mana, String slotName) {
        if (Multiblocked.isBotLoaded())
            output(ManaBotaniaCapability.CAP, mana, slotName);
        return this;
    }

    public MultiblockRecipeJS inputHeat(double heat) {
        return inputHeat(heat, null);
    }

    public MultiblockRecipeJS inputHeat(double heat, String slotName) {
        if (Multiblocked.isMekLoaded())
            input(HeatMekanismCapability.CAP, heat, slotName);
        return this;
    }

    public MultiblockRecipeJS outputHeat(double heat) {
        return outputHeat(heat, null);
    }

    public MultiblockRecipeJS outputHeat(double heat, String slotName) {
        if (Multiblocked.isMekLoaded())
            output(HeatMekanismCapability.CAP, heat, slotName);
        return this;
    }

    public MultiblockRecipeJS inputStress(float stress) {
        return inputStress(stress, null);
    }

    public MultiblockRecipeJS inputStress(float stress, String slotName) {
        if (Multiblocked.isCreateLoaded())
            input(CreateStressCapacityCapability.CAP, stress, slotName);
        return this;
    }

    public MultiblockRecipeJS outputStress(float stress) {
        return outputStress(stress, null);
    }

    public MultiblockRecipeJS outputStress(float stress, String slotName) {
        if (Multiblocked.isCreateLoaded())
            output(CreateStressCapacityCapability.CAP, stress, slotName);
        return this;
    }

    public MultiblockRecipeJS inputEMC(long emc) {
        inputEMC(emc, null);
        return this;
    }

    public MultiblockRecipeJS outputEMC(long emc) {
        outputEMC(emc, null);
        return this;
    }

    public MultiblockRecipeJS inputEMC(long emc, String slotName) {
        if (Multiblocked.isProjectELoaded()) {
            input(EMCProjectECapability.CAP, BigInteger.valueOf(emc), slotName);
        }
        return this;
    }

    public MultiblockRecipeJS outputEMC(long emc, String slotName) {
        if (Multiblocked.isProjectELoaded()) {
            output(EMCProjectECapability.CAP, BigInteger.valueOf(emc), slotName);
        }
        return this;
    }

    public MultiblockRecipeJS inputAura(int aura) {
        inputAura(aura, null);
        return this;
    }

    public MultiblockRecipeJS outputAura(int aura) {
        outputAura(aura, null);
        return this;
    }

    public MultiblockRecipeJS inputAura(int aura, String slotName) {
        if (Multiblocked.isNaturesAuraLoaded()) {
            input(AuraMultiblockCapability.CAP, aura, slotName);
        }
        return this;
    }

    public MultiblockRecipeJS outputAura(int aura, String slotName) {
        if (Multiblocked.isNaturesAuraLoaded()) {
            output(AuraMultiblockCapability.CAP, aura, slotName);
        }
        return this;
    }

    private <T extends Chemical<T>, U extends ChemicalStack<T>> MultiblockRecipeJS inputMekanismStack(ChemicalMekanismCapability<T, U> capability, MapJS stack, String slotName) {
        if (Multiblocked.isMekLoaded()) {
            String type = (String) stack.get("type");
            long amount = (long) stack.get("amount");
            T chemical = ChemicalUtils.readChemicalFromRegistry(new ResourceLocation(type), capability.empty, capability.registry.get());
            U chemicalStack = capability.createStack.apply(chemical, amount);
            input(capability, chemicalStack, slotName);
        }
        return this;
    }

    private <T extends Chemical<T>, U extends ChemicalStack<T>> MultiblockRecipeJS outputMekanismStack(ChemicalMekanismCapability<T, U> capability, MapJS stack, String slotName) {
        if (Multiblocked.isMekLoaded()) {
            String type = (String) stack.get("type");
            long amount = (long) stack.get("amount");
            T chemical = ChemicalUtils.readChemicalFromRegistry(new ResourceLocation(type), capability.empty, capability.registry.get());
            U chemicalStack = capability.createStack.apply(chemical, amount);
            output(capability, chemicalStack, slotName);
        }
        return this;
    }

    public MultiblockRecipeJS inputInfusion(MapJS stack) {
        return inputInfusion(stack, null);
    }

    public MultiblockRecipeJS inputInfusion(MapJS stack, String slotName) {
        return inputMekanismStack(ChemicalMekanismCapability.CAP_INFUSE, stack, slotName);
    }

    public MultiblockRecipeJS outputInfusion(MapJS stack) {
        return outputInfusion(stack, null);
    }

    public MultiblockRecipeJS outputInfusion(MapJS stack, String slotName) {
        return outputMekanismStack(ChemicalMekanismCapability.CAP_INFUSE, stack, slotName);
    }

    public MultiblockRecipeJS inputGas(MapJS stack) {
        return inputGas(stack, null);
    }

    public MultiblockRecipeJS inputGas(MapJS stack, String slotName) {
        return inputMekanismStack(ChemicalMekanismCapability.CAP_GAS, stack, slotName);
    }

    public MultiblockRecipeJS outputGas(MapJS stack) {
        return outputGas(stack, null);
    }

    public MultiblockRecipeJS outputGas(MapJS stack, String slotName) {
        return outputMekanismStack(ChemicalMekanismCapability.CAP_GAS, stack, slotName);
    }

    public MultiblockRecipeJS inputPigment(MapJS stack) {
        return inputPigment(stack, null);
    }

    public MultiblockRecipeJS inputPigment(MapJS stack, String slotName) {
        return inputMekanismStack(ChemicalMekanismCapability.CAP_PIGMENT, stack, slotName);
    }

    public MultiblockRecipeJS outputPigment(MapJS stack) {
        return outputPigment(stack, null);
    }

    public MultiblockRecipeJS outputPigment(MapJS stack, String slotName) {
        return outputMekanismStack(ChemicalMekanismCapability.CAP_PIGMENT, stack, slotName);
    }

    public MultiblockRecipeJS inputSlurry(MapJS stack) {
        return inputSlurry(stack, null);
    }

    public MultiblockRecipeJS inputSlurry(MapJS stack, String slotName) {
        return inputMekanismStack(ChemicalMekanismCapability.CAP_SLURRY, stack, slotName);
    }

    public MultiblockRecipeJS outputSlurry(MapJS stack) {
        return outputSlurry(stack, null);
    }

    public MultiblockRecipeJS outputSlurry(MapJS stack, String slotName) {
        return outputMekanismStack(ChemicalMekanismCapability.CAP_SLURRY, stack, slotName);
    }

    public MultiblockRecipeJS dimension(ResourceLocation dimension) {
        return dimension(dimension, false);
    }

    public MultiblockRecipeJS dimension(ResourceLocation dimension, boolean reverse) {
        conditions.add(new DimensionCondition(dimension).setReverse(reverse));
        return this;
    }

    public MultiblockRecipeJS biome(ResourceLocation biome) {
        return biome(biome, false);
    }

    public MultiblockRecipeJS biome(ResourceLocation biome, boolean reverse) {
        conditions.add(new BiomeCondition(biome).setReverse(reverse));
        return this;
    }

    public MultiblockRecipeJS raining(float level) {
        return raining(level, false);
    }

    public MultiblockRecipeJS raining(float level, boolean reverse) {
        conditions.add(new RainingCondition(level).setReverse(reverse));
        return this;
    }

    public MultiblockRecipeJS thunder(float level) {
        return thunder(level, false);
    }

    public MultiblockRecipeJS thunder(float level, boolean reverse) {
        conditions.add(new ThunderCondition(level).setReverse(reverse));
        return this;
    }

    public MultiblockRecipeJS yLevel(int min, int max) {
        return yLevel(min, max, false);
    }

    public MultiblockRecipeJS yLevel(int min, int max, boolean reverse) {
        conditions.add(new PositionYCondition(min, max).setReverse(reverse));
        return this;
    }

    public MultiblockRecipeJS requiresBlock(BlockState state, int count) {
        return requiresBlock(state, count, false);
    }

    public MultiblockRecipeJS requiresBlock(BlockState state, int count, boolean reverse) {
        conditions.add(new BlockCondition(state, count).setReverse(reverse));
        return this;
    }

    private static Map<MultiblockCapability<?>, List<JsonObject>> deserializeCapabilities(JsonObject json) {
        Map<MultiblockCapability<?>, List<JsonObject>> result = new HashMap<>();
        for (String key : json.keySet()) {
            List<JsonObject> objects = new ArrayList<>();
            json.getAsJsonArray(key).forEach(o -> objects.add(o.getAsJsonObject()));
            result.put(MbdCapabilities.get(key), objects);
        }
        return result;
    }

    @Override
    public void deserialize() {
        //Actually I don't think it is needed, but still implemented for potential future usage.
        machineType = GsonHelper.getAsString(json, "machine_map");
        try {
            if (json.has("data"))
                data = TagParser.parseTag(GsonHelper.getAsString(json, "data"));
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }

        if (json.has("duration"))
            duration = GsonHelper.getAsInt(json, "duration");
        if (json.has("text"))
            text = GsonHelper.getAsString(json, "text");
        if (json.has("inputs"))
            inputs = deserializeCapabilities(json.getAsJsonObject("inputs"));
        if (json.has("tickInputs"))
            tickInputs = deserializeCapabilities(json.getAsJsonObject("tickInputs"));
        if (json.has("outputs"))
            outputs = deserializeCapabilities(json.getAsJsonObject("outputs"));
        if (json.has("tickOutputs"))
            tickOutputs = deserializeCapabilities(json.getAsJsonObject("tickOutputs"));

        if (json.has("recipeConditions")) {
            JsonObject conditionsJson = json.getAsJsonObject("recipeConditions");
            for (String key : conditionsJson.keySet()) {
                conditions.add(MbdRecipeConditions.getCondition(key).createTemplate().deserialize(conditionsJson.getAsJsonObject(key)));
            }
        }
    }

    private static JsonObject serializeCapabilities(Map<MultiblockCapability<?>, List<JsonObject>> capabilities) {
        JsonObject result = new JsonObject();
        capabilities.forEach(((capability, contents) -> {
            JsonArray contentsJson = new JsonArray();
            for (JsonObject content : contents) {
                contentsJson.add(content);
            }
            result.add(capability.name, contentsJson);
        }));

        return result;
    }

    @Override
    public void serialize() {
        if (serializeOutputs) {
            json.add("outputs", serializeCapabilities(outputs));
            json.add("tickOutputs", serializeCapabilities(tickOutputs));
        }

        if (serializeInputs) {
            json.add("inputs", serializeCapabilities(inputs));
            json.add("tickInputs", serializeCapabilities(tickInputs));
        }

        JsonObject conditionsJson = new JsonObject();
        for (RecipeCondition condition : conditions) {
            conditionsJson.add(condition.getType(), condition.serialize());
        }
        json.add("recipeConditions", conditionsJson);
        json.addProperty("machine_map", machineType);
        if (text != null)
            json.addProperty("text", text);
        json.addProperty("data", data.getAsString());
        json.addProperty("duration", duration);

    }

    @Override
    public @Nullable JsonElement serializeIngredientStack(IngredientStackJS in) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "multiblocked:sized");
        json.add("ingredient", in.ingredient.toJson());
        json.addProperty("count", in.getCount());
        return json;
    }
}
