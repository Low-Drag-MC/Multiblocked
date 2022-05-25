package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

public class RightClickEvent extends EventJS {
    public static final String ID = "mbd.right_click";
    private final ComponentTileEntity<?> component;
    private final Player player;
    private final InteractionHand hand;
    private final BlockHitResult hit;

    public RightClickEvent(ComponentTileEntity<?> component, Player player, InteractionHand hand, BlockHitResult hit) {
        this.component = component;
        this.player = player;
        this.hand = hand;
        this.hit = hit;
    }

    public ComponentTileEntity<?> getComponent() {
        return component;
    }

    public Player getPlayer() {
        return player;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public BlockHitResult getHit() {
        return hit;
    }

    @Override
    public boolean canCancel() {
        return true;
    }
}
