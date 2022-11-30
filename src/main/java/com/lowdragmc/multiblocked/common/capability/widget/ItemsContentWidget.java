package com.lowdragmc.multiblocked.common.capability.widget;

import com.lowdragmc.lowdraglib.LDLMod;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.CycleItemStackHandler;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import com.lowdragmc.multiblocked.api.recipe.ingredient.SizedIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ItemsContentWidget extends ContentWidget<Ingredient> {
    protected CycleItemStackHandler itemHandler;
    protected boolean isDurability;

    public ItemsContentWidget(boolean isDurability) {
        this.isDurability = isDurability;
    }

    public ItemsContentWidget() {
        this(false);
    }

    @Override
    protected void onContentUpdate() {
        List<List<ItemStack>> stacks = Collections.singletonList(Arrays.stream(content.getItems()).map(ItemStack::copy).collect(Collectors.toList()));
        if (itemHandler != null) {
            itemHandler.updateStacks(stacks);
        } else {
            itemHandler = new CycleItemStackHandler(stacks);
            addWidget(new SlotWidget(itemHandler, 0, 1, 1, false, false).setDrawOverlay(false).setOnAddedTooltips((s, l) -> {
                if (chance < 1) {
                    l.add(chance == 0 ? new TranslatableComponent("multiblocked.gui.content.chance_0") : new TranslatableComponent("multiblocked.gui.content.chance_1", String.format("%.1f", chance * 100) + "%"));
                }
                if (perTick) {
                    l.add(new TranslatableComponent("multiblocked.gui.content.per_tick"));
                }
                if (isDurability) {
                    l.add(new TranslatableComponent("multiblocked.gui.content.durability"));
                }
            }));
            if (isDurability) {
                addWidget(new ImageWidget(1, 5, 18, 9, new TextTexture("D", 0xFFff5555)));
            }
        }
    }

    @Override
    public Ingredient getJEIContent(Object content) {
        if (content instanceof Ingredient) {
            return (Ingredient) content;
        } else if (content instanceof ItemStack itemStack) {
            ItemStack[] items = this.content.getItems();
            return new SizedIngredient(Ingredient.of(itemStack), items.length > 0 ? items[0].getCount() : 1);
        }
        return null;
    }

    @Override
    public Object getJEIIngredient(Ingredient content) {
        if (LDLMod.isReiLoaded()) {
            return EntryIngredients.ofIngredient(content);
        }
        return itemHandler.getStackInSlot(0);
    }
    
    private SizedIngredient getAsContent() {
        return (SizedIngredient)content;
    }

    @Override
    public void openConfigurator(WidgetGroup dialog) {
        super.openConfigurator(dialog);
        int x = 5;
        int y = 25;
        if (content instanceof SizedIngredient) {
            TextFieldWidget count;
            dialog.addWidget(new LabelWidget(5, y + 3, "multiblocked.gui.label.amount"));
            dialog.addWidget(count = new TextFieldWidget(125 - 60, y, 60, 15,  null, number -> {
                content = getAsContent().isTag() ? new SizedIngredient(getAsContent().getTag(), Integer.parseInt(number)) : new SizedIngredient(getAsContent().getInner(), Integer.parseInt(number));
                onContentUpdate();
            }).setNumbersOnly(1, Integer.MAX_VALUE).setCurrentString(getAsContent().getAmount()+""));

            TextFieldWidget tag;
            WidgetGroup groupOre = new WidgetGroup(x, y + 40, 120, 80);
            WidgetGroup groupIngredient = new WidgetGroup(x, y + 20, 120, 80);
            DraggableScrollableWidgetGroup container = new DraggableScrollableWidgetGroup(0, 20, 120, 50).setBackground(new ColorRectTexture(0xffaaaaaa));
            groupIngredient.addWidget(container);
            dialog.addWidget(groupIngredient);
            dialog.addWidget(groupOre);

            groupOre.addWidget(tag = new TextFieldWidget(30, 3, 90, 15,  () -> getAsContent().isTag() ? getAsContent().getTag() : "", null).setResourceLocationOnly());
            IItemHandlerModifiable handler;
            PhantomSlotWidget phantomSlotWidget = new PhantomSlotWidget(handler = new ItemStackHandler(1), 0, 0, 1).setClearSlotOnRightClick(false);
            groupOre.addWidget(phantomSlotWidget);
            phantomSlotWidget.setChangeListener(() -> {
                ItemStack newStack = handler.getStackInSlot(0);
                if (newStack.isEmpty()) return;
                Collection<ResourceLocation>
                        ids = newStack.getTags().map(TagKey::location).toList();
                if (ids.size() > 0) {
                    String tagString = ids.stream().findAny().get().toString();
                    content = new SizedIngredient(tagString, getAsContent().getAmount());
                    tag.setCurrentString(tagString);
                    phantomSlotWidget.setHoverTooltips(LocalizationUtils.format("multiblocked.gui.trait.item.ore_dict") + ": " + ids.stream().map(
                            ResourceLocation::toString).reduce("", (a, b) -> a + "\n" + b));
                } else {
                    content = new SizedIngredient("", getAsContent().getAmount());
                    tag.setCurrentString("");
                    handler.setStackInSlot(0, ItemStack.EMPTY);
                }
                onContentUpdate();

            }).setBackgroundTexture(new ColorRectTexture(0xaf444444));
            tag.setTextResponder(tagS -> {
                content = new SizedIngredient(tagS, getAsContent().getAmount());
                ItemStack[] matches = getAsContent().getInner().getItems();
                handler.setStackInSlot(0, matches.length > 0 ? matches[0] : ItemStack.EMPTY);
                phantomSlotWidget.setHoverTooltips(LocalizationUtils.format("multiblocked.gui.trait.item.ore_dict") + ":\n"  + getAsContent().getTag());
                onContentUpdate();
            });
            tag.setHoverTooltips("multiblocked.gui.trait.item.ore_dict");
            dialog.addWidget(new SwitchWidget(x, y + 22, 50, 15, (cd, r)->{
                groupOre.setVisible(r);
                content = r ? new SizedIngredient(tag.getCurrentString(), getAsContent().getAmount()) : new SizedIngredient(getAsContent().getInner(), getAsContent().getAmount());
                groupIngredient.setVisible(!r);
                if (r) {
                    ItemStack[] matches = getAsContent().getInner().getItems();
                    handler.setStackInSlot(0, matches.length > 0 ? matches[0] : ItemStack.EMPTY);
                    phantomSlotWidget.setHoverTooltips("oreDict: \n" + getAsContent().getTag());
                } else {
                    updateIngredientWidget(container, count);
                }
                onContentUpdate();
            }).setPressed(getAsContent().isTag()).setHoverBorderTexture(1, -1)
                    .setBaseTexture(
                            ResourceBorderTexture.BUTTON_COMMON, new TextTexture("tag (N)"))
                    .setPressedTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("tag (Y)"))
                    .setHoverTooltips("using tag dictionary"));

            groupIngredient.setVisible(!getAsContent().isTag());
            groupOre.setVisible(getAsContent().isTag());
            if (getAsContent().isTag()) {
                ItemStack[] matches = getAsContent().getInner().getItems();
                handler.setStackInSlot(0, matches.length > 0 ? matches[0] : ItemStack.EMPTY);
                phantomSlotWidget.setHoverTooltips(LocalizationUtils.format("multiblocked.gui.trait.item.ore_dict") + ":\n"  + getAsContent().getTag());
            } else {
                updateIngredientWidget(container, count);
            }
            groupIngredient.addWidget(new LabelWidget(x + 50, 5, "multiblocked.gui.tips.settings"));
            groupIngredient.addWidget(new ButtonWidget(100, 0, 20, 20, cd -> {
                ItemStack[] stacks = getAsContent().getInner().getItems();
                content = new SizedIngredient(Ingredient.of(ArrayUtils.add(stacks, new ItemStack(Items.IRON_INGOT))), getAsContent().getAmount());
                updateIngredientWidget(container, count);
                onContentUpdate();
            }).setButtonTexture(new ResourceTexture("multiblocked:textures/gui/add.png")).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.trait.item.add"));
        } else {
            dialog.addWidget(new LabelWidget(5, y + 3, "multiblocked.gui.label.unsupported_ingredient"));
        }
  }

    private void updateIngredientWidget(DraggableScrollableWidgetGroup container, TextFieldWidget count) {
        container.widgets.forEach(container::waitToRemoved);
        ItemStack[] matchingStacks = ArrayUtils.clone(content.getItems());
        for (int i = 0; i < matchingStacks.length; i++) {
            ItemStack stack = matchingStacks[i];
            IItemHandlerModifiable handler;
            int finalI = i;
            int x = (i % 4) * 30;
            int y = (i / 4) * 20;
            container.addWidget(new PhantomSlotWidget(handler = new ItemStackHandler(1), 0, x + 1, y + 1)
                    .setClearSlotOnRightClick(false)
                    .setChangeListener(() -> {
                        ItemStack newStack = handler.getStackInSlot(0);
                        matchingStacks[finalI] = newStack;
                        content = new SizedIngredient(Ingredient.of(matchingStacks), Integer.parseInt(count.getCurrentString()));
                        onContentUpdate();
                        updateIngredientWidget(container, count);
                    }).setBackgroundTexture(new ColorRectTexture(0xaf444444)));
            handler.setStackInSlot(0, stack);
            container.addWidget(new ButtonWidget(x + 21, y + 1, 9, 9, cd -> {
                content = new SizedIngredient(Ingredient.of((Arrays.stream(ArrayUtils.remove(matchingStacks, finalI)))), Integer.parseInt(count.getCurrentString()));
                onContentUpdate();
                updateIngredientWidget(container, count);
            }).setButtonTexture(new ResourceTexture("multiblocked:textures/gui/remove.png")).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.tips.remove"));
        }
    }

}
