package com.lowdragmc.multiblocked.common.capability.widget;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.items.ItemStackHandler;

public class EntityContentWidget extends ContentWidget<EntityType<?>> {
    protected ItemStackHandler itemHandler;

    @Override
    protected void onContentUpdate() {
        if (itemHandler == null) {
            itemHandler = new ItemStackHandler();
            addWidget(new SlotWidget(itemHandler, 0, 1, 1, false, false).setDrawOverlay(false).setOnAddedTooltips((s, l) -> {
                if (chance < 1) {
                    l.add(chance == 0 ? new TranslatableComponent("multiblocked.gui.content.chance_0") : new TranslatableComponent("multiblocked.gui.content.chance_1", String.format("%.1f", chance * 100)));
                }
                if (perTick) {
                    l.add(new TranslatableComponent("multiblocked.gui.content.per_tick"));
                }
            }));
        }
        SpawnEggItem item = ForgeSpawnEggItem.fromEntityType(content);
        itemHandler.setStackInSlot(0, item == null ? ItemStack.EMPTY : item.getDefaultInstance());
    }

    @Override
    public EntityType<?> getJEIContent(Object content) {
       if (content instanceof ItemStack itemStack && itemStack.getItem() instanceof SpawnEggItem spawnEggItem) {
            return spawnEggItem.getType(null);
        }
        return EntityType.PIG;
    }

    @Override
    public Object getJEIIngredient(EntityType<?> content) {
        return itemHandler.getStackInSlot(0);
    }

    @Override
    public void openConfigurator(WidgetGroup dialog) {
        super.openConfigurator(dialog);
        int y = 25;
        TextFieldWidget type;
        dialog.addWidget(new LabelWidget(5, y + 3, "multiblocked.gui.label.entity_type"));
        dialog.addWidget(type = new TextFieldWidget(125 - 60, y, 60, 15,  null, string -> {
            content = EntityType.byString(string).orElse(EntityType.PIG);
            onContentUpdate();
        }).setResourceLocationOnly());
        if (content != null) {
            type.setCurrentString(content.getRegistryName().toString());
        }
    }
}
