package com.lowdragmc.multiblocked.api.recipe.serde.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.network.FriendlyByteBuf;

public class SerializerInteger implements IContentSerializer<Integer> {
    @Override
    public void toNetwork(FriendlyByteBuf buf, Integer content) {
        buf.writeInt(content);
    }

    @Override
    public Integer fromNetwork(FriendlyByteBuf buf) {
        return buf.readInt();
    }

    @Override
    public Integer fromJson(JsonElement json) {
        return json.getAsInt();
    }

    @Override
    public JsonElement toJson(Integer content) {
        return new JsonPrimitive(content);
    }
}
