package com.lowdragmc.multiblocked.common.capability;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.proxy.CapabilityProxy;
import com.lowdragmc.multiblocked.api.capability.trait.CapabilityTrait;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.serde.content.SerializerBigInteger;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import com.lowdragmc.multiblocked.common.capability.trait.EMCPlayerCapabilityTrait;
import com.lowdragmc.multiblocked.common.capability.widget.NumberContentWidget;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

public class EMCProjectECapability extends MultiblockCapability<BigInteger> {

    public static final EMCProjectECapability CAP = new EMCProjectECapability();

    protected EMCProjectECapability() {
        super("projecte_emc", 0xFFAC2D5E, SerializerBigInteger.INSTANCE);
    }

    @Override
    public BigInteger defaultContent() {
        return BigInteger.ONE;
    }

    @Override
    public boolean isBlockHasCapability(@Nonnull IO io, @Nonnull BlockEntity tileEntity) {
        return tileEntity instanceof ComponentTileEntity && ((ComponentTileEntity<?>) tileEntity).hasTrait(EMCProjectECapability.CAP);
    }

    @Override
    public BigInteger copyInner(BigInteger content) {
        return content;
    }

    @Override
    public CapabilityProxy<? extends BigInteger> createProxy(@Nonnull IO io, @Nonnull BlockEntity tileEntity) {
        return new EMCProjectECapabilityProxy(tileEntity);
    }

    @Override
    public BlockInfo[] getCandidates() {
        return MbdComponents.DEFINITION_REGISTRY
                .values()
                .stream()
                .filter(definition -> definition.traits.has(CAP.name))
                .map(definition -> BlockInfo.fromBlock(MbdComponents.COMPONENT_BLOCKS_REGISTRY.get(definition.location)))
                .toArray(BlockInfo[]::new);
    }

    @Override
    public BigInteger deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return json.getAsBigInteger();
    }

    @Override
    public JsonElement serialize(BigInteger src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src);
    }

    @Override
    public ContentWidget<? super BigInteger> createContentWidget() {
        return new NumberContentWidget().setContentTexture(new TextTexture("EMC", color)).setUnit("EMC");
    }

    @Override
    public boolean hasTrait() {
        return true;
    }

    @Override
    public CapabilityTrait createTrait() {
        return new EMCPlayerCapabilityTrait();
    }

    public static class EMCProjectECapabilityProxy extends CapabilityProxy<BigInteger> {

        public EMCProjectECapabilityProxy(BlockEntity tileEntity) {
            super(EMCProjectECapability.CAP, tileEntity);
        }

        public EMCPlayerCapabilityTrait getTrait() {
            BlockEntity te = getTileEntity();
            if (te instanceof ComponentTileEntity && ((ComponentTileEntity<?>) te).hasTrait(EMCProjectECapability.CAP)) {
                CapabilityTrait trait = ((ComponentTileEntity<?>) te).getTrait(EMCProjectECapability.CAP);
                if (trait instanceof EMCPlayerCapabilityTrait) {
                    return (EMCPlayerCapabilityTrait) trait;
                }
            }
            return null;
        }

        @Override
        protected List<BigInteger> handleRecipeInner(IO io, Recipe recipe, List<BigInteger> left, @Nullable String slotName, boolean simulate) {
            EMCPlayerCapabilityTrait trait = getTrait();
            if (trait == null) return left;
            BigInteger sum = left.stream().reduce(BigInteger.ZERO, BigInteger::add);
            sum = trait.updateEMC(io == IO.IN ? sum.negate() : sum, simulate);
            return sum.compareTo(BigInteger.ZERO) <= 0 ? null : Collections.singletonList(sum);
        }

        BigInteger lastEMC = BigInteger.ZERO;

        @Override
        protected boolean hasInnerChanged() {
            EMCPlayerCapabilityTrait trait = getTrait();
            if (trait == null) return false;
            IKnowledgeProvider capability = trait.getCapability();
            if (capability == null) return false;
            if (lastEMC.equals(capability.getEmc())) return false;
            lastEMC = capability.getEmc();
            return true;
        }
    }
}
