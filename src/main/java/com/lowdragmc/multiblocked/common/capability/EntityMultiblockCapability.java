package com.lowdragmc.multiblocked.common.capability;

import com.google.gson.*;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.proxy.CapabilityProxy;
import com.lowdragmc.multiblocked.api.capability.trait.CapabilityTrait;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import com.lowdragmc.multiblocked.common.capability.trait.EntityCapabilityTrait;
import com.lowdragmc.multiblocked.common.capability.widget.EntityContentWidget;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.server.ServerWorld;


import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EntityMultiblockCapability extends MultiblockCapability<EntityType<?>> {
    public static final EntityMultiblockCapability CAP = new EntityMultiblockCapability();

    private EntityMultiblockCapability() {
        super("entity", 0xFF65CB9D);
    }

    @Override
    public EntityType<?> defaultContent() {
        return EntityType.PIG;
    }

    @Override
    public boolean isBlockHasCapability(@Nonnull IO io, @Nonnull TileEntity tileEntity) {
        if (tileEntity instanceof ComponentTileEntity) {
            return ((ComponentTileEntity<?>) tileEntity).hasTrait(CAP);
        }
        return false;
    }

    @Override
    public EntityType<?> copyInner(EntityType<?> content) {
        return content;
    }

    @Override
    public CapabilityProxy<? extends EntityType<?>> createProxy(@Nonnull IO io, @Nonnull TileEntity tileEntity) {
        return new EntityCapabilityProxy(tileEntity);
    }

    @Override
    public ContentWidget<? super EntityType<?>> createContentWidget() {
        return new EntityContentWidget();
    }

    @Override
    public boolean hasTrait() {
        return true;
    }

    @Override
    public CapabilityTrait createTrait() {
        return new EntityCapabilityTrait();
    }

    @Override
    public BlockInfo[] getCandidates() {
        return new BlockInfo[]{
                BlockInfo.fromBlockState(Blocks.BEACON.defaultBlockState()),
                BlockInfo.fromBlockState(Blocks.CHEST.defaultBlockState()),
        };
    }

    @Override
    public EntityType<?> of(Object o) {
        if (o instanceof EntityType<?>) {
            return (EntityType<?>) o;
        } else if (o instanceof CharSequence || o instanceof ResourceLocation) {
            return EntityType.byString(o.toString()).orElse(EntityType.PIG);
        }
        return EntityType.PIG;
    }

    @Override
    public EntityType<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return EntityType.byString(json.getAsString()).orElse(EntityType.PIG);
    }

    @Override
    public JsonElement serialize(EntityType<?> src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.getRegistryName().toString());
    }

    public static class EntityCapabilityProxy extends CapabilityProxy<EntityType<?>> {

        public EntityCapabilityProxy(TileEntity tileEntity) {
            super(EntityMultiblockCapability.CAP, tileEntity);
        }

        @Override
        protected List<EntityType<?>> handleRecipeInner(IO io, Recipe recipe, List<EntityType<?>> left, boolean simulate) {
            if (io == IO.IN) {
                List<Entity> entities = getTileEntity().getLevel().getEntities(null, new AxisAlignedBB(
                        getTileEntity().getBlockPos().above(),
                        getTileEntity().getBlockPos().above().offset(1, 1, 1)));
                for (Entity entity : entities) {
                    if (entity.isAlive()) {
                        if (left.remove(entity.getType())) {
                            if (!simulate) {
                                entity.remove(false);
                            }
                        }
                    }
                }
            } else if (io == IO.OUT){
                if (!simulate && getTileEntity().getLevel() instanceof ServerWorld) {
                    for (EntityType<?> type : left) {
                        type.spawn((ServerWorld) getTileEntity().getLevel(), null, null, null,
                                getTileEntity().getBlockPos().above(), SpawnReason.NATURAL,
                                false, false);
                    }
                }
                return null;
            }
            return left.isEmpty() ? null : left;
        }

        Set<EntityType<?>> entities = new HashSet<>();

        @Override
        protected boolean hasInnerChanged() {
            List<Entity> entities = getTileEntity().getLevel().getEntities(null, new AxisAlignedBB(
                    getTileEntity().getBlockPos().above(),
                    getTileEntity().getBlockPos().above().offset(1, 1, 1)));
            Set<EntityType<?>> temp = new HashSet<>();
            for (Entity entity : entities) {
                if (entity.isAlive()) {
                    temp.add(entity.getType());
                }
            }
            if (this.entities.size() == temp.size() && this.entities.containsAll(temp)) {
                return false;
            }
            this.entities = temp;
            return true;
        }
    }

}
