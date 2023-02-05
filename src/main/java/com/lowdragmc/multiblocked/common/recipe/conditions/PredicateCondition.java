package com.lowdragmc.multiblocked.common.recipe.conditions;

import com.google.gson.JsonObject;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.RecipeCondition;
import com.lowdragmc.multiblocked.api.recipe.RecipeLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;


/**
 * Used to operate some of the more specific conditions in dynamic recipes,
 * such as the need for a player to be near the machine, etc.
 * It should be used in the dynamic recipe created in {@link com.lowdragmc.multiblocked.api.kubejs.events.SearchRecipeEvent}.
 *
 * @author vfyjxf
 */
public class PredicateCondition extends RecipeCondition {

    /**
     * Because predicate can't be serialized, we need to store them in a map.
     * And we don't need to send predicate to client, so we don't need to sync it.
     */
    public static final Map<ResourceLocation, List<RecipeCondition>> PREDICATE_MAP = new HashMap<>();
    public static final PredicateCondition INSTANCE = new PredicateCondition();
    public static final Component DEFAULT = new TranslatableComponent("multiblocked.gui.condition.predicate.default");
    private Component tooltip;
    private final BiPredicate<Recipe, RecipeLogic> predicate;

    public PredicateCondition() {
        this.tooltip = DEFAULT;
        this.predicate = (recipe, logic) -> true;
    }

    public PredicateCondition(Component tooltip, BiPredicate<Recipe, RecipeLogic> predicate) {
        this.tooltip = tooltip;
        this.predicate = predicate;
    }

    @Override
    public String getType() {
        return "predicate";
    }

    @Override
    public Component getTooltips() {
        return tooltip;
    }

    @Override
    public boolean test(@NotNull Recipe recipe, @NotNull RecipeLogic recipeLogic) {
        return this.predicate.test(recipe, recipeLogic);
    }

    @Override
    public RecipeCondition createTemplate() {
        return new PredicateCondition();
    }

    @NotNull
    @Override
    public JsonObject serialize() {
        JsonObject jsonObject = super.serialize();
        if (tooltip != DEFAULT)
            jsonObject.addProperty("tooltip", Component.Serializer.toJson(tooltip));
        return jsonObject;
    }

    @Override
    public RecipeCondition deserialize(@NotNull JsonObject config) {
        super.deserialize(config);
        if (config.has("tooltip"))
            this.tooltip = Component.Serializer.fromJson(config.get("tooltip"));
        return this;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf) {
        super.toNetwork(buf);
        boolean hasTooltip = tooltip != DEFAULT;
        buf.writeBoolean(hasTooltip);
        if (hasTooltip)
            buf.writeComponent(tooltip);
    }

    @Override
    public RecipeCondition fromNetwork(FriendlyByteBuf buf) {
        super.fromNetwork(buf);
        if (buf.readBoolean())
            this.tooltip = buf.readComponent();
        return this;
    }
}
