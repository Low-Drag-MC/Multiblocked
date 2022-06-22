package com.lowdragmc.multiblocked.api.recipe.serde.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.recipe.ingredient.SizedIngredient;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public class SerializerIngredient implements IContentSerializer<Ingredient> {

    public static SerializerIngredient INSTANCE = new SerializerIngredient();

    private SerializerIngredient() {}

    @Override
    public void toNetwork(FriendlyByteBuf buf, Ingredient content) {
        content.toNetwork(buf);
    }

    @Override
    public Ingredient fromNetwork(FriendlyByteBuf buf) {
        return Ingredient.fromNetwork(buf);
    }

    @Override
    public Ingredient fromJson(JsonElement json) {
        try {
            return Ingredient.fromJson(json);
        } catch (Exception e) {
            return outdatedDeserialize(json);
        }
    }

    @Override
    public JsonElement toJson(Ingredient content) {
        return content.toJson();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Ingredient of(Object o) {
        if (o instanceof Ingredient ingredient) {
            return ingredient;
        } else if (o instanceof ItemStack itemStack) {
            return Ingredient.of(itemStack);
        } else if (o instanceof ItemLike itemLike) {
            return Ingredient.of(itemLike);
        } else if (o instanceof TagKey tag) {
            return Ingredient.of(tag);
        }
        return Ingredient.EMPTY;
    }

    @Deprecated
    private Ingredient outdatedDeserialize(JsonElement jsonElement) throws JsonParseException {
        try {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (jsonObject.has("tag")) {
                return new SizedIngredient(Ingredient.of(TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation(jsonObject.get("tag").getAsString().toLowerCase()))), jsonObject.get("amount").getAsInt());
            } else {
                return new SizedIngredient(Ingredient.fromJson(jsonObject.get("matches")), jsonObject.get("amount").getAsInt());
            }
        } catch (Exception e){
            Multiblocked.LOGGER.error("cant parse the item ingredient: {}", jsonElement.toString());
            return Ingredient.EMPTY;
        }
    }
}
