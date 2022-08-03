package com.lowdragmc.multiblocked.common.capability;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.proxy.CapabilityProxy;
import com.lowdragmc.multiblocked.api.capability.trait.CapabilityTrait;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.serde.content.IContentSerializer;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import com.lowdragmc.multiblocked.common.capability.trait.EntityCapabilityTrait;
import com.lowdragmc.multiblocked.common.capability.widget.EntityContentWidget;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EntityMultiblockCapability extends MultiblockCapability<EntityType<?>> {
    public static final EntityMultiblockCapability CAP = new EntityMultiblockCapability();

    private EntityMultiblockCapability() {
        super("entity", 0xFF65CB9D, new IContentSerializer<>() {

            @Override
            public EntityType<?> fromJson(JsonElement json) {
                return EntityType.byString(json.getAsString()).orElse(EntityType.PIG);
            }

            @Override
            public JsonElement toJson(EntityType<?> content) {
                return new JsonPrimitive(content.getRegistryName().toString());
            }

            @Override
            public EntityType<?> of(Object o) {
                if (o instanceof EntityType<?>) {
                    return (EntityType<?>) o;
                } else if (o instanceof CharSequence charSequence) {
                    return EntityType.byString(charSequence.toString()).orElse(EntityType.PIG);
                } else if (o instanceof ResourceLocation resourceLocation) {
                    return EntityType.byString(resourceLocation.toString()).orElse(EntityType.PIG);
                }
                return EntityType.PIG;
            }
        });
    }

    @Override
    public EntityType<?> defaultContent() {
        return EntityType.PIG;
    }

    @Override
    public boolean isBlockHasCapability(@Nonnull IO io, @Nonnull BlockEntity tileEntity) {
        return tileEntity instanceof ComponentTileEntity component && component.hasTrait(CAP);
    }

    @Override
    public EntityType<?> copyInner(EntityType<?> content) {
        return content;
    }

    @Override
    public EntityCapabilityProxy createProxy(@Nonnull IO io, @Nonnull BlockEntity tileEntity) {
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

    public static class EntityCapabilityProxy extends CapabilityProxy<EntityType<?>> {

        public EntityCapabilityProxy(BlockEntity tileEntity) {
            super(EntityMultiblockCapability.CAP, tileEntity);
        }

        @Override
        protected List<EntityType<?>> handleRecipeInner(IO io, Recipe recipe, List<EntityType<?>> left, boolean simulate) {
            if (io == IO.IN) {
                List<Entity> entities = getTileEntity().getLevel().getEntities(null, new AABB(
                        getTileEntity().getBlockPos().above(),
                        getTileEntity().getBlockPos().above().offset(1, 1, 1)));
                for (Entity entity : entities) {
                    if (entity.isAlive()) {
                        if (left.remove(entity.getType())) {
                            if (!simulate) {
                                entity.remove(Entity.RemovalReason.DISCARDED);
                            }
                        }
                    }
                }
            } else if (io == IO.OUT){
                if (!simulate && getTileEntity().getLevel() instanceof ServerLevel serverLevel) {
                    for (EntityType<?> type : left) {
                        type.spawn(serverLevel, null, null, null, getTileEntity().getBlockPos().above(), MobSpawnType.NATURAL, false, false);
                    }
                }
                return null;
            }
            return left.isEmpty() ? null : left;
        }

        Set<EntityType<?>> entities = new HashSet<>();

        @Override
        protected boolean hasInnerChanged() {
            List<Entity> entities = getTileEntity().getLevel().getEntities(null, new AABB(
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
