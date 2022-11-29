package com.lowdragmc.multiblocked.api.recipe;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.utils.FileUtility;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.ICapabilityProxyHolder;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.io.File;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 */
public class RecipeMap {
    public static final RecipeMap EMPTY = new RecipeMap("empty");
    public static final Map<String, RecipeMap> RECIPE_MAP_REGISTRY = new HashMap<>();
    public String name;
    public List<Recipe> fuelRecipes;
    public Set<MultiblockCapability<?>> inputCapabilities = new ObjectOpenHashSet<>();
    public Set<MultiblockCapability<?>> outputCapabilities = new ObjectOpenHashSet<>();
    public RecipeBuilder recipeBuilder = new RecipeBuilder(this);
    public ResourceTexture progressTexture = new ResourceTexture("multiblocked:textures/gui/progress_bar_arrow.png");
    public ResourceTexture fuelTexture = new ResourceTexture("multiblocked:textures/gui/progress_bar_fuel.png");
    public int fuelThreshold = 100;
    public IGuiTexture categoryTexture;
    
    static {
        register(EMPTY);
    }

    public HashMap<String, Recipe> recipes = new HashMap<>();

    public RecipeMap(String name) {
        this.name = name;
    }

    public RecipeMap copy() {
        RecipeMap copy = new RecipeMap(name);
        copy.inputCapabilities.addAll(inputCapabilities);
        copy.outputCapabilities.addAll(outputCapabilities);
        copy.progressTexture = progressTexture;
        copy.fuelTexture = fuelTexture;
        copy.fuelThreshold = fuelThreshold;
        copy.fuelRecipes = fuelRecipes == null ? null : new ArrayList<>(fuelRecipes);
        copy.categoryTexture = categoryTexture;
        copy.recipes.putAll(recipes);
        return copy;
    }

    public static void register(RecipeMap recipeMap) {
        RECIPE_MAP_REGISTRY.put(recipeMap.name, recipeMap);
    }

    public static void registerRecipeFromFile(Gson gson, File location) {
        for (File file : Optional.ofNullable(location.listFiles((f, n) -> n.endsWith(".json"))).orElse(new File[0])) {
            try {
                JsonObject config = (JsonObject) FileUtility.loadJson(file);
                RecipeMap recipeMap = gson.fromJson(config, RecipeMap.class);
                if (recipeMap != null && !recipeMap.name.equals("empty")) {
                    register(recipeMap);
                }
            } catch (Exception e) {
                Multiblocked.LOGGER.error("error while loading the recipe map file {}", file.toString());
            }
        }
    }

    public String getUnlocalizedName() {
        return Multiblocked.MODID + ".recupe_map." + name;
    }

    public RecipeBuilder start() {
        return recipeBuilder.copy();
    }

    public boolean hasCapability(IO io, MultiblockCapability<?> capability) {
        switch (io) {
            case IN: return inputCapabilities.contains(capability);
            case OUT: return outputCapabilities.contains(capability);
            case BOTH: return inputCapabilities.contains(capability) && outputCapabilities.contains(capability);
        }
        return false;
    }

    public void addRecipe(Recipe recipe) {
        recipes.put(recipe.uid, recipe);
        inputCapabilities.addAll(recipe.inputs.keySet());
        inputCapabilities.addAll(recipe.tickInputs.keySet());
        outputCapabilities.addAll(recipe.outputs.keySet());
        outputCapabilities.addAll(recipe.tickOutputs.keySet());
    }

    public void addFuelRecipe(Recipe recipe) {
        if (fuelRecipes == null) {
            fuelRecipes = new ArrayList<>();
        }
        fuelRecipes.add(recipe);
        inputCapabilities.addAll(recipe.inputs.keySet());
    }

    public List<Recipe> searchRecipe(ICapabilityProxyHolder holder) {
        if (!holder.hasProxies()) return Collections.emptyList();
        List<Recipe> matches = new ArrayList<>();
        for (Recipe recipe : recipes.values()) {
            if (recipe.matchRecipe(holder) && recipe.matchTickRecipe(holder)) {
                matches.add(recipe);
            }
        }
        return matches;
    }

    public boolean isFuelRecipeMap() {
        return fuelRecipes != null && !fuelRecipes.isEmpty();
    }


    public List<Recipe> searchFuelRecipe(ICapabilityProxyHolder holder) {
        if (!holder.hasProxies() || !isFuelRecipeMap()) return Collections.emptyList();
        List<Recipe> matches = new ArrayList<>();
        for (Recipe recipe : fuelRecipes) {
            if (recipe.matchRecipe(holder) && recipe.matchTickRecipe(holder)) {
                matches.add(recipe);
            }
        }
        return matches;
    }

    public Recipe getRecipe(String uid) {
        return recipes.get(uid);
    }

    public List<Recipe> allRecipes() {
        return new ArrayList<>(recipes.values());
    }

}
