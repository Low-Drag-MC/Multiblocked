package com.lowdragmc.multiblocked.common.capability.widget;


import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.CycleItemStackHandler;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
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

    @Override
    protected void onContentUpdate() {
        List<List<ItemStack>> stacks = Collections.singletonList(Arrays.stream(content.getItems()).map(ItemStack::copy).collect(Collectors.toList()));
        if (itemHandler != null) {
            itemHandler.updateStacks(stacks);
        } else {
            itemHandler = new CycleItemStackHandler(stacks);
            addWidget(new SlotWidget(itemHandler, 0, 1, 1, false, false).setDrawOverlay(false).setOnAddedTooltips((s, l) -> {
                if (chance < 1) {
                    l.add(chance == 0 ? new TranslatableComponent("multiblocked.gui.content.chance_0") : new TranslatableComponent("multiblocked.gui.content.chance_1", String.format("%.1f", chance * 100)));
                }
                if (perTick) {
                    l.add(new TranslatableComponent("multiblocked.gui.content.per_tick"));
                }
            }));
        }
    }

    @Override
    public Ingredient getJEIContent(Object content) {
        return (Ingredient) content;
    }

    @Override
    public Object getJEIIngredient(Ingredient content) {
        return itemHandler.getStackInSlot(0);
    }

    @Override
    public void openConfigurator(WidgetGroup dialog) {
        super.openConfigurator(dialog);
        int x = 5;
        int y = 25;
        dialog.addWidget(new LabelWidget(5, y + 3, "multiblocked.gui.label.amount"));

        WidgetGroup groupOre = new WidgetGroup(x, y + 40, 120, 80);
        WidgetGroup groupIngredient = new WidgetGroup(x, y + 20, 120, 80);
        DraggableScrollableWidgetGroup container = new DraggableScrollableWidgetGroup(0, 20, 120, 50).setBackground(new ColorRectTexture(0xffaaaaaa));
        groupIngredient.addWidget(container);
        dialog.addWidget(groupIngredient);
        dialog.addWidget(groupOre);


        IItemHandlerModifiable handler;
        PhantomSlotWidget phantomSlotWidget = new PhantomSlotWidget(handler = new ItemStackHandler(1), 0, 0, 1).setClearSlotOnRightClick(false);
        groupOre.addWidget(phantomSlotWidget);
        phantomSlotWidget.setChangeListener(() -> {
            ItemStack newStack = handler.getStackInSlot(0);
            if (newStack.isEmpty()) return;
            Collection<ResourceLocation> ids = newStack.getTags().map(TagKey::location).toList();
            if (ids.size() > 0) {
                phantomSlotWidget.setHoverTooltips(LocalizationUtils.format("multiblocked.gui.trait.item.ore_dict") + ": " + ids.stream().map(
                        ResourceLocation::toString).reduce("", (a, b) -> a + "\n" + b));
            } else {
                handler.setStackInSlot(0, ItemStack.EMPTY);
            }
            onContentUpdate();

        }).setBackgroundTexture(new ColorRectTexture(0xaf444444));

        dialog.addWidget(new SwitchWidget(x, y + 22, 50, 15, (cd, r) -> {
            groupOre.setVisible(r);
            groupIngredient.setVisible(!r);
            if (r) {
                ItemStack[] matches = content.getItems();
                handler.setStackInSlot(0, matches.length > 0 ? matches[0] : ItemStack.EMPTY);
            } else {
                updateIngredientWidget(container);
            }
            onContentUpdate();
        }).setHoverBorderTexture(1, -1)
                .setBaseTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("tag (N)"))
                .setPressedTexture(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("tag (Y)"))
                .setHoverTooltips("using tag dictionary"));
        updateIngredientWidget(container);
        groupIngredient.addWidget(new LabelWidget(x + 50, 5, "multiblocked.gui.tips.settings"));
        groupIngredient.addWidget(new ButtonWidget(100, 0, 20, 20, cd -> {
            updateIngredientWidget(container);
            onContentUpdate();
        }).setButtonTexture(new ResourceTexture("multiblocked:textures/gui/add.png")).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.trait.item.add"));
    }

    private void updateIngredientWidget(DraggableScrollableWidgetGroup container) {
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
                        onContentUpdate();
                    }).setBackgroundTexture(new ColorRectTexture(0xaf444444)));
            handler.setStackInSlot(0, stack);
            container.addWidget(new ButtonWidget(x + 21, y + 1, 9, 9, cd -> {
                updateIngredientWidget(container);
                onContentUpdate();
            }).setButtonTexture(new ResourceTexture("multiblocked:textures/gui/remove.png")).setHoverBorderTexture(1, -1).setHoverTooltips("multiblocked.gui.tips.remove"));
        }
    }

}
