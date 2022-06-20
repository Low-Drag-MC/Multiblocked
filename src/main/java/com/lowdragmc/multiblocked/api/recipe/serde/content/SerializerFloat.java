package com.lowdragmc.multiblocked.api.recipe.serde.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.network.FriendlyByteBuf;

public class SerializerFloat implements IContentSerializer<Float> {

    @Override
    public void toNetwork(FriendlyByteBuf buf, Float content) {
        buf.writeFloat(content);
    }

    @Override
    public Float fromNetwork(FriendlyByteBuf buf) {
        return buf.readFloat();
    }

    @Override
    public Float fromJson(JsonElement json) {
        return json.getAsFloat();
    }

    @Override
    public JsonElement toJson(Float content) {
        return new JsonPrimitive(content);
    }
}
