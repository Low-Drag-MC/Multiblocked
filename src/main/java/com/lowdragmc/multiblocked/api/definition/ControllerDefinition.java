package com.lowdragmc.multiblocked.api.definition;


import com.google.common.base.Suppliers;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.utils.JsonUtil;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.pattern.BlockPattern;
import com.lowdragmc.multiblocked.api.pattern.JsonBlockPattern;
import com.lowdragmc.multiblocked.api.pattern.MultiblockShapeInfo;
import com.lowdragmc.multiblocked.api.recipe.RecipeMap;
import com.lowdragmc.multiblocked.api.tile.ControllerTileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Definition of a controller, which define its structure, logic, recipe chain and so on.
 */
public class ControllerDefinition extends ComponentDefinition {
    protected Supplier<BlockPattern> basePattern;
    protected Supplier<RecipeMap> recipeMap;
    protected Supplier<ItemStack> catalyst; // if null, checking pattern per second
    public CatalystState consumeCatalyst;
    public boolean noNeedController;
    public List<MultiblockShapeInfo> designs; // TODO

    // used for Gson
    public ControllerDefinition() {
        this(null);
    }

    public ControllerDefinition(ResourceLocation location) {
        this(location, ControllerTileEntity::new);
    }

    public ControllerDefinition(ResourceLocation location, Function<ControllerDefinition, TileEntity> teSupplier) {
        super(location, d -> teSupplier.apply((ControllerDefinition) d));
        this.consumeCatalyst = CatalystState.NOT_CONSUMED;
        this.recipeMap = () -> RecipeMap.EMPTY;
    }

    public List<MultiblockShapeInfo> getDesigns() {
        if (designs != null) return designs;
        // auto gen
        if (getBasePattern() != null) {
            return autoGenDFS(getBasePattern(), new ArrayList<>(), new Stack<>());
        }
        return Collections.emptyList();
    }

    private List<MultiblockShapeInfo> autoGenDFS(BlockPattern structurePattern, List<MultiblockShapeInfo> pages, Stack<Integer> repetitionStack) {
        int[][] aisleRepetitions = structurePattern.aisleRepetitions;
        if (repetitionStack.size() == aisleRepetitions.length) {
            int[] repetition = new int[repetitionStack.size()];
            for (int i = 0; i < repetitionStack.size(); i++) {
                repetition[i] = repetitionStack.get(i);
            }
            pages.add(new MultiblockShapeInfo(structurePattern.getPreview(repetition)));
        } else {
            for (int i = aisleRepetitions[repetitionStack.size()][0]; i <= aisleRepetitions[repetitionStack.size()][1]; i++) {
                repetitionStack.push(i);
                autoGenDFS(structurePattern, pages, repetitionStack);
                repetitionStack.pop();
            }
        }
        return pages;
    }

    public String getDescription() {
        return location.getNamespace() + "." + location.getPath() + ".description";
    }

    @Override
    public boolean needUpdateTick() {
        return super.needUpdateTick() || getCatalyst() == null;
    }

    public BlockPattern getBasePattern() {
        return basePattern == null ? null : basePattern.get();
    }

    public RecipeMap getRecipeMap() {
        return recipeMap == null ? null : recipeMap.get();
    }

    public ItemStack getCatalyst() {
        return catalyst == null ? null : catalyst.get();
    }

    public void setBasePattern(BlockPattern basePattern) {
        this.basePattern = () -> basePattern;
    }

    public void setBasePattern(Supplier<BlockPattern> basePattern) {
        this.basePattern = basePattern;
    }

    public void setRecipeMap(RecipeMap recipeMap) {
        this.recipeMap = () -> recipeMap;
    }

    public void setRecipeMap(Supplier<RecipeMap> recipeMap) {
        this.recipeMap = recipeMap;
    }

    public void setDesigns(List<MultiblockShapeInfo> designs) {
        this.designs = designs;
    }

    public void setCatalyst(Supplier<ItemStack> catalyst) {
        this.catalyst = catalyst;
    }

    public void setCatalyst(ItemStack catalyst) {
        this.catalyst = () -> catalyst;
    }

    @Override
    public void fromJson(JsonObject json) {
        super.fromJson(json);
        int version = JSONUtils.getAsInt(json, "version", 0);

        if (json.has("basePattern")) {
            basePattern = Suppliers.memoize(()-> Multiblocked.GSON.fromJson(json.get("basePattern"), JsonBlockPattern.class).build());
        }
        if (json.has("recipeMap")) {
            recipeMap = Suppliers.memoize(()-> RecipeMap.RECIPE_MAP_REGISTRY.getOrDefault(json.get("recipeMap").getAsString(), RecipeMap.EMPTY));
        } else {
            setRecipeMap(RecipeMap.EMPTY);
        }
        if (json.has("catalyst")) {
            catalyst = Suppliers.memoize(()-> Multiblocked.GSON.fromJson(json.get("catalyst"), ItemStack.class));
            if (version > 1) {
                consumeCatalyst = JsonUtil.getEnumOr(json, "consumeCatalyst", CatalystState.class, consumeCatalyst);
            } else {
                consumeCatalyst = JSONUtils.getAsBoolean(json, "consumeCatalyst", false) ? CatalystState.CONSUMED : CatalystState.NOT_CONSUMED;
            }
            noNeedController = JSONUtils.getAsBoolean(json, "noNeedController", noNeedController);
        }
    }

    @Override
    public JsonObject toJson(JsonObject json) {
        json = super.toJson(json);
        if (getRecipeMap() != null) {
            json.addProperty("recipeMap", getRecipeMap().name);
        }
        if (getCatalyst() != null) {
            json.add("catalyst", Multiblocked.GSON.toJsonTree(getCatalyst()));
            json.addProperty("consumeCatalyst", consumeCatalyst.name());
            json.addProperty("noNeedController", noNeedController);
        }
        return json;
    }

    public enum CatalystState implements Predicate<ItemStack> {
        NOT_CONSUMED(itemStack -> true),
        CONSUMED(itemStack -> {
            if (itemStack.getCount() > 0) {
                itemStack.shrink(1);
                return true;
            }
            return false;
        }),
        CONSUME_DURABILITY(itemStack -> {
            if (itemStack.isDamageableItem() && itemStack.getDamageValue() < itemStack.getMaxDamage()) {
                itemStack.setDamageValue(itemStack.getDamageValue() + 1);
                return true;
            }
            return itemStack.getDamageValue() < itemStack.getMaxDamage();
        });

        final Function<ItemStack, Boolean> predicate;

        CatalystState(Function<ItemStack, Boolean> predicate){
            this.predicate = predicate;
        }

        @Override
        public boolean test(ItemStack itemStack) {
            return predicate.apply(itemStack);
        }
    }
}
