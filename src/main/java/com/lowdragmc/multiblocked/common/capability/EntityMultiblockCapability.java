package com.lowdragmc.multiblocked.common.capability;

import com.google.gson.JsonElement;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.proxy.CapabilityProxy;
import com.lowdragmc.multiblocked.api.capability.trait.CapabilityTrait;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.ingredient.EntityIngredient;
import com.lowdragmc.multiblocked.api.recipe.serde.content.IContentSerializer;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import com.lowdragmc.multiblocked.common.capability.trait.EntityCapabilityTrait;
import com.lowdragmc.multiblocked.common.capability.widget.EntityContentWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EntityMultiblockCapability extends MultiblockCapability<EntityIngredient> {
    public static final EntityMultiblockCapability CAP = new EntityMultiblockCapability();

    private EntityMultiblockCapability() {
        super("entity", 0xFF65CB9D, new IContentSerializer<>() {

            @Override
            public EntityIngredient fromJson(JsonElement json) {
                return EntityIngredient.fromJson(json);
            }

            @Override
            public JsonElement toJson(EntityIngredient content) {
                return content.toJson();
            }

            @Override
            public EntityIngredient of(Object o) {
                return EntityIngredient.of(o);
            }
        });
    }

    @Override
    public EntityIngredient defaultContent() {
        return new EntityIngredient();
    }

    @Override
    public boolean isBlockHasCapability(@Nonnull IO io, @Nonnull BlockEntity tileEntity) {
        return tileEntity instanceof ComponentTileEntity component && component.hasTrait(CAP);
    }

    @Override
    public EntityIngredient copyInner(EntityIngredient content) {
        return content.copy();
    }

    @Override
    public EntityCapabilityProxy createProxy(@Nonnull IO io, @Nonnull BlockEntity tileEntity) {
        return new EntityCapabilityProxy(tileEntity);
    }

    @Override
    public ContentWidget<? super EntityIngredient> createContentWidget() {
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
                BlockInfo.fromBlock(MbdComponents.COMPONENT_BLOCKS_REGISTRY.get(new ResourceLocation(Multiblocked.MODID, "entity"))),
        };
    }

    public static class EntityCapabilityProxy extends CapabilityProxy<EntityIngredient> {

        public EntityCapabilityProxy(BlockEntity tileEntity) {
            super(EntityMultiblockCapability.CAP, tileEntity);
        }

        @Override
        protected List<EntityIngredient> handleRecipeInner(IO io, Recipe recipe, List<EntityIngredient> left, @Nullable String slotName, boolean simulate) {
            if (getTileEntity() instanceof ComponentTileEntity<?> component) {
                BlockPos pos = component.getBlockPos().relative(component.getFrontFacing());
                if (io == IO.IN) {
                    List<Entity> entities = component.getLevel().getEntities(null, new AABB(
                            pos,
                            pos.offset(1, 1, 1)));
                    for (Entity entity : entities) {
                        if (entity.isAlive()) {
                            if (left.removeIf(ingredient -> ingredient.match(entity))) {
                                if (!simulate) {
                                    entity.remove(Entity.RemovalReason.DISCARDED);
                                }
                            }
                        }
                    }
                } else if (io == IO.OUT){
                    if (!simulate && component.getLevel() instanceof ServerLevel serverLevel) {
                        for (EntityIngredient ingredient : left) {
                            ingredient.spawn(serverLevel, ingredient.tag, pos);
                        }
                    }
                    return null;
                }
            }
            return left.isEmpty() ? null : left;
        }

        Set<Entity> entities = new HashSet<>();

        @Override
        protected boolean hasInnerChanged() {
            if (getTileEntity() instanceof ComponentTileEntity<?> component) {
                BlockPos pos = component.getBlockPos().relative(component.getFrontFacing());
                List<Entity> entities = component.getLevel().getEntities(null, new AABB(
                       pos,
                       pos.offset(1, 1, 1)));
                Set<Entity> temp = new HashSet<>();
                for (Entity entity : entities) {
                    if (entity.isAlive()) {
                        temp.add(entity);
                    }
                }
                if (this.entities.size() == temp.size() && this.entities.containsAll(temp)) {
                    return false;
                }
                this.entities = temp;
                return true;
            }
            return false;
        }
    }

}
