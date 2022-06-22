package com.lowdragmc.multiblocked.api.recipe.serde.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.multiblocked.api.recipe.Content;
import net.minecraft.network.FriendlyByteBuf;

public interface IContentSerializer<T> {

    void toNetwork(FriendlyByteBuf buf, T content);

    T fromNetwork(FriendlyByteBuf buf);

    T fromJson(JsonElement json);

    JsonElement toJson(T content);

    T of(Object o);


    @SuppressWarnings("unchecked")
    default void toNetworkContent(FriendlyByteBuf buf, Content content) {
        T inner = (T) content.getContent();
        toNetwork(buf, inner);
        buf.writeFloat(content.chance);
        buf.writeUtf(content.slotName);
    }

    default Content fromNetworkContent(FriendlyByteBuf buf) {
        T inner = fromNetwork(buf);
        float chance = buf.readFloat();
        String slotName = buf.readUtf();
        return new Content(inner, chance, slotName);
    }

    @SuppressWarnings("unchecked")
    default JsonElement toJsonContent(Content content) {
        JsonObject json = new JsonObject();
        json.add("content", toJson((T) content.getContent()));
        json.addProperty("chance", content.chance);
        if (content.slotName != null)
            json.addProperty("slotName", content.slotName);
        return json;
    }

    default Content fromJsonContent(JsonElement json) {
        JsonObject jsonObject = json.getAsJsonObject();
        T inner = fromJson(jsonObject.get("content"));
        float chance = jsonObject.has("chance") ? jsonObject.get("chance").getAsFloat() : 1;
        String slotName = jsonObject.has("slotName") ? jsonObject.get("slotName").getAsString() : null;
        return new Content(inner, chance, slotName);
    }
}
