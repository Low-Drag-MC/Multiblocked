package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class DropEvent extends EventJS {
    public static final String ID = "mbd.drop";
    private final ComponentTileEntity<?> component;
    private final NonNullList<ItemStack> drops;
    private final Player player;
    public DropEvent(ComponentTileEntity<?> component, NonNullList<ItemStack> drops, Player player) {
        this.component = component;
        this.drops = drops;
        this.player = player;
    }

    public ComponentTileEntity<?> getComponent() {
        return component;
    }

    public NonNullList<ItemStack> getDrops() {
        return drops;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public boolean canCancel() {
        return true;
    }
}
