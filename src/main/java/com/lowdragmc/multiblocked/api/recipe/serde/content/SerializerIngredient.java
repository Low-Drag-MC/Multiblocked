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
import net.minecraft.world.item.crafting.Ingredient;

public class SerializerIngredient implements IContentSerializer<Ingredient> {
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
