package com.lowdragmc.multiblocked.common.capability.widget;

import com.lowdragmc.lowdraglib.LDLMod;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.multiblocked.api.gui.recipe.ContentWidget;
import com.lowdragmc.multiblocked.api.recipe.ingredient.EntityIngredient;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.items.ItemStackHandler;

public class EntityContentWidget extends ContentWidget<EntityIngredient> {
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
        if (content.isEntityItem()) {
            itemHandler.setStackInSlot(0, content.getEntityItem());
        } else {
            SpawnEggItem item = ForgeSpawnEggItem.fromEntityType(content.type);
            itemHandler.setStackInSlot(0, item == null ? ItemStack.EMPTY : item.getDefaultInstance());
        }
    }

    @Override
    public EntityIngredient getJEIContent(Object content) {
        EntityIngredient ingredient = new EntityIngredient();
        if (content instanceof ItemStack itemStack) {
            if (itemStack.getItem() instanceof SpawnEggItem spawnEggItem) {
                ingredient.type = spawnEggItem.getType(null);
            } else {
                ingredient.type = EntityType.ITEM;
                ingredient.tag = new CompoundTag();
                CompoundTag entityTag = new CompoundTag();
                ingredient.tag.put("EntityTag", entityTag);
                entityTag.put("Item", itemStack.save(new CompoundTag()));
            }
        }
        return ingredient;
    }

    @Override
    public Object getJEIIngredient(EntityIngredient content) {
        if (itemHandler.getStackInSlot(0).isEmpty()) return null;
        if (LDLMod.isReiLoaded()) {
            return EntryStacks.of(itemHandler.getStackInSlot(0));
        }
        return itemHandler.getStackInSlot(0);
    }

    @Override
    public void openConfigurator(WidgetGroup dialog) {
        super.openConfigurator(dialog);
        int y = 25;
        TextFieldWidget type, tag;
        dialog.addWidget(new LabelWidget(5, y + 3, "multiblocked.gui.label.entity_type"));
        dialog.addWidget(type = new TextFieldWidget(125 - 60, y, 60, 15,  null, string -> {
            content.type = EntityType.byString(string).orElse(EntityType.PIG);
            onContentUpdate();
        }).setResourceLocationOnly());
        dialog.addWidget(new LabelWidget(5, y + 23, "multiblocked.gui.label.entity_tag"));
        dialog.addWidget(tag = new TextFieldWidget(125 - 60, y + 20, 60, 15,  null, string -> {
            try {
                content.tag = TagParser.parseTag(string);
                onContentUpdate();
            } catch (CommandSyntaxException ignored) {
            }
        }));
        if (content != null) {
            type.setCurrentString(content.type.getRegistryName().toString());
            if (content.tag != null) {
                tag.setCurrentString(content.tag.toString());
            }
        }
    }
}
