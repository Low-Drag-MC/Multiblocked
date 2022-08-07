package com.lowdragmc.multiblocked.api.json;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;

public class VoxelShapAdapterFactory implements TypeAdapterFactory {
    public static final VoxelShapAdapterFactory INSTANCE = new VoxelShapAdapterFactory();

    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!VoxelShape.class.isAssignableFrom(type.getRawType())) return null;
        return (TypeAdapter<T>) new VoxelShapeTypeAdapter(gson);
    }

    private static final class VoxelShapeTypeAdapter extends TypeAdapter<VoxelShape> {

        private final Gson gson;

        private VoxelShapeTypeAdapter(final Gson gson) {
            this.gson = gson;
        }

        @Override
        public void write(final JsonWriter out, final VoxelShape value) {
            final JsonArray jsonArray = new JsonArray();
            for (AxisAlignedBB aabb : value.toAabbs()) {
                JsonArray aaBB = new JsonArray();
                aaBB.add(aabb.minX);
                aaBB.add(aabb.minY);
                aaBB.add(aabb.minZ);
                aaBB.add(aabb.maxX);
                aaBB.add(aabb.maxY);
                aaBB.add(aabb.maxZ);
                jsonArray.add(aaBB);
            }
            gson.toJson(jsonArray, out);
        }

        @Override
        public VoxelShape read(final JsonReader in) {
            final JsonElement jsonElement = gson.fromJson(in, JsonElement.class);
            if (jsonElement.isJsonArray() && jsonElement.getAsJsonArray().size() > 0) {
                JsonArray jsonArray = jsonElement.getAsJsonArray();
                VoxelShape voxelShape = null;
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonArray array = jsonArray.get(i).getAsJsonArray();
                    VoxelShape shape = VoxelShapes.box(
                            array.get(0).getAsFloat(),
                            array.get(1).getAsFloat(),
                            array.get(2).getAsFloat(),
                            array.get(3).getAsFloat(),
                            array.get(4).getAsFloat(),
                            array.get(5).getAsFloat()
                    );
                    voxelShape = voxelShape == null ? shape : VoxelShapes.or(voxelShape, shape);
                }
                return voxelShape;
            }
            return VoxelShapes.block();
        }
    }
}
