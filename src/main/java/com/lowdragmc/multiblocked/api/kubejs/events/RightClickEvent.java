package com.lowdragmc.multiblocked.api.kubejs.events;

import com.lowdragmc.multiblocked.api.tile.ComponentTileEntity;
import dev.latvian.kubejs.event.EventJS;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockRayTraceResult;

public class RightClickEvent extends EventJS {
    public static final String ID = "mbd.right_click";
    private final ComponentTileEntity<?> component;
    private final PlayerEntity player;
    private final Hand hand;
    private final BlockRayTraceResult hit;

    public RightClickEvent(ComponentTileEntity<?> component, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        this.component = component;
        this.player = player;
        this.hand = hand;
        this.hit = hit;
    }

    public ComponentTileEntity<?> getComponent() {
        return component;
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    public Hand getHand() {
        return hand;
    }

    public BlockRayTraceResult getHit() {
        return hit;
    }

    @Override
    public boolean canCancel() {
        return true;
    }
}
