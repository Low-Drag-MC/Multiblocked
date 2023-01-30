package com.lowdragmc.multiblocked.api.registry;

import com.google.common.collect.Maps;
import com.lowdragmc.multiblocked.api.recipe.RecipeCondition;
import com.lowdragmc.multiblocked.common.recipe.conditions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author KilaBash
 * @date 2022/05/27
 * @implNote RecipeConditions
 */
public class MbdRecipeConditions {
    public static final Map<String, RecipeCondition> RECIPE_CONDITIONS_REGISTRY = Maps.newHashMap();
    public static final List<String> RECIPE_CONDITIONS_ORDER = new ArrayList<>();

    public static void registerCondition(RecipeCondition condition) {
        RECIPE_CONDITIONS_REGISTRY.put(condition.getType(), condition);
        RECIPE_CONDITIONS_ORDER.add(condition.getType());
    }

    public static RecipeCondition getCondition(String type) {
        return RECIPE_CONDITIONS_REGISTRY.get(type.toLowerCase());
    }

    /**
     * Uses VarInt instead of direct strings in Network to reduce payload.
     */
    public static int getConditionOrder(RecipeCondition type) {
        return RECIPE_CONDITIONS_ORDER.indexOf(type.getType());
    }

    public static int getConditionOrder(String type) {
        return RECIPE_CONDITIONS_ORDER.indexOf(type);
    }

    public static RecipeCondition getConditionByIndex(int order) {
        return RECIPE_CONDITIONS_REGISTRY.get(RECIPE_CONDITIONS_ORDER.get(order));
    }


    public static void registerConditions() {
        registerCondition(DimensionCondition.INSTANCE);
        registerCondition(ThunderCondition.INSTANCE);
        registerCondition(RainingCondition.INSTANCE);
        registerCondition(PositionYCondition.INSTANCE);
        registerCondition(BiomeCondition.INSTANCE);
        registerCondition(BlockCondition.INSTANCE);
        registerCondition(PredicateCondition.INSTANCE);
    }
}
