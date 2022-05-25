package com.lowdragmc.multiblocked.api.definition;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.multiblocked.api.block.CustomProperties;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import com.lowdragmc.multiblocked.client.renderer.IMultiblockedRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.commons.lang3.function.TriFunction;

import java.util.EnumMap;
import java.util.List;

/**
 * Definition of a component.
 */
public class ComponentDefinition {
    private BlockEntityType<? extends ComponentTileEntity<?>> tileType;
    public final ResourceLocation location;
    public transient final TriFunction<ComponentDefinition, BlockPos, BlockState, ? extends ComponentTileEntity<?>> teSupplier;
    public transient final EnumMap<Direction, List<AABB>> baseAABB;
    public transient final EnumMap<Direction, List<AABB>> formedAABB;
    public JsonObject traits;
    public boolean allowRotate;
    public boolean showInJei;
    public IMultiblockedRenderer baseRenderer;
    public IMultiblockedRenderer formedRenderer;
    public IMultiblockedRenderer workingRenderer;

    public ComponentDefinition(ResourceLocation location, TriFunction<ComponentDefinition, BlockPos, BlockState, ? extends ComponentTileEntity<?>> teSupplier) {
        this.location = location;
        this.teSupplier = teSupplier;
        this.baseRenderer = null;
        this.allowRotate = true;
        this.showInJei = true;
        baseAABB = new EnumMap<>(Direction.class);
        formedAABB = new EnumMap<>(Direction.class);
        traits = new JsonObject();
    }

    public ComponentTileEntity<?> createNewTileEntity(BlockPos pos, BlockState state){
        return tileType != null ? tileType.create(pos, state) : null;
    }

    public BlockEntityType<? extends ComponentTileEntity<?>> getTileType() {
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

    public BlockBehaviour.Properties getBlockProperties() {
        return this.properties.createBlock();
    }

    public Item.Properties getItemProperties() {
        return this.properties.createItem();
    }
}
