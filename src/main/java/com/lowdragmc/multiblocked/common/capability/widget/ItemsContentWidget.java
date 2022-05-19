package com.lowdragmc.multiblocked.common.capability.widget;


import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.PhantomSlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.SwitchWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.CycleItemStackHandler;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import com.lowdragmc.multiblocked.api.recipe.ItemsIngredient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.tags.ITagCollection;
import net.minecraft.tags.TagCollectionManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ItemsContentWidget extends ContentWidget<ItemsIngredient> {
    protected CycleItemStackHandler itemHandler;

    @Override
    protected void onContentUpdate() {
        List<List<ItemStack>> stacks = Collections.singletonList(Arrays.stream(content.ingredient.getItems()).map(stack -> {
            ItemStack copy = stack.copy();
            copy.setCount(content.getAmount());
            return copy;
        }).collect(Collectors.toList()));
        if (itemHandler != null) {
            itemHandler.updateStacks(stacks);
        } else {
            itemHandler = new CycleItemStackHandler(stacks);
            addWidget(new SlotWidget(itemHandler, 0, 1, 1, false, false).setDrawOverlay(false).setOnAddedTooltips((s, l)-> {
                if (chance < 1) {
                    l.add(chance == 0 ? new TranslationTextComponent("multiblocked.gui.content.chance_0") : new TranslationTextComponent("multiblocked.gui.content.chance_1", String.format("%.1f", chance * 100)));
                }
                if (perTick) {
                    l.add(new TranslationTextComponent("multiblocked.gui.content.per_tick"));
                }
            }));
        }
    }

    @Override
    public ItemsIngredient getJEIContent(Object content) {
        if (content instanceof ItemStack) {
            return new ItemsIngredient(Ingredient.of((ItemStack)content), this.content.getAmount());
        }
        return null;
    }

    @Override
    public Object getJEIIngredient(ItemsIngredient content) {
        return itemHandler.getStackInSlot(0);
    }

    @Override
    public void openConfigurator(WidgetGroup dialog) {
        super.openConfigurator(dialog);
        int x = 5;
        int y = 25;
        dialog.addWidget(new LabelWidget(5, y + 3, "multiblocked.gui.label.amount"));
        dialog.addWidget(new TextFieldWidget(125 - 60, y, 60, 15,  null, number -> {
            content = content.isTag() ? new ItemsIngredient(content.getTag(), Integer.parseInt(number)) : new ItemsIngredient(content.ingredient, Integer.parseInt(number));
            onContentUpdate();
        }).setNumbersOnly(1, Integer.MAX_VALUE).setCurrentString(content.getAmount()+""));

        TextFieldWidget tag;
        WidgetGroup groupOre = new WidgetGroup(x, y + 40, 120, 80);
        WidgetGroup groupIngredient = new WidgetGroup(x, y + 20, 120, 80);
        DraggableScrollableWidgetGroup container = new DraggableScrollableWidgetGroup(0, 20, 120, 50).setBackground(new ColorRectTexture(0xffaaaaaa));
        groupIngredient.addWidget(container);
        dialog.addWidget(groupIngredient);
        dialog.addWidget(groupOre);

        groupOre.addWidget(tag = new TextFieldWidget(30, 3, 90, 15,  () -> content.isTag() ? content.getTag() : "", null).setResourceLocationOnly());
        IItemHandlerModifiable handler;
        PhantomSlotWidget phantomSlotWidget = new PhantomSlotWidget(handler = new ItemStackHandler(1), 0, 0, 1).setClearSlotOnRightClick(false);
        groupOre.addWidget(phantomSlotWidget);
        phantomSlotWidget.setChangeListener(() -> {
            ItemStack newStack = handler.getStackInSlot(0);
            if (newStack.isEmpty()) return;
            ITagCollection<Item> tags = TagCollectionManager.getInstance().getItems();
            Collection<ResourceLocation> ids = tags.getMatchingTags(newStack.getItem());
            if (ids.size() > 0) {
                String tagString = ids.stream().findAny().get().toString();
                content = new ItemsIngredient(tagString, content.getAmount());
                tag.setCurrentString(tagString);
                phantomSlotWidget.setHoverTooltips(LocalizationUtils.format("multiblocked.gui.trait.item.ore_dict") + ": " + ids.stream().map(ResourceLocation::toString).reduce("", (a, b) -> a + "\n" + b));
            } else {
                content = new ItemsIngredient("", content.getAmount());
                tag.setCurrentString("");
                handler.setStackInSlot(0, ItemStack.EMPTY);
            }
            onContentUpdate();

        }).setBackgroundTexture(new ColorRectTexture(0xaf444444));
        tag.setTextResponder(tagS -> {
            content = new ItemsIngredient(tagS, content.getAmount());
            ItemStack[] matches = content.ingredient.getItems();
            handler.setStackInSlot(0, matches.length > 0 ? matches[0] : ItemStack.EMPTY);
            phantomSlotWidget.setHoverTooltips(LocalizationUtils.format("multiblocked.gui.trait.item.ore_dict") + ":\n"  + content.getTag());
            onContentUpdate();
        });
        tag.setHoverTooltips("multiblocked.gui.trait.item.ore_dic");
        dialog.addWidget(new SwitchWidget(x, y + 22, 50, 15, (cd, r)->{
            groupOre.setVisible(r);
            content = r ? new ItemsIngredient(tag.getCurrentString(), content.getAmount()) : new ItemsIngredient(content.ingredient, content.getAmount());
            groupIngredient.setVisible(!r);
            if (r) {
                ItemStack[] matches = content.ingredient.getItems();
                handler.setStackInSlot(0, matches.length > 0 ? matches[0] : ItemStack.EMPTY);
                phantomSlotWidget.setHoverTooltips("oreDict: \n" + content.getTag());
            } else {
                updateIngredientWidget(container);
            }
            onContentUpdate();
        }).setPressed(content.isTag()).setHoverBorderTexture(1, -1)
                .setBaseTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("tag (N)"))
                .setPressedTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("tag (Y)"))
                .setHoverTooltips("using tag dictionary"));

        groupIngredient.setVisible(!content.isTag());
        groupOre.setVisible(content.isTag());
        if (content.isTag()) {
            ItemStack[] matches = content.ingredient.getItems();
            handler.setStackInSlot(0, matches.length > 0 ? matches[0] : ItemStack.EMPTY);
            phantomSlotWidget.setHoverTooltips(LocalizationUtils.format("multiblocked.gui.trait.item.ore_dict") + ":\n"  + content.getTag());
        } else {
            updateIngredientWidget(container);
        }
        groupIngredient.addWidget(new LabelWidget(x + 50, 5, "multiblocked.gui.tips.settings"));
        groupIngredient.addWidget(new ButtonWidget(100, 0, 20, 20, cd -> {
            ItemStack[] stacks = content.ingredient.getItems();
            content = new ItemsIngredient(Ingredient.of(ArrayUtils.add(stacks, new ItemStack(Items.IRON_INGOT))), content.getAmount());
            updateIngredientWidget(container);
            onContentUpdate();
        }).setButtonTexture(new ResourceTexture("multiblocked:textures/gui/add.png")).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.trait.item.add"));
    }

    private void updateIngredientWidget(DraggableScrollableWidgetGroup container) {
        container.widgets.forEach(container::waitToRemoved);
        ItemStack[] matchingStacks = ArrayUtils.clone(content.ingredient.getItems());
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
                        content = new ItemsIngredient(Ingredient.of(matchingStacks), content.getAmount());
                        onContentUpdate();
                    }).setBackgroundTexture(new ColorRectTexture(0xaf444444)));
            handler.setStackInSlot(0, stack);
            container.addWidget(new ButtonWidget(x + 21, y + 1, 9, 9, cd -> {
                content = new ItemsIngredient(Ingredient.of(ArrayUtils.remove(matchingStacks, finalI)), content.getAmount());
                updateIngredientWidget(container);
                onContentUpdate();
            }).setButtonTexture(new ResourceTexture("multiblocked:textures/gui/remove.png")).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.tips.remove"));
        }
    }

}
