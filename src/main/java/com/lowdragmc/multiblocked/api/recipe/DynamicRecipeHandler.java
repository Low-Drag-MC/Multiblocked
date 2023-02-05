package com.lowdragmc.multiblocked.api.recipe;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.recipe.ingredient.EntityIngredient;
import com.lowdragmc.multiblocked.common.capability.*;
import com.lowdragmc.multiblocked.common.recipe.conditions.*;
import dev.latvian.mods.kubejs.util.MapJS;
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
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

import static com.lowdragmc.multiblocked.Multiblocked.GSON;

/**
 * Helps modpack authors to dynamically create recipes based on the state of the machine.
 */
@SuppressWarnings("unused")
public class DynamicRecipeHandler {

    public final Map<MultiblockCapability<?>, ImmutableList.Builder<Content>> additionalInputBuilder = new HashMap<>();
    public final Map<MultiblockCapability<?>, ImmutableList.Builder<Content>> additionalTickInputBuilder = new HashMap<>();
    public final Map<MultiblockCapability<?>, ImmutableList.Builder<Content>> additionalOutputBuilder = new HashMap<>();
    public final Map<MultiblockCapability<?>, ImmutableList.Builder<Content>> additionalTickOutputBuilder = new HashMap<>();

    public List<Consumer<Map<String, Pair<MultiblockCapability<?>, List<Content>>>>> inputHandler = new ArrayList<>();
    public List<Consumer<Map<String, Pair<MultiblockCapability<?>, List<Content>>>>> outputHandler = new ArrayList<>();
    public List<Consumer<Map<String, Pair<MultiblockCapability<?>, List<Content>>>>> tickInputHandler = new ArrayList<>();
    public List<Consumer<Map<String, Pair<MultiblockCapability<?>, List<Content>>>>> tickOutputHandler = new ArrayList<>();

    public final List<Pair<MultiblockCapability<?>, Predicate<Content>>> inputRemovers = new ArrayList<>();
    public final List<Pair<MultiblockCapability<?>, Predicate<Content>>> outputRemovers = new ArrayList<>();
    public final List<Pair<MultiblockCapability<?>, Predicate<Content>>> tickInputRemovers = new ArrayList<>();
    public final List<Pair<MultiblockCapability<?>, Predicate<Content>>> tickOutputRemovers = new ArrayList<>();
    public DoubleFunction<Double> chanceHandler;
    BiConsumer<Recipe, List<RecipeCondition>> conditionHandler;
    public Recipe recipe;
    public CompoundTag data;
    public final List<RecipeCondition> conditions = new ArrayList<>();
    protected int duration;
    protected Component text;
    protected boolean perTick;
    protected String slotName;
    protected String uiName;
    protected float chance = 1;

    public static DynamicRecipeHandler create() {
        DynamicRecipeHandler handler = new DynamicRecipeHandler();
        handler.data = new CompoundTag();
        return handler;
    }

    public static DynamicRecipeHandler from(Recipe recipe) {
        DynamicRecipeHandler handler = new DynamicRecipeHandler();
        handler.recipe = recipe;
        handler.conditions.addAll(recipe.conditions);
        handler.data = recipe.data.copy();
        handler.duration = recipe.duration;
        handler.text = recipe.text;
        return handler;
    }

    public DynamicRecipeHandler duration(int duration) {
        this.duration = duration;
        return this;
    }


    public DynamicRecipeHandler text(Component text) {
        this.text = text;
        return this;
    }

    public DynamicRecipeHandler inputHandler(Consumer<Map<String, Pair<MultiblockCapability<?>, List<Content>>>> inputHandler) {
        this.inputHandler.add(inputHandler);
        return this;
    }

    public DynamicRecipeHandler inputMultiplier(boolean perTick, String capability, double multiplier) {
        return addOperator(capability, multiplier, perTick ? tickInputHandler : inputHandler, ContentModifier.multiplier(multiplier));
    }

    public DynamicRecipeHandler inputMultiplier(boolean perTick, String capability, double multiplier, Predicate<Content> predicate) {
        return addOperatorWithPredicate(capability, multiplier, perTick ? tickInputHandler : inputHandler, ContentModifier.multiplier(multiplier), predicate);
    }

    public DynamicRecipeHandler inputAdder(boolean perTick, String capability, double addition) {
        return addOperator(capability, addition, perTick ? tickInputHandler : inputHandler, ContentModifier.addition(addition));
    }

    public DynamicRecipeHandler inputAdder(boolean perTick, String capability, double addition, Predicate<Content> predicate) {
        return addOperatorWithPredicate(capability, addition, perTick ? tickInputHandler : inputHandler, ContentModifier.addition(addition), predicate);
    }

    public DynamicRecipeHandler outputHandler(Consumer<Map<String, Pair<MultiblockCapability<?>, List<Content>>>> outputHandler) {
        this.outputHandler.add(outputHandler);
        return this;
    }

    public DynamicRecipeHandler outputMultiplier(boolean perTick, String capability, double multiplier) {
        return addOperator(capability, multiplier, perTick ? tickOutputHandler : outputHandler, ContentModifier.multiplier(multiplier));
    }

    public DynamicRecipeHandler outputMultiplier(boolean perTick, String capability, double addition, Predicate<Content> predicate) {
        return addOperatorWithPredicate(capability, addition, perTick ? tickOutputHandler : outputHandler, ContentModifier.multiplier(addition), predicate);
    }

    public DynamicRecipeHandler outputAdder(boolean perTick, String capability, double addition, Predicate<Content> predicate) {
        return addOperatorWithPredicate(capability, addition, perTick ? tickOutputHandler : outputHandler, ContentModifier.addition(addition), predicate);
    }

    @NotNull
    private DynamicRecipeHandler addOperator(
            String capability,
            double multiplier,
            List<Consumer<Map<String, Pair<MultiblockCapability<?>, List<Content>>>>> handler,
            ContentModifier modifier
    ) {
        handler.add(map -> {
            Pair<MultiblockCapability<?>, List<Content>> pair = map.get(capability);
            if (pair != null) {
                MultiblockCapability cap = pair.getLeft();
                List<Content> contents = pair.getRight();
                contents.replaceAll(content -> {
                    content.content = cap.copyWithModifier(content.content, modifier);
                    return content;
                });
            }
        });
        return this;
    }

    @NotNull
    private DynamicRecipeHandler addOperatorWithPredicate(
            String capability,
            double multiplier,
            List<Consumer<Map<String, Pair<MultiblockCapability<?>, List<Content>>>>> handler,
            ContentModifier modifier,
            Predicate<Content> predicate
    ) {
        handler.add(map -> {
            Pair<MultiblockCapability<?>, List<Content>> pair = map.get(capability);
            if (pair != null) {
                MultiblockCapability cap = pair.getLeft();
                List<Content> contents = pair.getRight();
                contents.replaceAll(content -> {
                    if (predicate.test(content)) {
                        content.content = cap.copyWithModifier(content.content, modifier);
                    }
                    return content;
                });
            }
        });
        return this;
    }

    public DynamicRecipeHandler tickInputHandler(Consumer<Map<String, Pair<MultiblockCapability<?>, List<Content>>>> tickInputHandler) {
        this.tickInputHandler.add(tickInputHandler);
        return this;
    }

    public DynamicRecipeHandler tickOutputHandler(Consumer<Map<String, Pair<MultiblockCapability<?>, List<Content>>>> tickOutputHandler) {
        this.tickOutputHandler.add(tickOutputHandler);
        return this;
    }

    public DynamicRecipeHandler data(CompoundTag tag) {
        this.data = tag;
        return this;
    }

    public DynamicRecipeHandler chanceHandler(DoubleFunction<Double> chanceHandler) {
        this.chanceHandler = chanceHandler;
        return this;
    }

    public DynamicRecipeHandler conditionHandler(BiConsumer<Recipe, List<RecipeCondition>> conditionHandler) {
        this.conditionHandler = conditionHandler;
        return this;
    }

    public <T> DynamicRecipeHandler input(MultiblockCapability<T> capability, T... obj) {
        (perTick ? additionalTickInputBuilder : additionalInputBuilder).computeIfAbsent(capability, c -> ImmutableList.builder()).addAll(Arrays.stream(obj).map(o -> new Content(o, chance, slotName, uiName)).iterator());
        return this;
    }

    public <T> DynamicRecipeHandler output(MultiblockCapability<T> capability, T... obj) {
        (perTick ? additionalTickOutputBuilder : additionalOutputBuilder).computeIfAbsent(capability, c -> ImmutableList.builder()).addAll(Arrays.stream(obj).map(o -> new Content(o, chance, slotName, uiName)).iterator());
        return this;
    }

    public <T> DynamicRecipeHandler inputs(MultiblockCapability<T> capability, Object... obj) {
        (perTick ? additionalTickInputBuilder : additionalInputBuilder).computeIfAbsent(capability, c -> ImmutableList.builder()).addAll(Arrays.stream(obj)
                .map(capability::of)
                .map(o -> new Content(o, chance, slotName, uiName)).iterator());
        return this;
    }

    public <T> DynamicRecipeHandler outputs(MultiblockCapability<T> capability, Object... obj) {
        (perTick ? additionalTickOutputBuilder : additionalOutputBuilder).computeIfAbsent(capability, c -> ImmutableList.builder()).addAll(Arrays.stream(obj)
                .map(capability::of)
                .map(o -> new Content(o, chance, slotName, uiName)).iterator());
        return this;
    }

    public DynamicRecipeHandler addCondition(RecipeCondition condition) {
        conditions.add(condition);
        return this;
    }

    public DynamicRecipeHandler removeTypedInputs(boolean perTick, MultiblockCapability<?> capability) {
        (perTick ? additionalTickInputBuilder : additionalInputBuilder).remove(capability);
        return this;
    }

    public DynamicRecipeHandler removeTypedInputs(boolean perTick, MultiblockCapability<?> capability, Predicate<Content> predicate) {
        (perTick ? tickInputRemovers : inputRemovers).add(Pair.of(capability, predicate));
        return this;
    }

    public DynamicRecipeHandler removeTypedOutputs(boolean perTick, MultiblockCapability<?> capability) {
        (perTick ? additionalTickOutputBuilder : additionalOutputBuilder).remove(capability);
        return this;
    }

    public DynamicRecipeHandler removeTypedOutputs(boolean perTick, MultiblockCapability<?> capability, Predicate<Content> predicate) {
        (perTick ? tickOutputRemovers : outputRemovers).add(Pair.of(capability, predicate));
        return this;
    }

    public DynamicRecipeHandler removeAllConditions() {
        conditions.clear();
        return this;
    }

    public DynamicRecipeHandler removeTypeCondition(String type) {
        conditions.removeIf(c -> c.getType().equals(type));
        return this;
    }

    public DynamicRecipeHandler inputFE(int forgeEnergy) {
        return input(FEMultiblockCapability.CAP, forgeEnergy);
    }

    public DynamicRecipeHandler outputFE(int forgeEnergy) {
        return output(FEMultiblockCapability.CAP, forgeEnergy);
    }

    public DynamicRecipeHandler inputItems(Ingredient... inputs) {
        return input(ItemMultiblockCapability.CAP, inputs);
    }

    public DynamicRecipeHandler inputItems(ItemStack... inputs) {
        for (ItemStack input : inputs)
            inputItems(Ingredient.of(input));
        return this;
    }

    public DynamicRecipeHandler outputItems(ItemStack... outputs) {
        return output(ItemMultiblockCapability.CAP, Arrays.stream(outputs).map(Ingredient::of).toArray(Ingredient[]::new));
    }

    public DynamicRecipeHandler inputFluids(FluidStack... inputs) {
        return input(FluidMultiblockCapability.CAP, inputs);
    }

    public DynamicRecipeHandler outputFluids(FluidStack... outputs) {
        return output(FluidMultiblockCapability.CAP, outputs);
    }

    public DynamicRecipeHandler inputEntities(EntityIngredient... inputs) {
        return input(EntityMultiblockCapability.CAP, inputs);
    }

    public DynamicRecipeHandler outputEntities(EntityIngredient... outputs) {
        return output(EntityMultiblockCapability.CAP, outputs);
    }

    public DynamicRecipeHandler inputHeat(double heat) {
        if (Multiblocked.isMekLoaded()) {
            return input(HeatMekanismCapability.CAP, heat);
        }
        return this;
    }

    public DynamicRecipeHandler outputHeat(double heat) {
        if (Multiblocked.isMekLoaded()) {
            return output(HeatMekanismCapability.CAP, heat);
        }
        return this;
    }

    public DynamicRecipeHandler inputGases(Object... inputs) {
        if (Multiblocked.isMekLoaded()) {
            return input(ChemicalMekanismCapability.CAP_GAS, Arrays.stream(inputs).map(ChemicalMekanismCapability.CAP_GAS::of).toArray(GasStack[]::new));
        }
        return this;
    }

    public DynamicRecipeHandler inputGas(MapJS input) {
        if (Multiblocked.isMekLoaded()) {
            return input(ChemicalMekanismCapability.CAP_GAS, ChemicalMekanismCapability.CAP_GAS.serializer.fromJson(input.toJson()));
        }
        return this;
    }

    public DynamicRecipeHandler outputGases(Object... outputs) {
        if (Multiblocked.isMekLoaded()) {
            return output(ChemicalMekanismCapability.CAP_GAS, Arrays.stream(outputs).map(ChemicalMekanismCapability.CAP_GAS::of).toArray(GasStack[]::new));
        }
        return this;
    }

    public DynamicRecipeHandler outputGas(MapJS output) {
        if (Multiblocked.isMekLoaded()) {
            return output(ChemicalMekanismCapability.CAP_GAS, ChemicalMekanismCapability.CAP_GAS.serializer.fromJson(output.toJson()));
        }
        return this;
    }

    public DynamicRecipeHandler inputSlurries(Object... inputs) {
        if (Multiblocked.isMekLoaded()) {
            return input(ChemicalMekanismCapability.CAP_SLURRY, Arrays.stream(inputs).map(ChemicalMekanismCapability.CAP_SLURRY::of).toArray(SlurryStack[]::new));
        }
        return this;
    }

    public DynamicRecipeHandler inputSlurry(MapJS input) {
        if (Multiblocked.isMekLoaded()) {
            return input(ChemicalMekanismCapability.CAP_SLURRY, ChemicalMekanismCapability.CAP_SLURRY.serializer.fromJson(input.toJson()));
        }
        return this;
    }

    public DynamicRecipeHandler outputSlurries(Object... outputs) {
        if (Multiblocked.isMekLoaded()) {
            return output(ChemicalMekanismCapability.CAP_SLURRY, Arrays.stream(outputs).map(ChemicalMekanismCapability.CAP_SLURRY::of).toArray(SlurryStack[]::new));
        }
        return this;
    }

    public DynamicRecipeHandler outputSlurry(MapJS output) {
        if (Multiblocked.isMekLoaded()) {
            return output(ChemicalMekanismCapability.CAP_SLURRY, ChemicalMekanismCapability.CAP_SLURRY.serializer.fromJson(output.toJson()));
        }
        return this;
    }

    public DynamicRecipeHandler inputInfusions(Object... inputs) {
        if (Multiblocked.isMekLoaded()) {
            return input(ChemicalMekanismCapability.CAP_INFUSE, Arrays.stream(inputs).map(ChemicalMekanismCapability.CAP_INFUSE::of).toArray(InfusionStack[]::new));
        }
        return this;
    }

    public DynamicRecipeHandler inputInfusion(MapJS input) {
        if (Multiblocked.isMekLoaded()) {
            return input(ChemicalMekanismCapability.CAP_INFUSE, ChemicalMekanismCapability.CAP_INFUSE.serializer.fromJson(input.toJson()));
        }
        return this;
    }

    public DynamicRecipeHandler outputInfusions(Object... outputs) {
        if (Multiblocked.isMekLoaded()) {
            return output(ChemicalMekanismCapability.CAP_INFUSE, Arrays.stream(outputs).map(ChemicalMekanismCapability.CAP_INFUSE::of).toArray(InfusionStack[]::new));
        }
        return this;
    }

    public DynamicRecipeHandler outputInfusion(MapJS output) {
        if (Multiblocked.isMekLoaded()) {
            return output(ChemicalMekanismCapability.CAP_INFUSE, ChemicalMekanismCapability.CAP_INFUSE.serializer.fromJson(output.toJson()));
        }
        return this;
    }

    public DynamicRecipeHandler inputPigments(Object... inputs) {
        if (Multiblocked.isMekLoaded()) {
            return input(ChemicalMekanismCapability.CAP_PIGMENT, Arrays.stream(inputs).map(ChemicalMekanismCapability.CAP_PIGMENT::of).toArray(PigmentStack[]::new));
        }
        return this;
    }

    public DynamicRecipeHandler inputPigment(MapJS input) {
        if (Multiblocked.isMekLoaded()) {
            return input(ChemicalMekanismCapability.CAP_PIGMENT, ChemicalMekanismCapability.CAP_PIGMENT.serializer.fromJson(input.toJson()));
        }
        return this;
    }

    public DynamicRecipeHandler outputPigments(Object... outputs) {
        if (Multiblocked.isMekLoaded()) {
            return output(ChemicalMekanismCapability.CAP_PIGMENT, Arrays.stream(outputs).map(ChemicalMekanismCapability.CAP_PIGMENT::of).toArray(PigmentStack[]::new));
        }
        return this;
    }

    public DynamicRecipeHandler outputPigment(MapJS output) {
        if (Multiblocked.isMekLoaded()) {
            return output(ChemicalMekanismCapability.CAP_PIGMENT, ChemicalMekanismCapability.CAP_PIGMENT.serializer.fromJson(output.toJson()));
        }
        return this;
    }

    public DynamicRecipeHandler inputMana(int mana) {
        if (Multiblocked.isBotLoaded()) {
            return input(ManaBotaniaCapability.CAP, mana);
        }
        return this;
    }

    public DynamicRecipeHandler outputMana(int mana) {
        if (Multiblocked.isBotLoaded()) {
            return output(ManaBotaniaCapability.CAP, mana);
        }
        return this;
    }

    public DynamicRecipeHandler inputCreate(float capacity) {
        if (Multiblocked.isCreateLoaded()) {
            return input(CreateStressCapacityCapability.CAP, capacity);
        }
        return this;
    }

    public DynamicRecipeHandler outputCreate(float capacity) {
        if (Multiblocked.isCreateLoaded()) {
            return output(CreateStressCapacityCapability.CAP, capacity);
        }
        return this;
    }

    public DynamicRecipeHandler inputAura(int aura) {
        if (Multiblocked.isNaturesAuraLoaded()) {
            return input(AuraMultiblockCapability.CAP, aura);
        }
        return this;
    }

    public DynamicRecipeHandler outputAura(int aura) {
        if (Multiblocked.isCreateLoaded()) {
            return output(AuraMultiblockCapability.CAP, aura);
        }
        return this;
    }

    public DynamicRecipeHandler inputEMC(BigInteger emc) {
        if (Multiblocked.isProjectELoaded()) {
            return input(EMCProjectECapability.CAP, emc);
        }
        return this;
    }

    public DynamicRecipeHandler outputEMC(BigInteger emc) {
        if (Multiblocked.isProjectELoaded()) {
            return output(EMCProjectECapability.CAP, emc);
        }
        return this;
    }

    public DynamicRecipeHandler inputPressure(float pressure) {
        if (Multiblocked.isPneumaticLoaded()) {
            return input(PneumaticPressureCapability.CAP, pressure);
        }
        return this;
    }

    public DynamicRecipeHandler outputPressure(float pressure) {
        if (Multiblocked.isPneumaticLoaded()) {
            return output(PneumaticPressureCapability.CAP, pressure);
        }
        return this;
    }

    public DynamicRecipeHandler dimension(ResourceLocation dimension, boolean reverse) {
        return addCondition(new DimensionCondition(dimension).setReverse(reverse));
    }

    public DynamicRecipeHandler removeDimension(ResourceLocation dimension) {
        conditions.removeIf(condition -> condition instanceof DimensionCondition &&
                ((DimensionCondition) condition).getDimension().equals(dimension));
        return this;
    }

    public DynamicRecipeHandler dimension(ResourceLocation dimension) {
        return dimension(dimension, false);
    }

    public DynamicRecipeHandler biome(ResourceLocation biome, boolean reverse) {
        return addCondition(new BiomeCondition(biome).setReverse(reverse));
    }

    public DynamicRecipeHandler removeBiome(ResourceLocation biome) {
        conditions.removeIf(condition -> condition instanceof BiomeCondition &&
                ((BiomeCondition) condition).getBiome().equals(biome));
        return this;
    }

    public DynamicRecipeHandler biome(ResourceLocation biome) {
        return biome(biome, false);
    }

    public DynamicRecipeHandler rain(float level, boolean reverse) {
        return addCondition(new RainingCondition(level).setReverse(reverse));
    }

    public DynamicRecipeHandler removeRain(float level) {
        conditions.removeIf(condition -> condition instanceof RainingCondition &&
                ((RainingCondition) condition).getLevel() == level);
        return this;
    }

    public DynamicRecipeHandler rain(float level) {
        return rain(level, false);
    }

    public DynamicRecipeHandler thunder(float level, boolean reverse) {
        return addCondition(new ThunderCondition(level).setReverse(reverse));
    }

    public DynamicRecipeHandler removeThunder(float level) {
        conditions.removeIf(condition -> condition instanceof ThunderCondition &&
                ((ThunderCondition) condition).getLevel() == level);
        return this;
    }

    public DynamicRecipeHandler thunder(float level) {
        return thunder(level, false);
    }

    public DynamicRecipeHandler posY(int min, int max, boolean reverse) {
        return addCondition(new PositionYCondition(min, max).setReverse(reverse));
    }

    public DynamicRecipeHandler removePosY(int min, int max) {
        conditions.removeIf(condition -> condition instanceof PositionYCondition &&
                ((PositionYCondition) condition).getMin() == min &&
                ((PositionYCondition) condition).getMax() == max);
        return this;
    }

    public DynamicRecipeHandler posY(int min, int max) {
        return posY(min, max, false);
    }

    public DynamicRecipeHandler block(BlockState blockState, int count, boolean reverse) {
        return addCondition(new BlockCondition(blockState, count).setReverse(reverse));
    }

    public DynamicRecipeHandler removeBlock(BlockState blockState, int count) {
        String compare = new BlockCondition(blockState, count).serialize().toString();
        conditions.removeIf(condition -> condition instanceof BlockCondition &&
                compare.equals(condition.serialize().toString()));
        return this;
    }

    public DynamicRecipeHandler removeBlock(BlockState blockState) {
        String compare = GSON.toJsonTree(blockState).toString();
        conditions.removeIf(condition -> condition instanceof BlockCondition &&
                compare.equals(GSON.toJsonTree(((BlockCondition) condition).getBlockState()).toString()));
        return this;
    }

    public DynamicRecipeHandler block(BlockState blockState, int count) {
        return block(blockState, count, false);
    }

    public DynamicRecipeHandler predicate(BiPredicate<Recipe, RecipeLogic> predicate, Component tooltip, boolean reverse) {
        return addCondition(new PredicateCondition(tooltip, predicate).setReverse(reverse));
    }

    public DynamicRecipeHandler predicate(BiPredicate<Recipe, RecipeLogic> predicate, boolean reverse) {
        return predicate(predicate, PredicateCondition.DEFAULT, reverse);
    }

    public DynamicRecipeHandler predicate(BiPredicate<Recipe, RecipeLogic> predicate, Component tooltip) {
        return predicate(predicate, tooltip, false);
    }

    public DynamicRecipeHandler predicate(BiPredicate<Recipe, RecipeLogic> predicate) {
        return predicate(predicate, PredicateCondition.DEFAULT, false);
    }

    public Recipe apply() {

        Map<MultiblockCapability<?>, ImmutableList.Builder<Content>> inputBuilder = new HashMap<>();
        Map<MultiblockCapability<?>, ImmutableList.Builder<Content>> outputBuilder = new HashMap<>();
        Map<MultiblockCapability<?>, ImmutableList.Builder<Content>> tickInputBuilder = new HashMap<>();
        Map<MultiblockCapability<?>, ImmutableList.Builder<Content>> tickOutputBuilder = new HashMap<>();

        if (recipe != null) {
            if (this.inputHandler != null) {
                Map<String, Pair<MultiblockCapability<?>, List<Content>>> modified = recipe.inputs.entrySet()
                        .stream()
                        .collect(Collectors.toMap(entry -> entry.getKey().name, entry -> Pair.of(entry.getKey(), copyContents(entry.getKey(), entry.getValue()))));

                for (Pair<MultiblockCapability<?>, Predicate<Content>> remover : this.inputRemovers) {
                    modified.get(remover.getKey().name)
                            .getRight()
                            .removeIf(content -> remover.getRight().test(content));
                }

                for (Consumer<Map<String, Pair<MultiblockCapability<?>, List<Content>>>> handler : this.inputHandler) {
                    handler.accept(modified);
                }

                modified.forEach((key, value) -> inputBuilder.computeIfAbsent(value.getLeft(), c -> ImmutableList.builder()).addAll(value.getRight()));
            } else {
                recipe.inputs.forEach((key, value) -> inputBuilder.computeIfAbsent(key, c -> ImmutableList.builder()).addAll(value));
            }

            if (this.outputHandler != null) {
                Map<String, Pair<MultiblockCapability<?>, List<Content>>> modified = recipe.outputs.entrySet()
                        .stream()
                        .collect(Collectors.toMap(entry -> entry.getKey().name, entry -> Pair.of(entry.getKey(), copyContents(entry.getKey(), entry.getValue()))));

                for (Pair<MultiblockCapability<?>, Predicate<Content>> remover : this.outputRemovers) {
                    modified.get(remover.getKey().name)
                            .getRight()
                            .removeIf(content -> remover.getRight().test(content));
                }

                for (Consumer<Map<String, Pair<MultiblockCapability<?>, List<Content>>>> handler : this.outputHandler) {
                    handler.accept(modified);
                }

                modified.forEach((key, value) -> outputBuilder.computeIfAbsent(value.getLeft(), c -> ImmutableList.builder()).addAll(value.getRight()));
            } else {
                recipe.outputs.forEach((key, value) -> outputBuilder.computeIfAbsent(key, c -> ImmutableList.builder()).addAll(value));
            }

            if (this.tickInputHandler != null) {
                Map<String, Pair<MultiblockCapability<?>, List<Content>>> modified = recipe.tickInputs.entrySet()
                        .stream()
                        .collect(Collectors.toMap(entry -> entry.getKey().name, entry -> Pair.of(entry.getKey(), copyContents(entry.getKey(), entry.getValue()))));

                for (Pair<MultiblockCapability<?>, Predicate<Content>> remover : this.tickInputRemovers) {
                    modified.get(remover.getKey().name)
                            .getRight()
                            .removeIf(content -> remover.getRight().test(content));
                }

                for (Consumer<Map<String, Pair<MultiblockCapability<?>, List<Content>>>> handler : this.tickInputHandler) {
                    handler.accept(modified);
                }

                modified.forEach((key, value) -> tickInputBuilder.computeIfAbsent(value.getLeft(), c -> ImmutableList.builder()).addAll(value.getRight()));
            } else {
                recipe.tickInputs.forEach((key, value) -> tickInputBuilder.computeIfAbsent(key, c -> ImmutableList.builder()).addAll(value));
            }

            if (this.tickOutputHandler != null) {
                Map<String, Pair<MultiblockCapability<?>, List<Content>>> modified = recipe.tickOutputs.entrySet()
                        .stream()
                        .collect(Collectors.toMap(entry -> entry.getKey().name, entry -> Pair.of(entry.getKey(), copyContents(entry.getKey(), entry.getValue()))));

                for (Pair<MultiblockCapability<?>, Predicate<Content>> remover : this.tickOutputRemovers) {
                    modified.get(remover.getKey().name)
                            .getRight()
                            .removeIf(content -> remover.getRight().test(content));
                }

                for (Consumer<Map<String, Pair<MultiblockCapability<?>, List<Content>>>> handler : this.tickOutputHandler) {
                    handler.accept(modified);
                }
                modified.forEach((key, value) -> tickOutputBuilder.computeIfAbsent(value.getLeft(), c -> ImmutableList.builder()).addAll(value.getRight()));
            } else {
                recipe.tickOutputs.forEach((key, value) -> tickOutputBuilder.computeIfAbsent(key, c -> ImmutableList.builder()).addAll(value));
            }
        }

        this.additionalInputBuilder.forEach((key, value) -> inputBuilder.computeIfAbsent(key, c -> ImmutableList.builder()).addAll(value.build()));
        this.additionalOutputBuilder.forEach((key, value) -> outputBuilder.computeIfAbsent(key, c -> ImmutableList.builder()).addAll(value.build()));
        this.additionalTickInputBuilder.forEach((key, value) -> tickInputBuilder.computeIfAbsent(key, c -> ImmutableList.builder()).addAll(value.build()));
        this.additionalTickOutputBuilder.forEach((key, value) -> tickOutputBuilder.computeIfAbsent(key, c -> ImmutableList.builder()).addAll(value.build()));

        return new Recipe(UUID.randomUUID().toString(),
                ImmutableMap.copyOf(inputBuilder.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().build()))),
                ImmutableMap.copyOf(outputBuilder.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().build()))),
                ImmutableMap.copyOf(tickInputBuilder.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().build()))),
                ImmutableMap.copyOf(tickOutputBuilder.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().build()))),
                ImmutableList.copyOf(conditions),
                data, text, duration);
    }

    private List<Content> copyContents(MultiblockCapability<?> capability, List<Content> contents) {
        List<Content> list = new ArrayList<>();
        for (Content content : contents) {
            list.add(new Content(capability.copyContent(content.content),
                    content.chance,
                    content.slotName,
                    content.uiName));
        }
        return list;
    }

}
