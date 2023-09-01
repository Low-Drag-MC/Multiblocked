package com.lowdragmc.multiblocked.api.recipe;

import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.recipe.DynamicRecipeHandler;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.common.capability.*;
import dev.latvian.mods.kubejs.util.MapJS;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.SlurryStack;

import java.util.Arrays;

public class MekRecipeHelper {

    public static void inputGases(DynamicRecipeHandler handler, Object... inputs) {
        if (Multiblocked.isMekLoaded()) {
            handler.input(ChemicalMekanismCapability.CAP_GAS, Arrays.stream(inputs).map(ChemicalMekanismCapability.CAP_GAS::of).toArray(GasStack[]::new));
        }
    }

    public static void inputGas(DynamicRecipeHandler handler, MapJS input) {
        if (Multiblocked.isMekLoaded()) {
            handler.input(ChemicalMekanismCapability.CAP_GAS, ChemicalMekanismCapability.CAP_GAS.serializer.fromJson(input.toJson()));
        }
    }

    public static void outputGases(DynamicRecipeHandler handler, Object... outputs) {
        if (Multiblocked.isMekLoaded()) {
            handler.output(ChemicalMekanismCapability.CAP_GAS, Arrays.stream(outputs).map(ChemicalMekanismCapability.CAP_GAS::of).toArray(GasStack[]::new));
        }
    }

    public static void outputGas(DynamicRecipeHandler handler, MapJS output) {
        if (Multiblocked.isMekLoaded()) {
            handler.output(ChemicalMekanismCapability.CAP_GAS, ChemicalMekanismCapability.CAP_GAS.serializer.fromJson(output.toJson()));
        }
    }

    public static void inputSlurries(DynamicRecipeHandler handler, Object... inputs) {
        if (Multiblocked.isMekLoaded()) {
            handler.input(ChemicalMekanismCapability.CAP_SLURRY, Arrays.stream(inputs).map(ChemicalMekanismCapability.CAP_SLURRY::of).toArray(SlurryStack[]::new));
        }
    }

    public static void inputSlurry(DynamicRecipeHandler handler, MapJS input) {
        if (Multiblocked.isMekLoaded()) {
            handler.input(ChemicalMekanismCapability.CAP_SLURRY, ChemicalMekanismCapability.CAP_SLURRY.serializer.fromJson(input.toJson()));
        }
    }

    public static void outputSlurries(DynamicRecipeHandler handler, Object... outputs) {
        if (Multiblocked.isMekLoaded()) {
            handler.output(ChemicalMekanismCapability.CAP_SLURRY, Arrays.stream(outputs).map(ChemicalMekanismCapability.CAP_SLURRY::of).toArray(SlurryStack[]::new));
        }
    }

    public static void outputSlurry(DynamicRecipeHandler handler, MapJS output) {
        if (Multiblocked.isMekLoaded()) {
            handler.output(ChemicalMekanismCapability.CAP_SLURRY, ChemicalMekanismCapability.CAP_SLURRY.serializer.fromJson(output.toJson()));
        }
    }

    public static void inputInfusions(DynamicRecipeHandler handler, Object... inputs) {
        if (Multiblocked.isMekLoaded()) {
            handler.input(ChemicalMekanismCapability.CAP_INFUSE, Arrays.stream(inputs).map(ChemicalMekanismCapability.CAP_INFUSE::of).toArray(InfusionStack[]::new));
        }
    }

    public static void inputInfusion(DynamicRecipeHandler handler, MapJS input) {
        if (Multiblocked.isMekLoaded()) {
            handler.input(ChemicalMekanismCapability.CAP_INFUSE, ChemicalMekanismCapability.CAP_INFUSE.serializer.fromJson(input.toJson()));
        }
    }

    public static void outputInfusions(DynamicRecipeHandler handler, Object... outputs) {
        if (Multiblocked.isMekLoaded()) {
            handler.output(ChemicalMekanismCapability.CAP_INFUSE, Arrays.stream(outputs).map(ChemicalMekanismCapability.CAP_INFUSE::of).toArray(InfusionStack[]::new));
        }
    }

    public static void outputInfusion(DynamicRecipeHandler handler, MapJS output) {
        if (Multiblocked.isMekLoaded()) {
            handler.output(ChemicalMekanismCapability.CAP_INFUSE, ChemicalMekanismCapability.CAP_INFUSE.serializer.fromJson(output.toJson()));
        }
    }

    public static void inputPigments(DynamicRecipeHandler handler, Object... inputs) {
        if (Multiblocked.isMekLoaded()) {
            handler.input(ChemicalMekanismCapability.CAP_PIGMENT, Arrays.stream(inputs).map(ChemicalMekanismCapability.CAP_PIGMENT::of).toArray(PigmentStack[]::new));
        }
    }

    public static void inputPigment(DynamicRecipeHandler handler, MapJS input) {
        if (Multiblocked.isMekLoaded()) {
            handler.input(ChemicalMekanismCapability.CAP_PIGMENT, ChemicalMekanismCapability.CAP_PIGMENT.serializer.fromJson(input.toJson()));
        }
    }

    public static void outputPigments(DynamicRecipeHandler handler, Object... outputs) {
        if (Multiblocked.isMekLoaded()) {
            handler.output(ChemicalMekanismCapability.CAP_PIGMENT, Arrays.stream(outputs).map(ChemicalMekanismCapability.CAP_PIGMENT::of).toArray(PigmentStack[]::new));
        }
    }

    public static void outputPigment(DynamicRecipeHandler handler, MapJS output) {
        if (Multiblocked.isMekLoaded()) {
            handler.output(ChemicalMekanismCapability.CAP_PIGMENT, ChemicalMekanismCapability.CAP_PIGMENT.serializer.fromJson(output.toJson()));
        }
    }

    public static void inputGases(RecipeBuilder builder, Object... inputs) {
        if (Multiblocked.isMekLoaded()) {
            builder.input(ChemicalMekanismCapability.CAP_GAS, Arrays.stream(inputs).map(ChemicalMekanismCapability.CAP_GAS::of).toArray(GasStack[]::new));
        }
    }

    public static void outputGases(RecipeBuilder builder, Object... outputs) {
        if (Multiblocked.isMekLoaded()) {
            builder.output(ChemicalMekanismCapability.CAP_GAS, Arrays.stream(outputs).map(ChemicalMekanismCapability.CAP_GAS::of).toArray(GasStack[]::new));
        }
    }

    public static void inputSlurries(RecipeBuilder builder, Object... inputs) {
        if (Multiblocked.isMekLoaded()) {
            builder.input(ChemicalMekanismCapability.CAP_SLURRY, Arrays.stream(inputs).map(ChemicalMekanismCapability.CAP_SLURRY::of).toArray(SlurryStack[]::new));
        }
    }

    public static void outputSlurries(RecipeBuilder builder, Object... outputs) {
        if (Multiblocked.isMekLoaded()) {
            builder.output(ChemicalMekanismCapability.CAP_SLURRY, Arrays.stream(outputs).map(ChemicalMekanismCapability.CAP_SLURRY::of).toArray(SlurryStack[]::new));
        }
    }

    public static void inputInfusions(RecipeBuilder builder, Object... inputs) {
        if (Multiblocked.isMekLoaded()) {
            builder.input(ChemicalMekanismCapability.CAP_INFUSE, Arrays.stream(inputs).map(ChemicalMekanismCapability.CAP_INFUSE::of).toArray(InfusionStack[]::new));
        }
    }

    public static void outputInfusions(RecipeBuilder builder, Object... outputs) {
        if (Multiblocked.isMekLoaded()) {
            builder.output(ChemicalMekanismCapability.CAP_INFUSE, Arrays.stream(outputs).map(ChemicalMekanismCapability.CAP_INFUSE::of).toArray(InfusionStack[]::new));
        }
    }

    public static void inputPigments(RecipeBuilder builder, Object... inputs) {
        if (Multiblocked.isMekLoaded()) {
            builder.input(ChemicalMekanismCapability.CAP_PIGMENT, Arrays.stream(inputs).map(ChemicalMekanismCapability.CAP_PIGMENT::of).toArray(PigmentStack[]::new));
        }
    }

    public static void outputPigments(RecipeBuilder builder, Object... outputs) {
        if (Multiblocked.isMekLoaded()) {
            builder.output(ChemicalMekanismCapability.CAP_PIGMENT, Arrays.stream(outputs).map(ChemicalMekanismCapability.CAP_PIGMENT::of).toArray(PigmentStack[]::new));
        }
    }

}