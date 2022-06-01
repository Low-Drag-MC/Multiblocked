package com.lowdragmc.multiblocked.api.definition;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.multiblocked.api.block.CustomProperties;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.function.Function;

/**
 * Definition of a component.
 */
public class ComponentDefinition {
    private TileEntityType<? extends TileEntity> tileType;
    public final ResourceLocation location;
    private transient final Function<ComponentDefinition, TileEntity> teSupplier;
    public JsonObject traits;
    public boolean allowRotate;
    public boolean showInJei;
    public IMultiblockedRenderer baseRenderer;
    public IMultiblockedRenderer formedRenderer;
    public IMultiblockedRenderer workingRenderer;

    public ComponentDefinition(ResourceLocation location, Function<ComponentDefinition, TileEntity> teSupplier) {
        this.location = location;
        this.teSupplier = teSupplier;
        this.baseRenderer = null;
        this.allowRotate = true;
        this.showInJei = true;
        traits = new JsonObject();
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

    public AbstractBlock.Properties getBlockProperties() {
        return this.properties.createBlock();
    }

    public Item.Properties getItemProperties() {
        return this.properties.createItem();
    }
}
