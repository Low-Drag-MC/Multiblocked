package com.lowdragmc.multiblocked.common.capability;


import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.TankWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.CycleItemStackHandler;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.proxy.CapCapabilityProxy;
import com.lowdragmc.multiblocked.api.capability.trait.CapabilityTrait;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import com.lowdragmc.multiblocked.api.recipe.Content;
import com.lowdragmc.multiblocked.api.recipe.ContentModifier;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.serde.content.SerializerFluidStack;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.common.capability.trait.FluidCapabilityTrait;
import com.lowdragmc.multiblocked.common.capability.widget.FluidContentWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class FluidMultiblockCapability extends MultiblockCapability<FluidStack> {
    public static final FluidMultiblockCapability CAP = new FluidMultiblockCapability();

    private FluidMultiblockCapability() {
        super("fluid", 0xFF3C70EE, SerializerFluidStack.INSTANCE);
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
    public FluidStack copyWithModifier(FluidStack content, ContentModifier modifier) {
        FluidStack copy = content.copy();
        copy.setAmount(modifier.apply(copy.getAmount()).intValue());
        return copy;
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
    public void handleRecipeUI(Widget widget, Content in, IngredientIO ingredientIO) {
        if (widget instanceof TankWidget tankWidget && in.content instanceof FluidStack fluidStack) {
            var fluidTank = new FluidTank(fluidStack.getAmount());
            fluidTank.fill(fluidStack.copy(), IFluidHandler.FluidAction.EXECUTE);
            tankWidget.setFluidTank(fluidTank)
                    .setIngredientIO(ingredientIO)
                    .setAllowClickDrained(false)
                    .setAllowClickFilled(false);
        }
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
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        list.add(BlockInfo.fromBlock(MbdComponents.COMPONENT_BLOCKS_REGISTRY.get(new ResourceLocation(Multiblocked.MODID, "fluid_input"))));
        list.add(BlockInfo.fromBlock(MbdComponents.COMPONENT_BLOCKS_REGISTRY.get(new ResourceLocation(Multiblocked.MODID, "fluid_output"))));
        return list.toArray(new BlockInfo[0]);
    }

    public static class FluidCapabilityProxy extends CapCapabilityProxy<IFluidHandler, FluidStack> {

        public FluidCapabilityProxy(BlockEntity tileEntity) {
            super(FluidMultiblockCapability.CAP, tileEntity, CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY);
        }

        @Override
        public void handleProxyMbdUI(ModularUI modularUI) {
            if (slots != null && !slots.isEmpty()) {
                for (String slotName : slots) {
                    for (Widget widget : modularUI.getWidgetsById("^%s_[0-9]+$".formatted(slotName))) {
                        if (widget instanceof TankWidget tankWidget) {
                            int index = Integer.parseInt(tankWidget.getId().split(slotName + "_")[1]);
                            var capability = getCapability(slotName);
                            if (capability.getTanks() > index) {
                                tankWidget.setFluidTank(new IFluidTank() {
                                    @NotNull
                                    @Override
                                    public FluidStack getFluid() {
                                        return capability.getFluidInTank(index);
                                    }

                                    @Override
                                    public int getFluidAmount() {
                                        return getFluid().getAmount();
                                    }

                                    @Override
                                    public int getCapacity() {
                                        return capability.getTankCapacity(index);
                                    }

                                    @Override
                                    public boolean isFluidValid(FluidStack stack) {
                                        return capability.isFluidValid(index, stack);
                                    }

                                    @Override
                                    public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
                                        return capability.fill(resource, action);
                                    }

                                    @NotNull
                                    @Override
                                    public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
                                        return capability.drain(maxDrain, action);
                                    }

                                    @NotNull
                                    @Override
                                    public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
                                        return capability.drain(resource, action);
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }

        @Override
        protected List<FluidStack> handleRecipeInner(IO io, Recipe recipe, List<FluidStack> left, @Nullable String slotName, boolean simulate) {
            IFluidHandler capability = getCapability(slotName);
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
            } else if (io == IO.OUT) {
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
            IFluidHandler capability = getCapability(null);
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
