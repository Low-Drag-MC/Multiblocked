package com.lowdragmc.multiblocked.network.s2c;

import com.lowdragmc.lowdraglib.networking.IPacket;
import com.lowdragmc.multiblocked.persistence.MultiblockWorldSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

public class SPacketRemoveDisabledRendering implements IPacket {
    private BlockPos controllerPos;

    public SPacketRemoveDisabledRendering() {
    }

    public SPacketRemoveDisabledRendering(BlockPos controllerPos) {
        this.controllerPos = controllerPos;
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarLong(controllerPos.asLong());
    }

    @Override
    public void decode(FriendlyByteBuf buf) {
        this.controllerPos = BlockPos.of(buf.readVarLong());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void execute(NetworkEvent.Context handler) {
        MultiblockWorldSavedData.removeDisableModel(controllerPos);
    }

}
