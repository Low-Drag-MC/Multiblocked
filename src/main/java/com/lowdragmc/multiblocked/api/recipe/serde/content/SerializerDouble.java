package com.lowdragmc.multiblocked.api.recipe.serde.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.network.FriendlyByteBuf;

public class SerializerDouble implements IContentSerializer<Double> {
    @Override
    public void toNetwork(FriendlyByteBuf buf, Double content) {
        buf.writeDouble(content);
    }

    @Override
    public Double fromNetwork(FriendlyByteBuf buf) {
        return buf.readDouble();
    }

    @Override
    public Double fromJson(JsonElement json) {
        return json.getAsDouble();
    }

    @Override
    public JsonElement toJson(Double content) {
        return new JsonPrimitive(content);
    }
}
