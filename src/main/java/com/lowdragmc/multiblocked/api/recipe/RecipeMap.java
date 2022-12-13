package com.lowdragmc.multiblocked.api.recipe;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.FileUtility;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.ICapabilityProxyHolder;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

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
    public IGuiTexture categoryTexture;
    public String uiLocation = "";
    private CompoundTag ui;
    
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
        copy.fuelRecipes = fuelRecipes == null ? null : new ArrayList<>(fuelRecipes);
        copy.categoryTexture = categoryTexture;
        copy.recipes.putAll(recipes);
        copy.uiLocation = uiLocation;
        copy.ui = ui;
        return copy;
    }

    @Nullable
    public WidgetGroup createLDLibUI(@Nullable Recipe recipe) {
        if (ui == null) {
            if (uiLocation == null || uiLocation.isEmpty()) return null;
            File file = new File(Multiblocked.location, uiLocation);
            if (file.isFile()) {
                try {
                    this.ui = NbtIo.read(file).getCompound("root");
                } catch (IOException ignored) {}
            }
        }
        if (this.ui != null) {
            WidgetGroup root = new WidgetGroup();
            root.deserializeNBT(ui);
            root.setSelfPosition(new Position(0, 0));
            root.setClientSideWidget();
            if (recipe != null) {
                handleUI(recipe, root);
            }
            return root;
        }
        return null;
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

    public void handleUI(Recipe recipe, WidgetGroup recipeWidget) {
        // recipe progress
        for (Widget widget : recipeWidget.getWidgetsById(Pattern.compile("^recipe_progress$"))) {
            if (widget instanceof ProgressWidget progressWidget) {
                progressWidget.setProgressSupplier(ProgressWidget.JEIProgress);
            }
        }
        // fuel progress
        for (Widget widget : recipeWidget.getWidgetsById(Pattern.compile("^recipe_fuel_progress$"))) {
            if (widget instanceof ProgressWidget progressWidget) {
                progressWidget.setProgressSupplier(ProgressWidget.JEIProgress);
            }
        }
        // duration
        for (Widget widget : recipeWidget.getWidgetsById(Pattern.compile("^recipe_duration$"))) {
            if (widget instanceof LabelWidget labelWidget) {
                labelWidget.setText((recipe.duration / 20.) + "s");
            } else if (widget instanceof ImageWidget imageWidget && imageWidget.getImage() instanceof TextTexture texture) {
                texture.updateText((recipe.duration / 20.) + "s");
            }
        }
        // custom data
        for (Widget widget : recipeWidget.getWidgetsById(Pattern.compile("^recipe_data$"))) {
            if (widget instanceof LabelWidget labelWidget) {
                labelWidget.setText(recipe.text.getString());
            } else if (widget instanceof ImageWidget imageWidget && imageWidget.getImage() instanceof TextTexture texture) {
                texture.updateText(recipe.text.getString());
            }
        }

        handleIngredientsUI(recipeWidget, recipe.inputs, IngredientIO.INPUT);
        handleIngredientsUI(recipeWidget, recipe.tickInputs, IngredientIO.INPUT);
        handleIngredientsUI(recipeWidget, recipe.outputs, IngredientIO.OUTPUT);
        handleIngredientsUI(recipeWidget, recipe.tickOutputs, IngredientIO.OUTPUT);

//        for (Recipe fuelRecipe : this.recipes.values()) {
//            handleIngredientsUI(recipeWidget, fuelRecipe.inputs, IngredientIO.INPUT);
//        }
        
    }

    private static void handleIngredientsUI(WidgetGroup recipeWidget, ImmutableMap<MultiblockCapability<?>, ImmutableList<Content>> ingredients, IngredientIO ingredientIO) {
        for (Map.Entry<MultiblockCapability<?>, ImmutableList<Content>> entry : ingredients.entrySet()) {
            MultiblockCapability<?> capability = entry.getKey();
            for (Content in : entry.getValue()) {
                if (in.uiName != null) {
                    for (Widget widget : recipeWidget.getWidgetsById(Pattern.compile("^%s$".formatted(in.uiName)))) {
                        capability.handleRecipeUI(widget, in, ingredientIO);
                    }
                }
            }
        }
    }
}
