package com.lowdragmc.multiblocked.api.definition;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.multiblocked.api.block.CustomProperties;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
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
import org.apache.commons.lang3.function.TriFunction;

/**
 * Definition of a component.
 */
public class ComponentDefinition {
    private BlockEntityType<? extends BlockEntity> tileType;
    public final ResourceLocation location;
    private transient final TriFunction<ComponentDefinition, BlockPos, BlockState, BlockEntity> teSupplier;
    public JsonObject traits;
    public boolean allowRotate;
    public boolean showInJei;
    public IMultiblockedRenderer baseRenderer;
    public IMultiblockedRenderer formedRenderer;
    public IMultiblockedRenderer workingRenderer;

    public ComponentDefinition(ResourceLocation location, TriFunction<ComponentDefinition, BlockPos, BlockState, BlockEntity> teSupplier) {
        this.location = location;
        this.teSupplier = teSupplier;
        this.baseRenderer = null;
        this.allowRotate = true;
        this.showInJei = true;
        traits = new JsonObject();
    }

    public BlockEntity createNewTileEntity(BlockPos pos, BlockState state){
        return tileType != null ? tileType.create(pos, state) : null;
    }

    public BlockEntityType<? extends BlockEntity> getTileType() {
        return tileType;
    }

    public void registerTileEntity(Block block, IForgeRegistry<BlockEntityType<?>> registry) {
        tileType = BlockEntityType.Builder.of((pos, state) -> teSupplier.apply(this, pos, state), block).build(null);
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
