package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import dev.latvian.kubejs.event.EventJS;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

public class DropEvent extends EventJS {
    public static final String ID = "mbd.drop";
    private final ComponentTileEntity<?> component;
    private final NonNullList<ItemStack> drops;
    private final  PlayerEntity player;
    public DropEvent(ComponentTileEntity<?> component, NonNullList<ItemStack> drops, PlayerEntity player) {
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

    public PlayerEntity getPlayer() {
        return player;
    }

    @Override
    public boolean canCancel() {
        return true;
    }
}
