package com.lowdragmc.multiblocked.api.recipe;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.common.capability.*;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.SlurryStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Tuple;
import net.minecraftforge.fluids.FluidStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unchecked")
public class RecipeBuilder {

    public final RecipeMap recipeMap;
    public final Map<MultiblockCapability<?>, ImmutableList.Builder<Tuple<Object, Float>>> inputBuilder = new HashMap<>();
    public final Map<MultiblockCapability<?>, ImmutableList.Builder<Tuple<Object, Float>>> tickInputBuilder = new HashMap<>();
    public final Map<MultiblockCapability<?>, ImmutableList.Builder<Tuple<Object, Float>>> outputBuilder = new HashMap<>();
    public final Map<MultiblockCapability<?>, ImmutableList.Builder<Tuple<Object, Float>>> tickOutputBuilder = new HashMap<>();
    public final Map<String, Object> data = new HashMap<>();
    protected int duration;
    protected TextComponent text;
    protected boolean perTick;
    protected String fixedName;
    protected float chance;


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
        copy.data.putAll(data);
        copy.duration = this.duration;
        copy.fixedName = null;
        copy.chance = 1;
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


    public RecipeBuilder text(TextComponent text) {
        this.text = text;
        return this;
    }

    public RecipeBuilder perTick(boolean perTick) {
        this.perTick = perTick;
        return this;
    }

    public RecipeBuilder name(String name) {
        this.fixedName = name;
        return this;
    }

    public RecipeBuilder chance(float chance) {
        this.chance = chance;
        return this;
    }

    public <T> RecipeBuilder input(MultiblockCapability<T> capability, T... obj) {
        (perTick ? tickInputBuilder : inputBuilder).computeIfAbsent(capability, c -> ImmutableList.builder()).add(Arrays.stream(obj).map(o->new Tuple<>(o, chance)).toArray(Tuple[]::new));
        return this;
    }

    public <T> RecipeBuilder output(MultiblockCapability<T> capability, T... obj) {
        (perTick ? tickOutputBuilder : outputBuilder).computeIfAbsent(capability, c -> ImmutableList.builder()).add(Arrays.stream(obj).map(o->new Tuple<>(o, chance)).toArray(Tuple[]::new));
        return this;
    }

    public <T> RecipeBuilder inputs(MultiblockCapability<T> capability, Object... obj) {
        (perTick ? tickInputBuilder : inputBuilder).computeIfAbsent(capability, c -> ImmutableList.builder()).add(Arrays.stream(obj)
                .map(capability::of)
                .map(o->new Tuple<>(o, chance)).toArray(Tuple[]::new));
        return this;
    }

    public <T> RecipeBuilder outputs(MultiblockCapability<T> capability, Object... obj) {
        (perTick ? tickOutputBuilder : outputBuilder).computeIfAbsent(capability, c -> ImmutableList.builder()).add(Arrays.stream(obj)
                .map(capability::of)
                .map(o->new Tuple<>(o, chance)).toArray(Tuple[]::new));
        return this;
    }

    public RecipeBuilder inputFE(int forgeEnergy) {
        return input(FEMultiblockCapability.CAP, forgeEnergy);
    }

    public RecipeBuilder outputFE(int forgeEnergy) {
        return output(FEMultiblockCapability.CAP, forgeEnergy);
    }

    public RecipeBuilder inputItems(ItemsIngredient... inputs) {
        return input(ItemMultiblockCapability.CAP, inputs);
    }

    public RecipeBuilder outputItems(ItemStack... outputs) {
        return output(ItemMultiblockCapability.CAP, Arrays.stream(outputs).map(ItemsIngredient::new).toArray(ItemsIngredient[]::new));
    }

    public RecipeBuilder inputFluids(FluidStack... inputs) {
        return input(FluidMultiblockCapability.CAP, inputs);
    }

    public RecipeBuilder outputFluids(FluidStack... outputs) {
        return output(FluidMultiblockCapability.CAP, outputs);
    }

    public RecipeBuilder inputHeat(double heat) {
        if (Multiblocked.isMekLoaded()) {
            return input(HeatMekanismCapability.CAP, heat);
        }
        return this;
    }

    public RecipeBuilder outputHeat(double heat) {
        if (Multiblocked.isMekLoaded()) {
            return output(HeatMekanismCapability.CAP, heat);
        }
        return this;
    }

    public RecipeBuilder inputGases(Object... inputs) {
        if (Multiblocked.isMekLoaded()) {
            return input(ChemicalMekanismCapability.CAP_GAS, Arrays.stream(inputs).map(ChemicalMekanismCapability.CAP_GAS::of).toArray(GasStack[]::new));
        }
        return this;
    }

    public RecipeBuilder outputGases(Object... outputs) {
        if (Multiblocked.isMekLoaded()) {
            return output(ChemicalMekanismCapability.CAP_GAS, Arrays.stream(outputs).map(ChemicalMekanismCapability.CAP_GAS::of).toArray(GasStack[]::new));
        }
        return this;
    }

    public RecipeBuilder inputSlurries(Object... inputs) {
        if (Multiblocked.isMekLoaded()) {
            return input(ChemicalMekanismCapability.CAP_SLURRY, Arrays.stream(inputs).map(ChemicalMekanismCapability.CAP_SLURRY::of).toArray(SlurryStack[]::new));
        }
        return this;
    }

    public RecipeBuilder outputSlurries(Object... outputs) {
        if (Multiblocked.isMekLoaded()) {
            return output(ChemicalMekanismCapability.CAP_SLURRY, Arrays.stream(outputs).map(ChemicalMekanismCapability.CAP_SLURRY::of).toArray(SlurryStack[]::new));
        }
        return this;
    }

    public RecipeBuilder inputInfusions(Object... inputs) {
        if (Multiblocked.isMekLoaded()) {
            return input(ChemicalMekanismCapability.CAP_INFUSE, Arrays.stream(inputs).map(ChemicalMekanismCapability.CAP_INFUSE::of).toArray(InfusionStack[]::new));
        }
        return this;
    }

    public RecipeBuilder outputInfusions(Object... outputs) {
        if (Multiblocked.isMekLoaded()) {
            return output(ChemicalMekanismCapability.CAP_INFUSE, Arrays.stream(outputs).map(ChemicalMekanismCapability.CAP_INFUSE::of).toArray(InfusionStack[]::new));
        }
        return this;
    }

    public RecipeBuilder inputPigments(Object... inputs) {
        if (Multiblocked.isMekLoaded()) {
            return input(ChemicalMekanismCapability.CAP_PIGMENT, Arrays.stream(inputs).map(ChemicalMekanismCapability.CAP_PIGMENT::of).toArray(PigmentStack[]::new));
        }
        return this;
    }

    public RecipeBuilder outputPigments(Object... outputs) {
        if (Multiblocked.isMekLoaded()) {
            return output(ChemicalMekanismCapability.CAP_PIGMENT, Arrays.stream(outputs).map(ChemicalMekanismCapability.CAP_PIGMENT::of).toArray(PigmentStack[]::new));
        }
        return this;
    }

    public RecipeBuilder inputMana(int mana) {
        if (Multiblocked.isBotLoaded()) {
            return input(ManaBotaniaCapability.CAP, mana);
        }
        return this;
    }

    public RecipeBuilder outputMana(int mana) {
        if (Multiblocked.isBotLoaded()) {
            return output(ManaBotaniaCapability.CAP, mana);
        }
        return this;
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
        return new Recipe(fixedName == null ? UUID.randomUUID().toString() : fixedName, inputBuilder.build(), outputBuilder.build(), tickInputBuilder.build(), tickOutputBuilder.build(), data.isEmpty() ? Recipe.EMPTY : ImmutableMap.copyOf(data), text, duration);
    }

    public void buildAndRegister(){
        recipeMap.addRecipe(build());
    }
}
