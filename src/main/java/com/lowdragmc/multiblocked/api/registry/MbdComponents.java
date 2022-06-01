package com.lowdragmc.multiblocked.api.registry;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.utils.EnumHelper;
import com.lowdragmc.lowdraglib.utils.FileUtility;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.block.BlockComponent;
import com.lowdragmc.multiblocked.api.block.ItemComponent;
import com.lowdragmc.multiblocked.api.definition.ComponentDefinition;
import com.lowdragmc.multiblocked.api.definition.ControllerDefinition;
import com.lowdragmc.multiblocked.api.tile.DummyComponentTileEntity;
import com.lowdragmc.multiblocked.client.renderer.ComponentTESR;
import com.lowdragmc.multiblocked.jei.multipage.MultiblockInfoCategory;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.registries.IForgeRegistry;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

public class MbdComponents {
    public static final Map<ResourceLocation, ComponentDefinition> DEFINITION_REGISTRY = new HashMap<>();
    public static final Map<ResourceLocation, ComponentDefinition> TEST_DEFINITION_REGISTRY = new HashMap<>();
    public static final Map<ResourceLocation, BlockComponent> COMPONENT_BLOCKS_REGISTRY = new HashMap<>();
    public static final Map<ResourceLocation, ItemComponent> COMPONENT_ITEMS_REGISTRY = new HashMap<>();
    public static final BlockComponent DummyComponentBlock;
    public static final ItemComponent DummyComponentItem;

    static {
        ComponentDefinition definition = new ComponentDefinition(new ResourceLocation(Multiblocked.MODID, "dummy_component"), DummyComponentTileEntity::new);
        definition.properties.isOpaque = false;
        definition.properties.tabGroup = null;
        definition.showInJei = false;
        registerComponent(definition);
        DummyComponentBlock = COMPONENT_BLOCKS_REGISTRY.get(definition.location);
        DummyComponentItem = COMPONENT_ITEMS_REGISTRY.get(definition.location);
    }

    public static void registerComponent(ComponentDefinition definition) {
        if (DEFINITION_REGISTRY.containsKey(definition.location)) return;
        DEFINITION_REGISTRY.put(definition.location, definition);
        COMPONENT_ITEMS_REGISTRY.computeIfAbsent(definition.location, x -> new ItemComponent(COMPONENT_BLOCKS_REGISTRY.computeIfAbsent(definition.location, X -> new BlockComponent(definition))));
        if (Multiblocked.isClient() && definition instanceof ControllerDefinition && Multiblocked.isJeiLoaded()) {
            MultiblockInfoCategory.registerMultiblock((ControllerDefinition) definition);
        }
    }

    public static void registerBlocks(IForgeRegistry<Block> registry) {
        COMPONENT_BLOCKS_REGISTRY.values().forEach(registry::register);
    }
    
    public static void registerTileEntity(IForgeRegistry<TileEntityType<?>> registry) {
        for (BlockComponent block : COMPONENT_BLOCKS_REGISTRY.values()) {
            block.definition.registerTileEntity(block, registry);
        }
    }

    public static List<Runnable> handlers = new ArrayList<>();

    public static <T extends ComponentDefinition> void registerComponentFromFile(Gson gson, File location, Class<T> clazz, BiConsumer<T, JsonObject> postHandler) {
        for (File file : Optional.ofNullable(location.listFiles((f, n) -> n.endsWith(".json"))).orElse(new File[0])) {
            try {
                JsonObject config = (JsonObject) FileUtility.loadJson(file);
                T definition = gson.fromJson(config, clazz);
                if (definition != null) {
                    registerComponent(definition);
                    if (postHandler != null) {
                        handlers.add(()->postHandler.accept(definition, config));
                    }
                }
            } catch (Exception e) {
                Multiblocked.LOGGER.error("error while loading the definition file {}", file.toString());
            }
        }

    }

    public static void commonLastWork() {
        handlers.forEach(Runnable::run);
        handlers.clear();
    }

    public static void clientLastWork() {
        // set block render layer
        for (BlockComponent block : MbdComponents.COMPONENT_BLOCKS_REGISTRY.values()) {
            RenderTypeLookup.setRenderLayer(block, renderType -> true);
        }
        // register tesr
        for (ComponentDefinition definition : DEFINITION_REGISTRY.values()) {
            ClientRegistry.bindTileEntityRenderer(definition.getTileType(), ComponentTESR::new);
        }
    }
}
