package com.lowdragmc.multiblocked.common.capability;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.CycleItemStackHandler;
import com.lowdragmc.multiblocked.Multiblocked;
import com.lowdragmc.multiblocked.api.capability.IO;
import com.lowdragmc.multiblocked.api.capability.MultiblockCapability;
import com.lowdragmc.multiblocked.api.capability.proxy.CapCapabilityProxy;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import com.lowdragmc.multiblocked.api.recipe.Content;
import com.lowdragmc.multiblocked.api.recipe.ContentModifier;
import com.lowdragmc.multiblocked.api.recipe.Recipe;
import com.lowdragmc.multiblocked.api.recipe.ingredient.SizedIngredient;
import com.lowdragmc.multiblocked.api.recipe.serde.content.SerializerIngredient;
import com.lowdragmc.multiblocked.api.registry.MbdComponents;
import com.lowdragmc.multiblocked.common.capability.widget.ItemsContentWidget;
import com.lowdragmc.multiblocked.core.mixins.NBTIngredientMixin;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.crafting.NBTIngredient;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ItemDurabilityMultiblockCapability extends MultiblockCapability<Ingredient> {
    public static final ItemDurabilityMultiblockCapability CAP = new ItemDurabilityMultiblockCapability();

    private ItemDurabilityMultiblockCapability() {
        super("item_durability", 0xFFD96106, SerializerIngredient.INSTANCE);
    }

    @Override
    public Ingredient defaultContent() {
        return new SizedIngredient(Ingredient.of(Items.FLINT_AND_STEEL), 1);
    }

    @Override
    public boolean isBlockHasCapability(@Nonnull IO io, @Nonnull
            BlockEntity tileEntity) {
        return !getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, tileEntity).isEmpty();
    }

    @Override
    public Ingredient copyInner(Ingredient content) {
        //Use network serialization to reduce overhead
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        content.toNetwork(buf);
        return Ingredient.fromNetwork(buf);
    }

    @Override
    public Ingredient copyWithModifier(Ingredient content, ContentModifier modifier) {
        Ingredient copy = copyInner(content);
        return copy instanceof SizedIngredient sizedIngredient ? new SizedIngredient(copy, modifier.apply(sizedIngredient.getAmount()).intValue()) : copy;
    }

    @Override
    public ItemCapabilityProxy createProxy(@Nonnull IO io, @Nonnull
            BlockEntity tileEntity) {
        return new ItemCapabilityProxy(tileEntity);
    }

    @Override
    public ContentWidget<? super Ingredient> createContentWidget() {
        return new ItemsContentWidget(true);
    }

    @Override
    public void handleRecipeUI(Widget widget, Content in, IngredientIO ingredientIO) {
        if (widget instanceof SlotWidget slotWidget && in.content instanceof Ingredient ingredient) {
            slotWidget.setHandlerSlot(new CycleItemStackHandler(List.of(Arrays.stream(ingredient.getItems()).toList())), 0)
                    .setIngredientIO(ingredientIO)
                    .setCanTakeItems(false)
                    .setCanPutItems(false);
        }
    }

    @Override
    public BlockInfo[] getCandidates() {
        return new BlockInfo[]{
                BlockInfo.fromBlockState(Blocks.CHEST.defaultBlockState()),
                BlockInfo.fromBlockState(Blocks.WHITE_SHULKER_BOX.defaultBlockState()),
                BlockInfo.fromBlockState(Blocks.TRAPPED_CHEST.defaultBlockState()),
                BlockInfo.fromBlock(MbdComponents.COMPONENT_BLOCKS_REGISTRY.get(new ResourceLocation(Multiblocked.MODID, "item_input"))),
                BlockInfo.fromBlock(MbdComponents.COMPONENT_BLOCKS_REGISTRY.get(new ResourceLocation(Multiblocked.MODID, "item_output")))
        };
    }

    public static class ItemCapabilityProxy extends CapCapabilityProxy<IItemHandler, Ingredient> {

        public ItemCapabilityProxy(BlockEntity tileEntity) {
            super(ItemDurabilityMultiblockCapability.CAP, tileEntity, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
        }

        @Override
        public void handleProxyMbdUI(ModularUI modularUI) {
            if (slots != null && !slots.isEmpty()) {
                for (String slotName : slots) {
                    for (Widget widget : modularUI.getWidgetsById("^%s_[0-9]+$".formatted(slotName))) {
                        if (widget instanceof SlotWidget slotWidget) {
                            int index = Integer.parseInt(slotWidget.getId().split(slotName + "_")[1]);
                            IItemHandler capability = getCapability(slotName);
                            if (capability.getSlots() > index) {
                                slotWidget.setHandlerSlot(capability, index);
                            }
                        }
                    }
                }
            }
        }

        @Override
        protected List<Ingredient> handleRecipeInner(IO io, Recipe recipe, List<Ingredient> left, @Nullable String slotName, boolean simulate) {
            IItemHandler capability = getCapability(slotName);
            if (capability == null) return left;
            Iterator<Ingredient> iterator = left.iterator();
            if (io == IO.IN) {
                while (iterator.hasNext()) {
                    Ingredient ingredient = iterator.next();
                    SLOT_LOOKUP:
                    for (int i = 0; i < capability.getSlots(); i++) {
                        ItemStack itemStack = capability.getStackInSlot(i);
                        //Does not look like a good implementation, but I think it's at least equal to vanilla Ingredient::test
                        if (ingredient.test(itemStack)) {
                            ItemStack[] ingredientStacks = ingredient.getItems();
                            for (ItemStack ingredientStack : ingredientStacks) {
                                if (ingredientStack.is(itemStack.getItem())) {
                                    if (itemStack.getDamageValue() < itemStack.getMaxDamage()) {
                                        int cost = Math.min(itemStack.getMaxDamage() - itemStack.getDamageValue(), ingredientStack.getCount());
                                        if (!itemStack.isDamageableItem()) {
                                            ingredientStack.setCount(0);
                                        } else if (!simulate) {
                                            ItemStack extracted = capability.extractItem(i, itemStack.getCount(), false);
                                            if (extracted.isEmpty()) cost = 0;
                                            extracted.setDamageValue(itemStack.getDamageValue() + cost);
                                            capability.insertItem(i, extracted, false);
                                        } else {
                                            ItemStack extracted = capability.extractItem(i, itemStack.getCount(), true);
                                            if (extracted.isEmpty()) cost = 0;
                                        }
                                        ingredientStack.setCount(ingredientStack.getCount() - cost);
                                    }
                                    if (ingredientStack.isEmpty()) {
                                        iterator.remove();
                                        break SLOT_LOOKUP;
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (io == IO.OUT) {
                while (iterator.hasNext()) {
                    Ingredient ingredient = iterator.next();
                    ItemStack output = ingredient instanceof NBTIngredient nbtIngredient ? ((NBTIngredientMixin) nbtIngredient).getStack() : ingredient.getItems()[0];
                    for (int i = 0; i < capability.getSlots(); i++) {
                        ItemStack itemStack = capability.getStackInSlot(i);
                        if (itemStack.getDamageValue() > 0) {
                            int cost = Math.min(itemStack.getDamageValue(), output.getCount());
                            if (!simulate) {
                                // TODO UN SAFE
                                ItemStack extracted = capability.extractItem(i, itemStack.getCount(), false);
                                if (extracted.isEmpty()) cost = 0;
                                extracted.setDamageValue(itemStack.getDamageValue() - cost);
                                capability.insertItem(i, extracted, false);
                            } else {
                                ItemStack extracted = capability.extractItem(i, itemStack.getCount(), true);
                                if (extracted.isEmpty()) cost = 0;
                            }
                            output.setCount(output.getCount() - cost);
                        }
                        if (output.isEmpty()) break;
                    }
                    if (output.isEmpty()) iterator.remove();
                }
            }
            return left.isEmpty() ? null : left;
        }

        ItemStack[] lastStacks = new ItemStack[0];
        int[] limits = new int[0];

        @Override
        protected boolean hasInnerChanged() {
            IItemHandler capability = getCapability(null);
            if (capability == null) return false;
            boolean same = true;
            if (lastStacks.length == capability.getSlots()) {
                for (int i = 0; i < capability.getSlots(); i++) {
                    ItemStack content = capability.getStackInSlot(i);
                    ItemStack lastContent = lastStacks[i];
                    if (lastContent == null) {
                        same = false;
                        break;
                    } else if (lastContent.isEmpty() && content.isEmpty()) {

                    } else if (!content.equals(lastContent, true)) {
                        same = false;
                        break;
                    }
                    int cap = capability.getSlotLimit(i);
                    int lastCap = limits[i];
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
            lastStacks = new ItemStack[capability.getSlots()];
            limits = new int[capability.getSlots()];
            for (int i = 0; i < capability.getSlots(); i++) {
                lastStacks[i] = capability.getStackInSlot(i).copy();
                limits[i] = capability.getSlotLimit(i);
            }
            return true;
        }
    }
}
