package com.lowdragmc.multiblocked.api.registry;

import com.google.gson.JsonObject;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class MbdComponents {
    public static final Map<ResourceLocation, ComponentDefinition> DEFINITION_REGISTRY = new HashMap<>();
    public static final Map<ResourceLocation, ComponentDefinition> TEST_DEFINITION_REGISTRY = new HashMap<>();
    public static final Map<ResourceLocation, Block> COMPONENT_BLOCKS_REGISTRY = new HashMap<>();
    public static final Map<ResourceLocation, BlockItem> COMPONENT_ITEMS_REGISTRY = new HashMap<>();
    public static final Map<ItemStack, ControllerDefinition[]> NO_NEED_CONTROLLER_MB = new HashMap<>();
    public static final Set<Item> CATALYST_SET = new HashSet<>();
    public static BlockComponent DummyComponentBlock;
    public static ItemComponent DummyComponentItem;
    private static final List<RegistryEntry<?, ?>> ENTRIES = new ArrayList<>();

    static {
        ComponentDefinition definition = new ComponentDefinition(new ResourceLocation(Multiblocked.MODID, "dummy_component"), DummyComponentTileEntity.class);
        definition.properties.isOpaque = false;
        definition.properties.tabGroup = null;
        definition.properties.showInJei = false;
        registerComponent(definition);
    }

    public static void registerComponent(ComponentDefinition definition) {
        if (DEFINITION_REGISTRY.containsKey(definition.location)) return;
        registerComponent(definition, x -> new BlockComponent(definition), block -> new ItemComponent(block));
    }


    public static <T extends ComponentDefinition, B extends Block> void registerComponent(T definition, Function<T, B> block, Function<B, BlockItem> item) {
        ENTRIES.add(new RegistryEntry<>(definition, block, item));
    }

    private record RegistryEntry<T extends ComponentDefinition, B extends Block>(T definition, Function<T, B> block, Function<B, BlockItem> item){ }

    @SuppressWarnings("unchecked")
    public static void registerBlocks(IForgeRegistry<Block> registry) {
        for (RegistryEntry entry : ENTRIES) {
            if (DEFINITION_REGISTRY.containsKey(entry.definition.location)) return;
            DEFINITION_REGISTRY.put(entry.definition.location, entry.definition);
            COMPONENT_ITEMS_REGISTRY.computeIfAbsent(entry.definition.location, x -> (BlockItem) entry.item.apply(
                    COMPONENT_BLOCKS_REGISTRY.computeIfAbsent(entry.definition.location, X -> (Block) entry.block.apply(entry.definition))));
            if (Multiblocked.isClient() && entry.definition instanceof ControllerDefinition && Multiblocked.isJeiLoaded()) {
                MultiblockInfoCategory.registerMultiblock((ControllerDefinition) entry.definition);
            }
        }
        ENTRIES.clear();
        DummyComponentBlock = (BlockComponent) COMPONENT_BLOCKS_REGISTRY.get(new ResourceLocation(Multiblocked.MODID, "dummy_component"));
        DummyComponentItem = (ItemComponent) COMPONENT_ITEMS_REGISTRY.get(new ResourceLocation(Multiblocked.MODID, "dummy_component"));
        COMPONENT_BLOCKS_REGISTRY.values().forEach(registry::register);
    }
    
    public static void registerTileEntity(IForgeRegistry<BlockEntityType<?>> registry) {
        COMPONENT_BLOCKS_REGISTRY.forEach((k, v) -> Optional.ofNullable(DEFINITION_REGISTRY.get(k)).ifPresent(d -> d.registerTileEntity(v, registry)));
    }

    public static List<Runnable> handlers = new ArrayList<>();

    public static <T extends ComponentDefinition> void registerComponentFromFile(File location, Class<T> clazz, BiConsumer<T, JsonObject> postHandler) {
        registerComponentFromFile(location, clazz, null, null, postHandler);
    }

    public static <T extends ComponentDefinition, B extends Block> void registerComponentFromFile(File location, Class<T> clazz, @Nullable Function<T, B> block, @Nullable Function<B, BlockItem> item, BiConsumer<T, JsonObject> postHandler) {
        for (File file : Optional.ofNullable(location.listFiles((f, n) -> n.endsWith(".json"))).orElse(new File[0])) {
            try {
                JsonObject config = (JsonObject) FileUtility.loadJson(file);
                Constructor<?> constructor = Arrays.stream(clazz.getDeclaredConstructors())
                        .filter(c -> {
                            if (c.getParameterCount() != 1) return false;
                            Class<?>[] classes = c.getParameterTypes();
                            return ResourceLocation.class.isAssignableFrom(classes[0]);
                        }).findFirst().orElseThrow(() -> new IllegalArgumentException("cant find the constructor with the parameters(resourcelocation)"));
                T definition = (T) constructor.newInstance(new ResourceLocation(config.get("location").getAsString()));
                definition.fromJson(config);
                if (block == null || item == null) {
                    registerComponent(definition);
                } else {
                    registerComponent(definition, block, item);
                }
                if (postHandler != null) {
                    handlers.add(()->postHandler.accept(definition, config));
                }
            } catch (Exception e) {
                Multiblocked.LOGGER.error("error while loading the definition file {}", file.toString());
            }
        }
    }

    public static <T extends ComponentDefinition> void registerComponentFromResource(Class<?> source, ResourceLocation location, Class<T> clazz, BiConsumer<T, JsonObject> postHandler) {
        registerComponentFromResource(source, location, clazz, null, null, postHandler);
    }

    @SuppressWarnings("unchecked")
    public static <T extends ComponentDefinition, B extends Block> void registerComponentFromResource(Class<?> source, ResourceLocation location, Class<T> clazz, @Nullable Function<T, B> block, @Nullable Function<B, BlockItem> item, BiConsumer<T, JsonObject> postHandler) {
        try {
            InputStream inputstream = source.getResourceAsStream(String.format("/assets/%s/definition/%s.json", location.getNamespace(), location.getPath()));
            JsonObject config = FileUtility.jsonParser.parse(new InputStreamReader(inputstream)).getAsJsonObject();
            Constructor<?> constructor = Arrays.stream(clazz.getDeclaredConstructors())
                    .filter(c -> {
                        if (c.getParameterCount() != 1) return false;
                        Class<?>[] classes = c.getParameterTypes();
                        return ResourceLocation.class.isAssignableFrom(classes[0]);
                    }).findFirst().orElseThrow(() -> new IllegalArgumentException("cant find the constructor with the parameters(resourcelocation)"));
            T definition = (T) constructor.newInstance(new ResourceLocation(config.get("location").getAsString()));
            definition.fromJson(config);
            if (block == null || item == null) {
                registerComponent(definition);
            } else {
                registerComponent(definition, block, item);
            }
            if (postHandler != null) {
                handlers.add(()->postHandler.accept(definition, config));
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

    public static void registerNoNeedController(ItemStack catalyst, ControllerDefinition definition) {
        CATALYST_SET.add(catalyst.getItem());
        ItemStack key = catalyst;
        for (ItemStack itemStack : NO_NEED_CONTROLLER_MB.keySet()) {
            if (ItemStack.isSameItemSameTags(itemStack, catalyst)) {
                key = itemStack;
                break;
            }
        }
        NO_NEED_CONTROLLER_MB.put(key, ArrayUtils.add(NO_NEED_CONTROLLER_MB.get(key), definition));
    }

    public static ControllerDefinition[] checkNoNeedController(ItemStack catalyst) {
        if (catalyst == null) return new ControllerDefinition[0];
        if (CATALYST_SET.contains(catalyst.getItem())) {
            for (ItemStack itemStack : NO_NEED_CONTROLLER_MB.keySet()) {
                if (ItemStack.isSameItemSameTags(itemStack, catalyst)) {
                    return NO_NEED_CONTROLLER_MB.get(itemStack);
                }
            }
        }
        return new ControllerDefinition[0];
    }

    public static void init() {
    }
}
