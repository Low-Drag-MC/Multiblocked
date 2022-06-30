package com.lowdragmc.multiblocked.api.registry;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.client.renderer.IBlockRendererProvider;
import com.lowdragmc.lowdraglib.utils.FileUtility;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.block.BlockComponent;
import com.lowdragmc.multiblocked.api.block.ItemComponent;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.tile.DummyComponentTileEntity;
import com.lowdragmc.multiblocked.client.renderer.ComponentTESR;
import com.lowdragmc.multiblocked.jei.multipage.MultiblockInfoCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class MbdComponents {
    public static final Map<ResourceLocation, ComponentDefinition> DEFINITION_REGISTRY = new HashMap<>();
    public static final Map<ResourceLocation, ComponentDefinition> TEST_DEFINITION_REGISTRY = new HashMap<>();
    public static final Map<ResourceLocation, Block> COMPONENT_BLOCKS_REGISTRY = new HashMap<>();
    public static final Map<ResourceLocation, BlockItem> COMPONENT_ITEMS_REGISTRY = new HashMap<>();
    public static final BlockComponent DummyComponentBlock;
    public static final ItemComponent DummyComponentItem;

    static {
        ComponentDefinition definition = new ComponentDefinition(new ResourceLocation(Multiblocked.MODID, "dummy_component"), DummyComponentTileEntity.class);
        definition.properties.isOpaque = false;
        definition.properties.tabGroup = null;
        definition.showInJei = false;
        registerComponent(definition);
        DummyComponentBlock = (BlockComponent) COMPONENT_BLOCKS_REGISTRY.get(definition.location);
        DummyComponentItem = (ItemComponent) COMPONENT_ITEMS_REGISTRY.get(definition.location);
    }

    public static void registerComponent(ComponentDefinition definition) {
        if (DEFINITION_REGISTRY.containsKey(definition.location)) return;
        registerComponent(definition, BlockComponent::new, ItemComponent::new);
    }

    public static <T extends ComponentDefinition, B extends Block> void registerComponent(T definition, Function<T, B> block, Function<B, BlockItem> item) {
        if (DEFINITION_REGISTRY.containsKey(definition.location)) return;
        DEFINITION_REGISTRY.put(definition.location, definition);
        COMPONENT_ITEMS_REGISTRY.computeIfAbsent(definition.location, x -> item.apply(
                (B) COMPONENT_BLOCKS_REGISTRY.computeIfAbsent(definition.location, X -> block.apply(definition))));
        if (Multiblocked.isClient() && definition instanceof ControllerDefinition && Multiblocked.isJeiLoaded()) {
            MultiblockInfoCategory.registerMultiblock((ControllerDefinition) definition);
        }
    }

    public static void registerBlocks(IForgeRegistry<Block> registry) {
        COMPONENT_BLOCKS_REGISTRY.values().forEach(registry::register);
    }
    
    public static void registerTileEntity(IForgeRegistry<BlockEntityType<?>> registry) {
        COMPONENT_BLOCKS_REGISTRY.forEach((k, v) -> Optional.ofNullable(DEFINITION_REGISTRY.get(k)).ifPresent(d -> d.registerTileEntity(v, registry)));
    }

    public static List<Runnable> handlers = new ArrayList<>();

    public static <T extends ComponentDefinition> void registerComponentFromFile(Gson gson, File location, Class<T> clazz, BiConsumer<T, JsonObject> postHandler) {
        registerComponentFromFile(gson, location, clazz, null, null, postHandler);
    }

    public static <T extends ComponentDefinition, B extends Block> void registerComponentFromFile(Gson gson, File location, Class<T> clazz, @Nullable Function<T, B> block, @Nullable Function<B, BlockItem> item, BiConsumer<T, JsonObject> postHandler) {
        for (File file : Optional.ofNullable(location.listFiles((f, n) -> n.endsWith(".json"))).orElse(new File[0])) {
            try {
                JsonObject config = (JsonObject) FileUtility.loadJson(file);
                T definition = gson.fromJson(config, clazz);
                if (definition != null) {
                    if (block == null || item == null) {
                        registerComponent(definition);
                    } else {
                        registerComponent(definition, block, item);
                    }
                    if (postHandler != null) {
                        handlers.add(()->postHandler.accept(definition, config));
                    }
                }
            } catch (Exception e) {
                Multiblocked.LOGGER.error("error while loading the definition file {}", file.toString());
            }
        }
    }

    public static <T extends ComponentDefinition> void registerComponentFromResource(Class<?> source, Gson gson, ResourceLocation location, Class<T> clazz, BiConsumer<T, JsonObject> postHandler) {
        registerComponentFromResource(source, gson, location, clazz, null, null, postHandler);
    }

    public static <T extends ComponentDefinition, B extends Block> void registerComponentFromResource(Class<?> source, Gson gson, ResourceLocation location, Class<T> clazz, @Nullable Function<T, B> block, @Nullable Function<B, BlockItem> item, BiConsumer<T, JsonObject> postHandler) {
        try {
            InputStream inputstream = source.getResourceAsStream(String.format("/assets/%s/definition/%s.json", location.getNamespace(), location.getPath()));
            JsonObject config = FileUtility.jsonParser.parse(new InputStreamReader(inputstream)).getAsJsonObject();
            T definition = gson.fromJson(config, clazz);
            if (definition != null) {
                if (block == null || item == null) {
                    registerComponent(definition);
                } else {
                    registerComponent(definition, block, item);
                }
                if (postHandler != null) {
                    handlers.add(()->postHandler.accept(definition, config));
                }
            }
        } catch (Exception e) {
            Multiblocked.LOGGER.error("error while loading the definition resource {}", location.toString());
        }
    }

    public static void commonLastWork() {
        handlers.forEach(Runnable::run);
        handlers.clear();
    }

    public static void clientLastWork() {
        // set block render layer
        for (Block block : MbdComponents.COMPONENT_BLOCKS_REGISTRY.values()) {
            ItemBlockRenderTypes.setRenderLayer(block, renderType -> true);
        }
    }

    public static void registerBlockEntityRenderer(EntityRenderersEvent.RegisterRenderers event) {
        // register tesr
        for (ComponentDefinition definition : DEFINITION_REGISTRY.values()) {
            event.registerBlockEntityRenderer(definition.getTileType(), ComponentTESR::new);
        }
    }
}
