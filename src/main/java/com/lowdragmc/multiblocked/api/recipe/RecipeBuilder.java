package com.lowdragmc.multiblocked.api.recipe;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.recipe.ingredient.EntityIngredient;
import com.lowdragmc.multiblocked.common.capability.*;
import com.lowdragmc.multiblocked.common.recipe.conditions.*;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.SlurryStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;

import java.math.BigInteger;
import java.util.*;

@SuppressWarnings("unchecked")
public class RecipeBuilder {

    public final RecipeMap recipeMap;
    public final Map<MultiblockCapability<?>, ImmutableList.Builder<Content>> inputBuilder = new HashMap<>();
    public final Map<MultiblockCapability<?>, ImmutableList.Builder<Content>> tickInputBuilder = new HashMap<>();
    public final Map<MultiblockCapability<?>, ImmutableList.Builder<Content>> outputBuilder = new HashMap<>();
    public final Map<MultiblockCapability<?>, ImmutableList.Builder<Content>> tickOutputBuilder = new HashMap<>();
    public CompoundTag data = new CompoundTag();
    public final List<RecipeCondition> conditions = new ArrayList<>();
    protected int duration;
    protected Component text;
    protected boolean perTick;
    protected String fixedName;
    protected String slotName;
    protected String uiName;
    protected float chance = 1;


    public RecipeBuilder(RecipeMap recipeMap) {
        this.recipeMap = recipeMap;
    }

    public RecipeBuilder copy() {
        RecipeBuilder copy = new RecipeBuilder(recipeMap);
        inputBuilder.forEach((k, v) -> {
            ImmutableList.Builder<Content> builder = ImmutableList.builder();
            copy.inputBuilder.put(k, builder.addAll(v.build()));
        });
        outputBuilder.forEach((k, v) -> {
            ImmutableList.Builder<Content> builder = ImmutableList.builder();
            copy.outputBuilder.put(k, builder.addAll(v.build()));
        });
        tickInputBuilder.forEach((k, v) -> {
            ImmutableList.Builder<Content> builder = ImmutableList.builder();
            copy.tickInputBuilder.put(k, builder.addAll(v.build()));
        });
        tickOutputBuilder.forEach((k, v) -> {
            ImmutableList.Builder<Content> builder = ImmutableList.builder();
            copy.tickOutputBuilder.put(k, builder.addAll(v.build()));
        });
        copy.data.merge(data);
        copy.conditions.addAll(conditions);
        copy.duration = this.duration;
        copy.fixedName = null;
        copy.chance = this.chance;
        copy.perTick = this.perTick;
        return copy;
    }

    public RecipeBuilder duration(int duration) {
        this.duration = duration;
        return this;
    }

    public RecipeBuilder data(CompoundTag tag) {
        this.data = tag;
        return this;
    }


    public RecipeBuilder text(Component text) {
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

    public RecipeBuilder slotName(String slotName) {
        this.slotName = slotName != null && !slotName.isEmpty() ? slotName : null;
        return this;
    }

    public RecipeBuilder uiName(String uiName) {
        this.uiName = uiName != null && !uiName.isEmpty() ? uiName : null;
        return this;
    }

    public <T> RecipeBuilder input(MultiblockCapability<T> capability, T... obj) {
        (perTick ? tickInputBuilder : inputBuilder).computeIfAbsent(capability, c -> ImmutableList.builder()).addAll(Arrays.stream(obj).map(o -> new Content(o, chance, slotName, uiName)).iterator());
        return this;
    }

    public <T> RecipeBuilder output(MultiblockCapability<T> capability, T... obj) {
        (perTick ? tickOutputBuilder : outputBuilder).computeIfAbsent(capability, c -> ImmutableList.builder()).addAll(Arrays.stream(obj).map(o -> new Content(o, chance, slotName, uiName)).iterator());
        return this;
    }

    public <T> RecipeBuilder inputs(MultiblockCapability<T> capability, Object... obj) {
        (perTick ? tickInputBuilder : inputBuilder).computeIfAbsent(capability, c -> ImmutableList.builder()).addAll(Arrays.stream(obj)
                .map(capability::of)
                .map(o -> new Content(o, chance, slotName, uiName)).iterator());
        return this;
    }

    public <T> RecipeBuilder outputs(MultiblockCapability<T> capability, Object... obj) {
        (perTick ? tickOutputBuilder : outputBuilder).computeIfAbsent(capability, c -> ImmutableList.builder()).addAll(Arrays.stream(obj)
                .map(capability::of)
                .map(o -> new Content(o, chance, slotName, uiName)).iterator());
        return this;
    }

    public RecipeBuilder addCondition(RecipeCondition condition) {
        conditions.add(condition);
        return this;
    }

    public RecipeBuilder inputFE(int forgeEnergy) {
        return input(FEMultiblockCapability.CAP, forgeEnergy);
    }

    public RecipeBuilder outputFE(int forgeEnergy) {
        return output(FEMultiblockCapability.CAP, forgeEnergy);
    }

    public RecipeBuilder inputItems(Ingredient... inputs) {
        return input(ItemMultiblockCapability.CAP, inputs);
    }

    public RecipeBuilder outputItems(ItemStack... outputs) {
        return output(ItemMultiblockCapability.CAP, Arrays.stream(outputs).map(Ingredient::of).toArray(Ingredient[]::new));
    }

    public RecipeBuilder inputFluids(FluidStack... inputs) {
        return input(FluidMultiblockCapability.CAP, inputs);
    }

    public RecipeBuilder outputFluids(FluidStack... outputs) {
        return output(FluidMultiblockCapability.CAP, outputs);
    }

    public RecipeBuilder inputEntities(EntityIngredient... inputs) {
        return input(EntityMultiblockCapability.CAP, inputs);
    }

    public RecipeBuilder outputEntities(EntityIngredient... outputs) {
        return output(EntityMultiblockCapability.CAP, outputs);
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

    public RecipeBuilder inputCreate(float capacity) {
        if (Multiblocked.isCreateLoaded()) {
            return input(CreateStressCapacityCapability.CAP, capacity);
        }
        return this;
    }

    public RecipeBuilder outputCreate(float capacity) {
        if (Multiblocked.isCreateLoaded()) {
            return output(CreateStressCapacityCapability.CAP, capacity);
        }
        return this;
    }

    public RecipeBuilder inputAura(int aura) {
        if (Multiblocked.isNaturesAuraLoaded()) {
            return input(AuraMultiblockCapability.CAP, aura);
        }
        return this;
    }

    public RecipeBuilder outputAura(int aura) {
        if (Multiblocked.isCreateLoaded()) {
            return output(AuraMultiblockCapability.CAP, aura);
        }
        return this;
    }

    public RecipeBuilder inputEMC(BigInteger emc) {
        if (Multiblocked.isProjectELoaded()) {
            return input(EMCProjectECapability.CAP, emc);
        }
        return this;
    }

    public RecipeBuilder outputEMC(BigInteger emc) {
        if (Multiblocked.isProjectELoaded()) {
            return output(EMCProjectECapability.CAP, emc);
        }
        return this;
    }

    public RecipeBuilder inputPressure(float pressure) {
        if (Multiblocked.isPneumaticLoaded()) {
            return input(PneumaticPressureCapability.CAP, pressure);
        }
        return this;
    }

    public RecipeBuilder outputPressure(float pressure) {
        if (Multiblocked.isPneumaticLoaded()) {
            return output(PneumaticPressureCapability.CAP, pressure);
        }
        return this;
    }

    // conditions
    public RecipeBuilder dimension(ResourceLocation dimension, boolean reverse) {
        return addCondition(new DimensionCondition(dimension).setReverse(reverse));
    }

    public RecipeBuilder dimension(ResourceLocation dimension) {
        return dimension(dimension, false);
    }

    public RecipeBuilder biome(ResourceLocation biome, boolean reverse) {
        return addCondition(new BiomeCondition(biome).setReverse(reverse));
    }

    public RecipeBuilder biome(ResourceLocation biome) {
        return biome(biome, false);
    }

    public RecipeBuilder rain(float level, boolean reverse) {
        return addCondition(new RainingCondition(level).setReverse(reverse));
    }

    public RecipeBuilder rain(float level) {
        return rain(level, false);
    }

    public RecipeBuilder thunder(float level, boolean reverse) {
        return addCondition(new ThunderCondition(level).setReverse(reverse));
    }

    public RecipeBuilder thunder(float level) {
        return thunder(level, false);
    }

    public RecipeBuilder posY(int min, int max, boolean reverse) {
        return addCondition(new PositionYCondition(min, max).setReverse(reverse));
    }

    public RecipeBuilder posY(int min, int max) {
        return posY(min, max, false);
    }

    public RecipeBuilder block(BlockState blockState, int count, boolean reverse) {
        return addCondition(new BlockCondition(blockState, count).setReverse(reverse));
    }

    public RecipeBuilder block(BlockState blockState, int count) {
        return block(blockState, count, false);
    }

    public Recipe build() {
        ImmutableMap.Builder<MultiblockCapability<?>, ImmutableList<Content>> inputBuilder = new ImmutableMap.Builder<>();
        for (Map.Entry<MultiblockCapability<?>, ImmutableList.Builder<Content>> entry : this.inputBuilder.entrySet()) {
            inputBuilder.put(entry.getKey(), entry.getValue().build());
        }
        ImmutableMap.Builder<MultiblockCapability<?>, ImmutableList<Content>> outputBuilder = new ImmutableMap.Builder<>();
        for (Map.Entry<MultiblockCapability<?>, ImmutableList.Builder<Content>> entry : this.outputBuilder.entrySet()) {
            outputBuilder.put(entry.getKey(), entry.getValue().build());
        }
        ImmutableMap.Builder<MultiblockCapability<?>, ImmutableList<Content>> tickInputBuilder = new ImmutableMap.Builder<>();
        for (Map.Entry<MultiblockCapability<?>, ImmutableList.Builder<Content>> entry : this.tickInputBuilder.entrySet()) {
            tickInputBuilder.put(entry.getKey(), entry.getValue().build());
        }
        ImmutableMap.Builder<MultiblockCapability<?>, ImmutableList<Content>> tickOutputBuilder = new ImmutableMap.Builder<>();
        for (Map.Entry<MultiblockCapability<?>, ImmutableList.Builder<Content>> entry : this.tickOutputBuilder.entrySet()) {
            tickOutputBuilder.put(entry.getKey(), entry.getValue().build());
        }
        return new Recipe(fixedName == null ? UUID.randomUUID().toString() : fixedName, inputBuilder.build(), outputBuilder.build(), tickInputBuilder.build(), tickOutputBuilder.build(), ImmutableList.copyOf(conditions), data, text, duration);
    }

    public void buildAndRegister() {
        buildAndRegister(false);
    }

    public void buildAndRegister(boolean isFuel) {
        if (isFuel) {
            recipeMap.addFuelRecipe(build());
        } else {
            recipeMap.addRecipe(build());
        }
    }
}
