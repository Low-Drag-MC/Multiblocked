package com.lowdragmc.multiblocked.common.capability;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.ICapabilityProxyHolder;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.proxy.CapabilityProxy;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.serde.content.SerializerFloat;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.common.capability.widget.NumberContentWidget;
import com.lowdragmc.multiblocked.common.tile.CreateKineticSourceTileEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

/**
 * @author KilaBash
 * @date 2022/06/02
 * @implNote CreateStressCapacityCapability
 */
public class CreateStressCapacityCapability extends MultiblockCapability<Float> {
    public static final CreateStressCapacityCapability CAP = new CreateStressCapacityCapability();

    private CreateStressCapacityCapability() {
        super("create_stress", 0xFFd9d06f, SerializerFloat.INSTANCE);
    }

    @Override
    public Float defaultContent() {
        return 64f;
    }

    @Override
    public boolean isBlockHasCapability(@Nonnull IO io, @Nonnull
            BlockEntity tileEntity) {
        if (tileEntity instanceof CreateKineticSourceTileEntity) {
            if (io == IO.IN && ((CreateKineticSourceTileEntity) tileEntity).isGenerator()) {
                return false;
            }
            return io != IO.OUT || ((CreateKineticSourceTileEntity) tileEntity).isGenerator();
        }
        return false;
    }

    @Override
    public Float copyInner(Float content) {
        return content;
    }

    @Override
    public CreateStressCapacityCapability.ManaBotainaCapabilityProxy createProxy(@Nonnull IO io, @Nonnull
            BlockEntity tileEntity) {
        return new CreateStressCapacityCapability.ManaBotainaCapabilityProxy(tileEntity);
    }

    @Override
    public ContentWidget<? super Float> createContentWidget() {
        return new NumberContentWidget().setContentTexture(new TextTexture("SC", color)).setUnit("Stress");
    }

    @Override
    public BlockInfo[] getCandidates() {
        return new BlockInfo[]{
                BlockInfo.fromBlock(MbdComponents.COMPONENT_BLOCKS_REGISTRY.get(new ResourceLocation(Multiblocked.MODID, "create_input"))),
                BlockInfo.fromBlock(MbdComponents.COMPONENT_BLOCKS_REGISTRY.get(new ResourceLocation(Multiblocked.MODID, "create_output")))
        };
    }

    @Override
    public Float deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return jsonElement.getAsFloat();
    }

    @Override
    public JsonElement serialize(Float value, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(value);
    }

    public static class ManaBotainaCapabilityProxy extends CapabilityProxy<Float> {

        public ManaBotainaCapabilityProxy(BlockEntity tileEntity) {
            super(CreateStressCapacityCapability.CAP, tileEntity);
        }

        public CreateKineticSourceTileEntity getCapability() {
            return (CreateKineticSourceTileEntity) getTileEntity();
        }

        @Override
        protected List<Float> handleRecipeInner(IO io, Recipe recipe, List<Float> left, boolean simulate) {
            CreateKineticSourceTileEntity capability = getCapability();
            if (capability == null) return left;
            float sum = left.stream().reduce(0f, Float::sum);
            if (io == IO.IN && !capability.isGenerator()) {
                float capacity = Mth.abs(capability.getSpeed()) * capability.definition.stress;
                if (capacity > 0) {
                    sum = sum - capacity;
                }
            } else if (io == IO.OUT && capability.isGenerator()) {
                if (simulate) {
                    available = capability.setupWorkingCapacity(sum, true);
                }
                sum = sum - available;
            }
            return sum <= 0 ? null : Collections.singletonList(sum);
        }

        float available;

        @Override
        public void preWorking(ICapabilityProxyHolder holder, IO io, Recipe recipe) {
            CreateKineticSourceTileEntity capability = getCapability();
            if (capability == null) return;
            if (available > 0 && capability.isGenerator() && io == IO.OUT) {
                capability.setupWorkingCapacity(available, false);
            }
        }

        @Override
        public void postWorking(ICapabilityProxyHolder holder, IO io, Recipe recipe) {
            CreateKineticSourceTileEntity capability = getCapability();
            if (capability == null) return;
            if (capability.isGenerator() && io == IO.OUT) {
                capability.stopWorkingCapacity();
            }
        }

        float lastCapacity = -1;

        @Override
        protected boolean hasInnerChanged() {
            CreateKineticSourceTileEntity capability = getCapability();
            if (capability == null) return false;
            if (capability.isGenerator()) {
                return false;
            } else {
                float capacity = capability.getSpeed() * capability.definition.stress;
                if (lastCapacity == capacity) return false;
                lastCapacity = capacity;
                return true;
            }
        }
    }
}
