package com.lowdragmc.multiblocked.api.definition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.multiblocked.api.block.CustomProperties;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.trait.CapabilityTrait;
import com.lowdragmc.multiblocked.api.capability.trait.InterfaceUser;
import com.lowdragmc.multiblocked.api.registry.MbdCapabilities;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.api.tile.IComponent;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import com.lowdragmc.multiblocked.core.core.DynamicTileEntityGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.IForgeRegistry;

import java.lang.reflect.Constructor;
import java.util.*;

/**
 * Definition of a component.
 */
public class ComponentDefinition {
    private BlockEntityType<? extends BlockEntity> tileType;
    private transient final Class<? extends IComponent> clazz;
    public final ResourceLocation location;
    public JsonObject traits;
    public boolean allowRotate;
    public boolean showInJei;
    public IMultiblockedRenderer baseRenderer;
    public IMultiblockedRenderer formedRenderer;
    public IMultiblockedRenderer workingRenderer;

    public ComponentDefinition(ResourceLocation location, Class<? extends IComponent> clazz) {
        this.location = location;
        this.clazz = clazz;
        this.baseRenderer = null;
        this.allowRotate = true;
        this.showInJei = true;
        this.traits = new JsonObject();
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

    public IRenderer getRenderer() {
        return baseRenderer;
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

    // ******* properties ******* //
    public CustomProperties properties = new CustomProperties();

    public BlockBehaviour.Properties getBlockProperties() {
        return this.properties.createBlock();
    }

    public Item.Properties getItemProperties() {
        return this.properties.createItem();
    }
}
