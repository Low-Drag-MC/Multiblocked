package com.lowdragmc.multiblocked.api.definition;

import com.google.common.base.Suppliers;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.utils.ShapeUtils;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.json.IMultiblockedRendererTypeAdapterFactory;
import com.lowdragmc.multiblocked.api.sound.SoundState;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/8/8
 * @implNote StatusProperties
 */
public class StatusProperties {
    public final static String UNFORMED = "unformed";
    public final static String IDLE = "idle";
    public final static String WORKING = "working";
    public final static String SUSPEND = "suspend";

    public final static StatusProperties EMPTY = new StatusProperties(UNFORMED);

    public final boolean builtin;
    public String name;
    public StatusProperties parent;
    public Supplier<IMultiblockedRenderer> renderer;
    public Integer lightEmissive;
    public VoxelShape shape;
    public SoundState sound;
    private Map<Direction, VoxelShape> cache;
    public StatusProperties(String name) {
        this(name, null, false);
    }

    public StatusProperties(String name, StatusProperties parent) {
        this(name, parent, false);
    }

    public StatusProperties(String name, StatusProperties parent, boolean builtin) {
        this.name = name;
        this.parent = parent;
        this.builtin = builtin;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setParent(StatusProperties parent) {
        this.parent = parent;
    }

    public StatusProperties getParent() {
        return parent;
    }

    public IMultiblockedRenderer getRenderer() {
        return renderer == null ? parent == null ? null : parent.getRenderer() : renderer.get();
    }

    public int getLightEmissive() {
        return lightEmissive == null ? parent == null ? 0 : parent.getLightEmissive() : lightEmissive;
    }

    public void setLightEmissive(Integer lightEmissive) {
        this.lightEmissive = lightEmissive;
    }

    public VoxelShape getShape() {
        return shape == null ? parent == null ? Shapes.block() : parent.getShape() : shape;
    }

    public VoxelShape getShape(Direction direction) {
        if (this.cache == null) {
            this.cache = new EnumMap<>(Direction.class);
        }
        VoxelShape shape = getShape();
        if (shape.isEmpty() || shape == Shapes.block()) return shape;
        return this.cache.computeIfAbsent(direction, dir -> ShapeUtils.rotate(shape, dir));
    }

    public SoundState getSound() {
        return sound == null ? parent == null ? SoundState.EMPTY : parent.getSound() : sound;
    }

    public void setRenderer(Supplier<IMultiblockedRenderer> renderer) {
        this.renderer = renderer;
    }

    public void setLightEmissive(int lightEmissive) {
        this.lightEmissive = lightEmissive;
    }

    public void setShape(VoxelShape shape) {
        this.shape = shape;
        this.cache = null;
    }

    public void setSound(SoundState sound) {
        this.sound = sound;
    }

    public void setRenderer(IMultiblockedRenderer renderer) {
        this.renderer = () -> renderer;
    }

    // ******* serialize ******* //

    public void fromJson(JsonObject json) {
        if (json.has("renderer")) {
            JsonElement jsonElement = json.get("renderer");
            if (IMultiblockedRendererTypeAdapterFactory.INSTANCE.isPostRenderer(jsonElement)) {
                setRenderer(Suppliers.memoize(() -> Multiblocked.GSON.fromJson(jsonElement, IMultiblockedRenderer.class)));
            } else {
                setRenderer(Multiblocked.GSON.fromJson(jsonElement, IMultiblockedRenderer.class));
            }
        }
        if (json.has("lightEmissive")) {
            lightEmissive = json.get("lightEmissive").getAsInt();
        }
        if (json.has("shape")) {
            shape = Multiblocked.GSON.fromJson(json.get("shape"), VoxelShape.class);
        }
        if (json.has("sound")) {
            sound = Multiblocked.GSON.fromJson(json.get("sound"), SoundState.class);
            if (sound.sound.equals(SoundState.EMPTY.sound)) {
                sound = SoundState.EMPTY;
            } else {
                sound.status = name;
            }
        }
    }

    public JsonObject toJson(JsonObject json) {
        if (renderer != null) {
            json.add("renderer", Multiblocked.GSON.toJsonTree(renderer.get()));
        }
        if (lightEmissive != null) {
            json.addProperty("lightEmissive", lightEmissive);
        }
        if (shape != null) {
            json.add("shape", Multiblocked.GSON.toJsonTree(shape));
        }
        if (parent != null) {
            json.addProperty("parent", parent.name);
        }
        if (sound != null) {
            json.add("sound", Multiblocked.GSON.toJsonTree(sound));
        }
        return json;
    }
}
