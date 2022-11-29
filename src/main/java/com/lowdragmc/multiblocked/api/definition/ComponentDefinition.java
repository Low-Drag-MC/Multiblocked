package com.lowdragmc.multiblocked.api.definition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.block.CustomProperties;
import com.lowdragmc.multiblocked.api.json.IMultiblockedRendererTypeAdapterFactory;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import com.mojang.realmsclient.util.JsonUtils;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Definition of a component.
 */
public class ComponentDefinition {
    private final Function<ComponentDefinition, TileEntity> teSupplier;
    private TileEntityType<? extends TileEntity> tileType;
    public final ResourceLocation location;
    public JsonObject traits;

    // ******* status properties ******* //
    public final Map<String, StatusProperties> status;

    // ******* block item properties ******* //
    public CustomProperties properties;

    public ComponentDefinition(ResourceLocation location, Function<ComponentDefinition, TileEntity> teSupplier) {
        this.location = location;
        this.teSupplier = teSupplier;
        this.status = new LinkedHashMap<>();
        this.status.put(StatusProperties.UNFORMED, new StatusProperties(StatusProperties.UNFORMED, null, true));
        this.status.put(StatusProperties.IDLE, new StatusProperties(StatusProperties.IDLE, getBaseStatus(), true));
        this.status.put(StatusProperties.WORKING, new StatusProperties(StatusProperties.WORKING, getIdleStatus(), true));
        this.status.put(StatusProperties.SUSPEND, new StatusProperties(StatusProperties.SUSPEND, getWorkingStatus(), true));
        this.traits = new JsonObject();
        this.properties = new CustomProperties();
    }

    public TileEntity createNewTileEntity(){
        return tileType != null ? tileType.create() : null;
    }

    public TileEntityType<? extends TileEntity> getTileType() {
        return tileType;
    }

    public void registerTileEntity(Block block, IForgeRegistry<TileEntityType<?>> registry) {
        tileType = TileEntityType.Builder.of(()-> teSupplier.apply(this), block).build(null);
        tileType.setRegistryName(location);
        registry.register(tileType);
    }

    public IMultiblockedRenderer getBaseRenderer() {
        return getBaseStatus().getRenderer();
    }

    public StatusProperties getBaseStatus() {
        return getStatus(StatusProperties.UNFORMED);
    }

    public StatusProperties getIdleStatus() {
        return getStatus(StatusProperties.IDLE);
    }

    public StatusProperties getWorkingStatus() {
        return getStatus(StatusProperties.WORKING);
    }

    public StatusProperties getSuspendStatus() {
        return getStatus(StatusProperties.SUSPEND);
    }

    @Override
    public String toString() {
        return location.toString();
    }

    public String getID() {
        return location.getNamespace() + "." + location.getPath();
    }

    public ItemStack getStackForm() {
        return new ItemStack(MbdComponents.COMPONENT_ITEMS_REGISTRY.get(location), 1);
    }

    public boolean needUpdateTick() {
        return false;
    }

    public StatusProperties getStatus(String status) {
        return this.status.containsKey(status) ? this.status.get(status) : this.status.getOrDefault(StatusProperties.UNFORMED, StatusProperties.EMPTY);
    }

    public AbstractBlock.Properties getBlockProperties() {
        return this.properties.createBlock();
    }

    public Item.Properties getItemProperties() {
        return this.properties.createItem();
    }

    // ******* serialize ******* //

    public final static int VERSION = 2;

    public void fromJson(JsonObject json) {
        int version = JSONUtils.getAsInt(json, "version", 0);

        if (version > VERSION) {
            throw new IllegalArgumentException(String.format("using outdated version of mbd. script is {%d}, mbd supports {%d}", version, VERSION));
        }

        if (json.has("traits")) {
            traits = json.get("traits").getAsJsonObject();
        }
        if (json.has("properties")) {
            properties = Multiblocked.GSON.fromJson(json.get("properties"), CustomProperties.class);
        }

        if (version > 0) {
            JsonObject statusJson = json.get("status").getAsJsonObject();
            getBaseStatus().fromJson(statusJson.get(StatusProperties.UNFORMED).getAsJsonObject());
            getIdleStatus().fromJson(statusJson.get(StatusProperties.IDLE).getAsJsonObject());
            getWorkingStatus().fromJson(statusJson.get(StatusProperties.WORKING).getAsJsonObject());
            getSuspendStatus().fromJson(statusJson.get(StatusProperties.SUSPEND).getAsJsonObject());
            for (Map.Entry<String, JsonElement> entry : statusJson.entrySet()) {
                parseStatus(entry.getKey(), statusJson);
            }
        } else { // legacy
            properties.rotationState = JSONUtils.getAsBoolean(json, "allowRotate", true) ? CustomProperties.RotationState.ALL : CustomProperties.RotationState.NONE;
            properties.showInJei = JSONUtils.getAsBoolean(json, "showInJei", properties.showInJei);

            if (json.has("baseRenderer")) {
                JsonElement renderer = json.get("baseRenderer");
                if (IMultiblockedRendererTypeAdapterFactory.INSTANCE.isPostRenderer(renderer)) {
                    getBaseStatus().setRenderer(() -> Multiblocked.GSON.fromJson(renderer, IMultiblockedRenderer.class));
                } else {
                    getBaseStatus().setRenderer(Multiblocked.GSON.fromJson(renderer, IMultiblockedRenderer.class));
                }
            }

            if (json.has("formedRenderer")) {
                JsonElement renderer = json.get("formedRenderer");
                if (IMultiblockedRendererTypeAdapterFactory.INSTANCE.isPostRenderer(renderer)) {
                    getIdleStatus().setRenderer(() -> Multiblocked.GSON.fromJson(renderer, IMultiblockedRenderer.class));
                } else {
                    getIdleStatus().setRenderer(Multiblocked.GSON.fromJson(renderer, IMultiblockedRenderer.class));
                }
            }

            if (json.has("workingRenderer")) {
                JsonElement renderer = json.get("workingRenderer");
                if (IMultiblockedRendererTypeAdapterFactory.INSTANCE.isPostRenderer(renderer)) {
                    getWorkingStatus().setRenderer(() -> Multiblocked.GSON.fromJson(renderer, IMultiblockedRenderer.class));
                } else {
                    getWorkingStatus().setRenderer(Multiblocked.GSON.fromJson(renderer, IMultiblockedRenderer.class));
                }
            }
        }
    }

    private StatusProperties parseStatus(String name, JsonObject json) {
        if (status.containsKey(name)) {
            return status.get(name);
        } else {
            StatusProperties parent = null;
            JsonObject statusJson = json.get(name).getAsJsonObject();
            if (statusJson.has("parent")) {
                String parentName = statusJson.get("parent").getAsString();
                parent = json.has(parentName) ? parseStatus(parentName, json) : null;
            }
            StatusProperties result = new StatusProperties(name, parent);
            result.fromJson(statusJson);
            status.put(name, result);
            return result;
        }
    }

    public JsonObject toJson(JsonObject json) {
        json.addProperty("version", VERSION);
        json.addProperty("location", location.toString());
        json.add("traits", traits);
        json.add("properties", Multiblocked.GSON.toJsonTree(properties));
        JsonObject statusJson = new JsonObject();
        status.forEach((name, status) -> statusJson.add(name, status.toJson(new JsonObject())));
        json.add("status", statusJson);
        return json;
    }
}
