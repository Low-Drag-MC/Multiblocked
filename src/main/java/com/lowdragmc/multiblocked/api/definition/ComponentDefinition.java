package com.lowdragmc.multiblocked.api.definition;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.multiblocked.api.block.CustomProperties;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Function;

///**
// * Definition of a component.
// */
public class ComponentDefinition {
    private TileEntityType<? extends ComponentTileEntity<?>> tileType;
    public final ResourceLocation location;
    public transient final Function<ComponentDefinition, ? extends ComponentTileEntity<?>> teSupplier;
    public transient final EnumMap<Direction, List<AxisAlignedBB>> baseAABB;
    public transient final EnumMap<Direction, List<AxisAlignedBB>> formedAABB;
    public JsonObject traits;
    public boolean allowRotate;
    public boolean showInJei;
    public IMultiblockedRenderer baseRenderer;
    public IMultiblockedRenderer formedRenderer;
    public IMultiblockedRenderer workingRenderer;

    public ComponentDefinition(ResourceLocation location, Function<ComponentDefinition, ? extends ComponentTileEntity<?>> teSupplier) {
        this.location = location;
        this.teSupplier = teSupplier;
        this.baseRenderer = null;
        this.allowRotate = true;
        this.showInJei = true;
        baseAABB = new EnumMap<>(Direction.class);
        formedAABB = new EnumMap<>(Direction.class);
        traits = new JsonObject();
    }

    public ComponentTileEntity<?> createNewTileEntity(){
        return tileType != null ? tileType.create() : null;
    }

    public TileEntityType<? extends ComponentTileEntity<?>> getTileType() {
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

    public ItemStack getStackForm() {
        return new ItemStack(MbdComponents.COMPONENT_ITEMS_REGISTRY.get(location), 1);
    }

//    public void setAABB(boolean isFormed, AxisAlignedBB... aaBBs) {
//        if (isFormed) this.formedAABB.clear(); else this.baseAABB.clear();
//        EnumMap<Direction, List<AxisAlignedBB>> aabb = isFormed ? this.formedAABB : this.baseAABB;
//        Arrays.stream(aaBBs).forEach(aaBB->{
//            for (Direction facing : Direction.values()) {
//                aabb.computeIfAbsent(facing, f->new ArrayList<>()).add(RayTraceUtils.rotateAABB(aaBB, facing));
//            }
//        });
//    }
//
//    public List<AxisAlignedBB> getAABB(boolean isFormed, Direction facing) {
//        return isFormed ? this.formedAABB.getOrDefault(facing, Collections.singletonList(Block.FULL_BLOCK_AABB)) :
//                this.baseAABB.getOrDefault(facing, Collections.singletonList(Block.FULL_BLOCK_AABB));
//    }


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
