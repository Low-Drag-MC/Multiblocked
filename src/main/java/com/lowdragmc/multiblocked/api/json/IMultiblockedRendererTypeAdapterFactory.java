package com.lowdragmc.multiblocked.api.json;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.lowdragmc.multiblocked.api.registry.MbdRenderers;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import net.minecraft.util.JSONUtils;

public class IMultiblockedRendererTypeAdapterFactory implements TypeAdapterFactory {
    public static final IMultiblockedRendererTypeAdapterFactory INSTANCE = new IMultiblockedRendererTypeAdapterFactory();

    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (IMultiblockedRenderer.class.isAssignableFrom(type.getRawType())) {
            return (TypeAdapter<T>) new IMultiblockedRendererTypeAdapter(gson);
        }
        return null;
    }

    private static final class IMultiblockedRendererTypeAdapter extends TypeAdapter<IMultiblockedRenderer> {

        private final Gson gson;

        private IMultiblockedRendererTypeAdapter(final Gson gson) {
            this.gson = gson;
        }

        @Override
        public void write(final JsonWriter out, final IMultiblockedRenderer value) {
            if (value != null) {
                JsonObject jsonObject = value.toJson(gson, new JsonObject());
                jsonObject.addProperty("type", value.getType());
                jsonObject.addProperty("postRenderer", value.isPostRenderer());
                gson.toJson(jsonObject, out);
            } else {
                gson.toJson(JsonNull.INSTANCE, out);
            }
        }

        @Override
        public IMultiblockedRenderer read(final JsonReader in) {
            final JsonElement jsonElement = gson.fromJson(in, JsonElement.class);
            if (jsonElement.isJsonNull()) return null;
            JsonObject jsonObj = jsonElement.getAsJsonObject();
            final String type = jsonObj.get("type").getAsString();
            IMultiblockedRenderer renderer = MbdRenderers.getRenderer(type);
            if (renderer != null) {
                return renderer.fromJson(gson, jsonObj);
            }
            return null;
        }

    }

    /**
     * check the renderer need to be loaded.
     */
    public boolean isPostRenderer(JsonElement jsonElement) {
        if (jsonElement.isJsonObject()) {
            if (jsonElement.getAsJsonObject().has("postRenderer")) {
                return JSONUtils.getAsBoolean(jsonElement.getAsJsonObject(), "postRenderer", false);
            }
            final String type = JSONUtils.getAsString(jsonElement.getAsJsonObject(), "type", "");
            if (type.equals("blockstate")) { // legacy
                return true;
            }
        }
        return false;
    }
}
