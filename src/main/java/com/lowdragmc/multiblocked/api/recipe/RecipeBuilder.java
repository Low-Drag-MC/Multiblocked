package com.lowdragmc.multiblocked.api.recipe;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.common.capability.FEMultiblockCapability;
import com.lowdragmc.multiblocked.common.capability.FluidMultiblockCapability;
import com.lowdragmc.multiblocked.common.capability.ItemMultiblockCapability;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fluids.FluidStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class RecipeBuilder {

    public final RecipeMap recipeMap;
    public final Map<MultiblockCapability<?>, ImmutableList.Builder<Tuple<Object, Float>>> inputBuilder = new HashMap<>();
    public final Map<MultiblockCapability<?>, ImmutableList.Builder<Tuple<Object, Float>>> tickInputBuilder = new HashMap<>();
    public final Map<MultiblockCapability<?>, ImmutableList.Builder<Tuple<Object, Float>>> outputBuilder = new HashMap<>();
    public final Map<MultiblockCapability<?>, ImmutableList.Builder<Tuple<Object, Float>>> tickOutputBuilder = new HashMap<>();
    public final Map<String, Object> data = new HashMap<>();
    protected int duration;
    protected ITextComponent text;
    protected StringBuilder keyBuilder = new StringBuilder(); // to make each recipe has a unique identifier and no need to set name yourself.
    protected boolean perTick;

    public RecipeBuilder(RecipeMap recipeMap) {
        this.recipeMap = recipeMap;
    }

    public RecipeBuilder copy() {
        RecipeBuilder copy = new RecipeBuilder(recipeMap);
        inputBuilder.forEach((k, v)->{
            ImmutableList.Builder<Tuple<Object, Float>> builder = ImmutableList.builder();
            copy.inputBuilder.put(k, builder.addAll(v.build()));
        });
        outputBuilder.forEach((k, v)->{
            ImmutableList.Builder<Tuple<Object, Float>> builder = ImmutableList.builder();
            copy.outputBuilder.put(k, builder.addAll(v.build()));
        });
        tickInputBuilder.forEach((k, v)->{
            ImmutableList.Builder<Tuple<Object, Float>> builder = ImmutableList.builder();
            copy.tickInputBuilder.put(k, builder.addAll(v.build()));
        });
        tickOutputBuilder.forEach((k, v)->{
            ImmutableList.Builder<Tuple<Object, Float>> builder = ImmutableList.builder();
            copy.tickOutputBuilder.put(k, builder.addAll(v.build()));
        });
        data.forEach(copy.data::put);
        copy.duration = this.duration;
        copy.keyBuilder = new StringBuilder(keyBuilder.toString());
        return copy;
    }

    public RecipeBuilder duration(int duration) {
        this.duration = duration;
        return this;
    }

    public RecipeBuilder data(String key, Object object) {
        this.data.put(key, object);
        return this;
    }


    public RecipeBuilder text(ITextComponent text) {
        this.text = text;
        return this;
    }

    public RecipeBuilder perTick(boolean perTick) {
        this.perTick = perTick;
        return this;
    }

    public RecipeBuilder input(MultiblockCapability<?> capability, float chance, Object... obj) {
        keyBuilder.append(chance);
        (perTick ? tickInputBuilder : inputBuilder).computeIfAbsent(capability, c -> ImmutableList.builder()).add(Arrays.stream(obj).map(o->new Tuple<>(o, chance)).toArray(Tuple[]::new));
        return this;
    }

    public RecipeBuilder output(MultiblockCapability<?> capability, float chance, Object... obj) {
        keyBuilder.append(chance);
        (perTick ? tickOutputBuilder : outputBuilder).computeIfAbsent(capability, c -> ImmutableList.builder()).add(Arrays.stream(obj).map(o->new Tuple<>(o, chance)).toArray(Tuple[]::new));
        return this;
    }

    public RecipeBuilder inputFE(int forgeEnergy) {
        return inputFE(1, forgeEnergy);
    }

    public RecipeBuilder outputFE(int forgeEnergy) {
        return outputFE(1, forgeEnergy);
    }

    public RecipeBuilder inputFE(float chance, int forgeEnergy) {
        keyBuilder.append(FEMultiblockCapability.CAP.name).append(forgeEnergy);
        return input(FEMultiblockCapability.CAP, chance, forgeEnergy);
    }

    public RecipeBuilder outputFE(float chance, int forgeEnergy) {
        keyBuilder.append(FEMultiblockCapability.CAP.name).append(forgeEnergy);
        return output(FEMultiblockCapability.CAP, chance, forgeEnergy);
    }

    public RecipeBuilder inputItems(ItemsIngredient... inputs) {
        return inputItems(1, inputs);
    }

    public RecipeBuilder outputItems(ItemStack... outputs) {
        return outputItems(1, outputs);
    }

    public RecipeBuilder inputItems(float chance, ItemsIngredient... inputs) {
        keyBuilder.append(ItemMultiblockCapability.CAP.name);
        for (ItemsIngredient input : inputs) {
            keyBuilder.append(input.hashCode());
        }
        return input(ItemMultiblockCapability.CAP, chance, (Object[]) inputs);
    }

    public RecipeBuilder outputItems(float chance, ItemStack... outputs) {
        keyBuilder.append(ItemMultiblockCapability.CAP.name);
        for (ItemStack output : outputs) {
            keyBuilder.append(output.getCount());
            ResourceLocation name = output.getItem().getRegistryName();
            keyBuilder.append(name == null ? "" : name.hashCode());
        }
        return output(ItemMultiblockCapability.CAP, chance, Arrays.stream(outputs).map(ItemsIngredient::new).toArray());
    }

    public RecipeBuilder inputFluids(FluidStack... inputs) {
        return inputFluids(1, inputs);
    }

    public RecipeBuilder outputFluids(FluidStack... outputs) {
        return outputFluids(1, outputs);
    }

    public RecipeBuilder inputFluids(float chance, FluidStack... inputs) {
        keyBuilder.append(FluidMultiblockCapability.CAP.name);
        for (FluidStack input : inputs) {
            keyBuilder.append(input.getAmount());
            keyBuilder.append(input.getTranslationKey());
        }
        return input(FluidMultiblockCapability.CAP, chance, (Object[]) inputs);
    }

    public RecipeBuilder outputFluids(float chance, FluidStack... outputs) {
        keyBuilder.append(FluidMultiblockCapability.CAP.name);
        for (FluidStack output : outputs) {
            keyBuilder.append(output.getAmount());
            keyBuilder.append(output.getTranslationKey());
        }
        return output(FluidMultiblockCapability.CAP, chance, (Object[]) outputs);
    }


    public Recipe build() {
        ImmutableMap.Builder<MultiblockCapability<?>, ImmutableList<Tuple<Object, Float>>> inputBuilder = new ImmutableMap.Builder<>();
        for (Map.Entry<MultiblockCapability<?>, ImmutableList.Builder<Tuple<Object, Float>>> entry : this.inputBuilder.entrySet()) {
            inputBuilder.put(entry.getKey(), entry.getValue().build());
        }
        ImmutableMap.Builder<MultiblockCapability<?>, ImmutableList<Tuple<Object, Float>>> outputBuilder = new ImmutableMap.Builder<>();
        for (Map.Entry<MultiblockCapability<?>, ImmutableList.Builder<Tuple<Object, Float>>> entry : this.outputBuilder.entrySet()) {
            outputBuilder.put(entry.getKey(), entry.getValue().build());
        }
        ImmutableMap.Builder<MultiblockCapability<?>, ImmutableList<Tuple<Object, Float>>> tickInputBuilder = new ImmutableMap.Builder<>();
        for (Map.Entry<MultiblockCapability<?>, ImmutableList.Builder<Tuple<Object, Float>>> entry : this.tickInputBuilder.entrySet()) {
            tickInputBuilder.put(entry.getKey(), entry.getValue().build());
        }
        ImmutableMap.Builder<MultiblockCapability<?>, ImmutableList<Tuple<Object, Float>>> tickOutputBuilder = new ImmutableMap.Builder<>();
        for (Map.Entry<MultiblockCapability<?>, ImmutableList.Builder<Tuple<Object, Float>>> entry : this.tickOutputBuilder.entrySet()) {
            tickOutputBuilder.put(entry.getKey(), entry.getValue().build());
        }
        return new Recipe(keyBuilder.toString(), inputBuilder.build(), outputBuilder.build(), tickInputBuilder.build(), tickOutputBuilder.build(), data.isEmpty() ? Recipe.EMPTY : ImmutableMap.copyOf(data), text, duration);
    }

    public void buildAndRegister(){
        recipeMap.addRecipe(build());
    }
}
