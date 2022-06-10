package com.lowdragmc.multiblocked.common.capability;


import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.lowdragmc.lowdraglib.json.FluidStackTypeAdapter;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.proxy.CapCapabilityProxy;
import com.lowdragmc.multiblocked.api.capability.trait.CapabilityTrait;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import com.lowdragmc.multiblocked.api.kubejs.MultiblockedJSPlugin;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.common.capability.trait.FluidCapabilityTrait;
import com.lowdragmc.multiblocked.common.capability.widget.FluidContentWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FluidMultiblockCapability extends MultiblockCapability<FluidStack> {
    public static final FluidMultiblockCapability CAP = new FluidMultiblockCapability();

    private  FluidMultiblockCapability() {
        super("fluid", 0xFF3C70EE);
    }

    @Override
    public FluidStack defaultContent() {
        return new FluidStack(Fluids.LAVA.getSource(), 1000);
    }

    @Override
    public boolean isBlockHasCapability(@Nonnull IO io, @Nonnull
    BlockEntity tileEntity) {
        return !getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, tileEntity).isEmpty();
    }

    @Override
    public FluidStack copyInner(FluidStack content) {
        return content.copy();
    }


    @Override
    public FluidCapabilityProxy createProxy(@Nonnull IO io, @Nonnull
    BlockEntity tileEntity) {
        return new FluidCapabilityProxy(tileEntity);
    }

    @Override
    public ContentWidget<? super FluidStack> createContentWidget() {
        return new FluidContentWidget();
    }

    @Override
    public boolean hasTrait() {
        return true;
    }

    @Override
    public CapabilityTrait createTrait() {
        return new FluidCapabilityTrait();
    }

    @Override
    public BlockInfo[] getCandidates() {
        List<BlockInfo> list = new ArrayList<>();
        for (Block block : ForgeRegistries.BLOCKS.getValues()) {
            if (block.getRegistryName() != null) {
                String path = block.getRegistryName().getPath();
                if (path.contains("tank") || path.contains("fluid") || path.contains("liquid")) {
                    try {
                        if (block instanceof EntityBlock entityBlock) {
                            BlockEntity tileEntity = entityBlock.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
                            if (tileEntity != null && isBlockHasCapability(IO.BOTH, tileEntity)) {
                                list.add(new BlockInfo(block.defaultBlockState(), true));
                            }
                        }
                    } catch (Throwable ignored) { }
                }
            }
        }
        list.add(BlockInfo.fromBlock(MbdComponents.COMPONENT_BLOCKS_REGISTRY.get(new ResourceLocation(Multiblocked.MODID, "fluid_input"))));
        list.add(BlockInfo.fromBlock(MbdComponents.COMPONENT_BLOCKS_REGISTRY.get(new ResourceLocation(Multiblocked.MODID, "fluid_output"))));
        return list.toArray(new BlockInfo[0]);
    }

    @Override
    public FluidStack of(Object o) {
        if (o instanceof FluidStack) {
            return ((FluidStack) o).copy();
        }
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return FluidStackTypeAdapter.INSTANCE.deserialize(jsonElement, type, jsonDeserializationContext);
    }

    @Override
    public JsonElement serialize(FluidStack fluidStack, Type type, JsonSerializationContext jsonSerializationContext) {
        return FluidStackTypeAdapter.INSTANCE.serialize(fluidStack, type, jsonSerializationContext);
    }

    public static class FluidCapabilityProxy extends CapCapabilityProxy<IFluidHandler, FluidStack> {

        public FluidCapabilityProxy(BlockEntity tileEntity) {
            super(FluidMultiblockCapability.CAP, tileEntity, CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY);
        }

        @Override
        protected List<FluidStack> handleRecipeInner(IO io, Recipe recipe, List<FluidStack> left, boolean simulate) {
            IFluidHandler capability = getCapability();
            if (capability == null) return left;
            Iterator<FluidStack> iterator = left.iterator();
            if (io == IO.IN) {
                while (iterator.hasNext()) {
                    FluidStack fluidStack = iterator.next();
                    boolean found = false;
                    for (int i = 0; i < capability.getTanks(); i++) {
                        FluidStack stored = capability.getFluidInTank(i);
                        if (!stored.isFluidEqual(fluidStack)) {
                            continue;
                        }
                        found = true;
                    }
                    if (!found) continue;
                    FluidStack drained = capability.drain(fluidStack.copy(), simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
                    fluidStack.setAmount(fluidStack.getAmount() - drained.getAmount());
                    if (fluidStack.getAmount() <= 0) {
                        iterator.remove();
                    }
                }
            } else if (io == IO.OUT){
                while (iterator.hasNext()) {
                    FluidStack fluidStack = iterator.next();
                    int filled = capability.fill(fluidStack.copy(), simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
                    fluidStack.setAmount(fluidStack.getAmount() - filled);
                    if (fluidStack.getAmount() <= 0) {
                        iterator.remove();
                    }
                }
            }
            return left.isEmpty() ? null : left;
        }

        FluidStack[] lastContents = new FluidStack[0];
        int[] lastCaps = new int[0];

        @Override
        protected boolean hasInnerChanged() {
            IFluidHandler capability = getCapability();
            if (capability == null) return false;
            boolean same = true;
            if (capability.getTanks() == lastContents.length) {
                for (int i = 0; i < capability.getTanks(); i++) {
                    FluidStack content = capability.getFluidInTank(i);
                    FluidStack lastContent = lastContents[i];
                    if (lastContent == null) {
                        same = false;
                        break;
                    } else if (!content.isFluidEqual(lastContent) || content.getAmount() != lastContent.getAmount()) {
                        same = false;
                        break;
                    }
                    int cap = capability.getTankCapacity(i);
                    int lastCap = lastCaps[i];
                    if (cap != lastCap) {
                        same = false;
                        break;
                    }
                }
            } else {
                same = false;
            }

            if (same) {
                return false;
            }
            lastContents = new FluidStack[capability.getTanks()];
            lastCaps = new int[lastContents.length];
            for (int i = 0; i < lastContents.length; i++) {
                FluidStack content = capability.getFluidInTank(i);
                lastContents[i] = content.copy();
                lastCaps[i] = capability.getTankCapacity(i);
            }
            return true;
        }
    }
}
