package com.lowdragmc.multiblocked.api.definition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.block.CustomProperties;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.trait.CapabilityTrait;
import com.lowdragmc.multiblocked.api.capability.trait.InterfaceUser;
import com.lowdragmc.multiblocked.api.json.IMultiblockedRendererTypeAdapterFactory;
import com.lowdragmc.multiblocked.api.registry.MbdCapabilities;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.api.tile.IComponent;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import com.lowdragmc.multiblocked.core.core.DynamicTileEntityGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;

/**
 * Definition of a component.
 */
public class ComponentDefinition {
    private BlockEntityType<? extends BlockEntity> tileType;
    private final Class<? extends IComponent> clazz;
    public final ResourceLocation location;
    public JsonObject traits;
    public String uiLocation;
    private CompoundTag ui;

    // ******* status properties ******* //
    public final Map<String, StatusProperties> status;
    // ******* block item properties ******* //
    public CustomProperties properties;

    public ComponentDefinition(ResourceLocation location,  Class<? extends IComponent> clazz) {
        this.location = location;
        this.clazz = clazz;
        this.status = new LinkedHashMap<>();
        this.status.put(StatusProperties.UNFORMED, new StatusProperties(StatusProperties.UNFORMED, null, true));
        this.status.put(StatusProperties.IDLE, new StatusProperties(StatusProperties.IDLE, getBaseStatus(), true));
        this.status.put(StatusProperties.WORKING, new StatusProperties(StatusProperties.WORKING, getIdleStatus(), true));
        this.status.put(StatusProperties.SUSPEND, new StatusProperties(StatusProperties.SUSPEND, getWorkingStatus(), true));
        this.traits = new JsonObject();
        this.uiLocation = "";
        this.properties = new CustomProperties();
    }

    public BlockEntity createNewTileEntity(BlockPos pos, BlockState state){
        return tileType != null ? tileType.create(pos, state) : null;
    }

    public BlockEntityType<? extends BlockEntity> getTileType() {
        return tileType;
    }

    public void registerTileEntity(Block block, IForgeRegistry<BlockEntityType<?>> registry) {
        final Class<?> teClazz;
        List<CapabilityTrait> useInterfaceTraits = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : traits.entrySet()) {
            MultiblockCapability<?> capability = MbdCapabilities.get(entry.getKey());
            if (capability != null && capability.hasTrait()) {
                CapabilityTrait trait = capability.createTrait();
                if (trait.getClass().isAnnotationPresent(InterfaceUser.class)) {
                    useInterfaceTraits.add(trait);
                }
            }
        }
        if (!useInterfaceTraits.isEmpty()) {
            teClazz = new DynamicTileEntityGenerator(location.getPath(), useInterfaceTraits, clazz).generateClass();
        } else {
            teClazz = clazz;
        }

        Constructor<?> constructor = Arrays.stream(teClazz.getDeclaredConstructors())
                .filter(c -> {
                    if (c.getParameterCount() != 3) return false;
                    Class<?>[] classes = c.getParameterTypes();
                    return ComponentDefinition.class.isAssignableFrom(classes[0]) && classes[1] == BlockPos.class && classes[2] == BlockState.class;
                }).findFirst().orElseThrow(() -> new IllegalArgumentException("cant find the constructor with the parameters(definition, pos, state)"));

        tileType = BlockEntityType.Builder.of((pos, state) -> {
            try {
                return (BlockEntity) constructor.newInstance(new Object[]{this, pos, state});
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }, block).build(null);
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

    public BlockBehaviour.Properties getBlockProperties() {
        return this.properties.createBlock();
    }

    public Item.Properties getItemProperties() {
        return this.properties.createItem();
    }

    // ******* ldlib ui ******* //
    @Nullable
    public WidgetGroup createLDLibUI() {
        if (ui == null) {
            if (uiLocation == null || uiLocation.isEmpty()) return null;
            File file = new File(Multiblocked.location, uiLocation);
            if (file.isFile()) {
                try {
                    this.ui = NbtIo.read(file).getCompound("root");
                } catch (IOException ignored) {}
            }
        }
        if (this.ui != null) {
            WidgetGroup root = new WidgetGroup();
            root.deserializeNBT(ui);
            return root;
        }
        return null;
    }

    // ******* serialize ******* //

    public final static int VERSION = 3;

    public void fromJson(JsonObject json) {
        int version = GsonHelper.getAsInt(json, "version", 0);

        if (version > VERSION) {
            throw new IllegalArgumentException(String.format("using outdated version of mbd. script is {%d}, mbd supports {%d}", version, VERSION));
        }

        uiLocation = GsonHelper.getAsString(json, "ui", "");

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
            properties.rotationState = GsonHelper.getAsBoolean(json, "allowRotate", true) ? CustomProperties.RotationState.ALL : CustomProperties.RotationState.NONE;
            properties.showInJei = GsonHelper.getAsBoolean(json, "showInJei", properties.showInJei);

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
        if (uiLocation != null && !uiLocation.isEmpty()) {
            json.addProperty("ui", uiLocation);
        }
        json.add("traits", traits);
        json.add("properties", Multiblocked.GSON.toJsonTree(properties));
        JsonObject statusJson = new JsonObject();
        status.forEach((name, status) -> statusJson.add(name, status.toJson(new JsonObject())));
        json.add("status", statusJson);
        return json;
    }
}
